(function () {
    'use strict';

    const ANONYMOUS_KEY = 'eshop_anonymous_id';
    const SESSION_KEY = 'eshop_session_id';
    const EVENT_TYPES = new Set([
        'page_view',
        'product_view',
        'search',
        'add_to_cart',
        'remove_from_cart',
        'checkout_started',
        'chat_message',
        'recommendation_click'
    ]);

    function randomId(prefix) {
        if (window.crypto && window.crypto.randomUUID) {
            return prefix + '-' + window.crypto.randomUUID();
        }
        return prefix + '-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
    }

    function storedId(storage, key, prefix) {
        let id = storage.getItem(key);
        if (!id) {
            id = randomId(prefix);
            storage.setItem(key, id);
        }
        return id;
    }

    const anonymousId = storedId(localStorage, ANONYMOUS_KEY, 'anon');
    const sessionId = storedId(sessionStorage, SESSION_KEY, 'session');

    function track(eventType, productId, metadata) {
        if (!EVENT_TYPES.has(eventType) && eventType !== 'purchase') return;

        const parsedProductId = productId === null || productId === undefined || productId === ''
            ? null
            : Number(productId);
        const payload = {
            eventType: eventType,
            anonymousId: anonymousId,
            sessionId: sessionId,
            productId: Number.isSafeInteger(parsedProductId) && parsedProductId > 0 ? parsedProductId : null,
            pageUrl: window.location.pathname,
            metadata: metadata ? JSON.stringify(metadata) : null
        };

        fetch('/api/tracking/events', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
            keepalive: true
        }).then(response => {
            if (!response.ok) {
                console.warn('Tracking event was rejected:', eventType, response.status);
            }
        }).catch(error => {
            console.warn('Could not send tracking event:', eventType, error);
        });
    }

    window.EShopTracking = {
        track: track,
        anonymousId: anonymousId,
        sessionId: sessionId
    };

    document.querySelectorAll('[data-tracking-anonymous-id]').forEach(input => {
        input.value = anonymousId;
    });
    document.querySelectorAll('[data-tracking-session-id]').forEach(input => {
        input.value = sessionId;
    });

    track('page_view', null, {
        path: window.location.pathname,
        title: document.title
    });

    document.addEventListener('submit', event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;

        if (form.matches('form[action$="/cart/add"]')) {
            const productId = form.querySelector('[name="productId"]')?.value;
            const quantity = form.querySelector('[name="quantity"]')?.value || '1';
            track('add_to_cart', productId, { quantity: quantity });
        } else if (form.matches('form[action$="/cart/remove"]')) {
            const productId = form.querySelector('[name="productId"]')?.value;
            track('remove_from_cart', productId, null);
        } else if (form.matches('form[action$="/cart/clear"]')) {
            form.closest('main')?.querySelectorAll('form[action$="/cart/remove"] [name="productId"]')
                .forEach(input => track('remove_from_cart', input.value, { cleared: true }));
        } else if (form.matches('form[action$="/products"]')) {
            const keyword = form.querySelector('[name="keyword"]')?.value.trim() || '';
            const categoryId = form.querySelector('[name="categoryId"]')?.value || '';
            track('search', null, {
                query: keyword.slice(0, 200),
                categoryId: categoryId
            });
        } else if (form.matches('form[action$="/checkout"]')) {
            track('checkout_started', null, { method: 'checkout' });
        }

        if (form.querySelector('#sendBtn')) {
            const text = form.querySelector('#userInput')?.value.trim() || '';
            const hasImage = Boolean(document.getElementById('imageInput')?.files?.length)
                || Boolean(document.getElementById('imagePreview')?.getAttribute('src'));
            if (text || hasImage) {
                track('chat_message', null, {
                    messageLength: text.length,
                    hasImage: hasImage
                });
            }
        }
    });

    document.addEventListener('click', event => {
        const target = event.target instanceof Element
            ? event.target.closest('[data-recommendation-id]')
            : null;
        if (target) {
            track('recommendation_click', target.getAttribute('data-recommendation-id'), null);
        }
    });

    document.querySelectorAll('[data-product-view-id]').forEach(element => {
        track('product_view', element.getAttribute('data-product-view-id'), null);
    });

    const recommendationRoot = document.getElementById('recommendationsRoot');
    if (recommendationRoot) {
        fetch('/api/recommendations?limit=8&anonymousId=' + encodeURIComponent(anonymousId), {
            headers: { 'Accept': 'application/json' }
        }).then(response => {
            if (!response.ok) throw new Error('Recommendations request failed: ' + response.status);
            return response.json();
        }).then(products => {
            if (!Array.isArray(products) || products.length === 0) return;
            recommendationRoot.innerHTML = `
                <section class="mt-5">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h4 class="fw-bold mb-0"><i class="fa-solid fa-wand-magic-sparkles text-primary me-2"></i>Gợi ý dành cho bạn</h4>
                    </div>
                    <div class="row row-cols-1 row-cols-sm-2 row-cols-md-4 g-3">
                        ${products.map(product => `
                            <div class="col">
                                <div class="card h-100 product-card">
                                    <img src="${escapeHtml(product.imageUrl || 'https://placehold.co/400x300/e2e8f0/475569?text=' + encodeURIComponent(product.name))}"
                                         class="card-img-top" style="height:180px;object-fit:cover" alt="${escapeHtml(product.name)}">
                                    <div class="card-body">
                                        <span class="badge bg-light text-primary border">${escapeHtml(product.category || '')}</span>
                                        <h6 class="card-title fw-bold mt-2">${escapeHtml(product.name)}</h6>
                                        <div class="product-price">${Number(product.price || 0).toLocaleString('vi-VN')} đ</div>
                                        <a href="/products/${encodeURIComponent(product.id)}" class="btn btn-sm btn-outline-primary w-100 mt-3" data-recommendation-id="${escapeHtml(product.id)}">
                                            Xem chi tiết
                                        </a>
                                    </div>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </section>`;
        }).catch(error => {
            console.warn('Could not load product recommendations:', error);
        });
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
})();

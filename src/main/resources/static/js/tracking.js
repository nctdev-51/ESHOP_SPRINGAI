(function () {
    const ANONYMOUS_KEY = 'eshop_anonymous_id';
    const SESSION_KEY = 'eshop_session_id';

    function randomId(prefix) {
        if (window.crypto && window.crypto.randomUUID) return prefix + '-' + window.crypto.randomUUID();
        return prefix + '-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
    }

    let anonymousId = localStorage.getItem(ANONYMOUS_KEY);
    if (!anonymousId) {
        anonymousId = randomId('anon');
        localStorage.setItem(ANONYMOUS_KEY, anonymousId);
    }
    let sessionId = sessionStorage.getItem(SESSION_KEY);
    if (!sessionId) {
        sessionId = randomId('session');
        sessionStorage.setItem(SESSION_KEY, sessionId);
    }

    function track(eventType, productId, metadata) {
        const payload = {
            eventType: eventType,
            anonymousId: anonymousId,
            sessionId: sessionId,
            productId: productId || null,
            pageUrl: window.location.href,
            metadata: metadata ? JSON.stringify(metadata) : null
        };
        fetch('/api/tracking/events', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
            keepalive: true
        }).catch(() => {});
    }

    window.EShopTracking = {
        track: track,
        anonymousId: anonymousId,
        sessionId: sessionId
    };

    track('page_view', null, {
        path: window.location.pathname,
        title: document.title
    });

    document.querySelectorAll('form[action$="/cart/add"]').forEach(form => {
        form.addEventListener('submit', () => {
            const productId = form.querySelector('[name="productId"]')?.value;
            track('add_to_cart', productId, { quantity: form.querySelector('[name="quantity"]')?.value || '1' });
        });
    });

    document.querySelectorAll('form[action$="/cart/remove"]').forEach(form => {
        form.addEventListener('submit', () => {
            const productId = form.querySelector('[name="productId"]')?.value;
            track('remove_from_cart', productId, null);
        });
    });

    const searchForm = document.querySelector('form[action$="/products"]');
    if (searchForm) {
        searchForm.addEventListener('submit', () => {
            const keyword = searchForm.querySelector('[name="keyword"]')?.value;
            track('search', null, { query: keyword || '' });
        });
    }

    const checkoutForm = document.querySelector('form[action$="/checkout"]');
    if (checkoutForm) {
        checkoutForm.addEventListener('submit', () => {
            track('checkout_started', null, { method: 'checkout' });
            track('purchase', null, { submitted: true });
        });
    }

    const imageInput = document.getElementById('imageInput');
    if (imageInput) {
        imageInput.addEventListener('change', event => {
            if (event.target.files && event.target.files.length > 0) {
                track('image_uploaded', null, { type: event.target.files[0].type });
            }
        });
    }

    const aiForm = document.querySelector('#sendBtn')?.closest('form');
    if (aiForm) {
        aiForm.addEventListener('submit', () => {
            const input = document.getElementById('userInput');
            const query = input?.value.trim() || '';
            if (query) {
                track('chat_message', null, { query: query });
                if (/tìm|màu|laptop|máy tính|dưới|gaming/i.test(query)) {
                    track('semantic_search', null, { query: query });
                }
            }
        });
    }

    document.querySelectorAll('[data-product-view-id]').forEach(element => {
        track('product_view', element.dataset.productViewId, null);
    });

    document.querySelectorAll('[data-recommendation-id]').forEach(link => {
        link.addEventListener('click', () => {
            track('recommendation_click', link.dataset.recommendationId, null);
        });
    });

    const recommendationRoot = document.getElementById('recommendationsRoot');
    if (recommendationRoot) {
        fetch('/api/recommendations?limit=8&anonymousId=' + encodeURIComponent(anonymousId), {
            headers: { 'Accept': 'application/json' }
        })
            .then(response => response.ok ? response.json() : [])
            .then(products => {
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
                                            <a href="/products/${product.id}" class="btn btn-sm btn-outline-primary w-100 mt-3" data-recommendation-id="${product.id}">
                                                Xem chi tiết
                                            </a>
                                        </div>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </section>`;
                recommendationRoot.querySelectorAll('[data-recommendation-id]').forEach(link => {
                    link.addEventListener('click', () => track('recommendation_click', link.dataset.recommendationId, null));
                });
            })
            .catch(() => {});
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }
})();

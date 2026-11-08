package com.example.eshop.repository;

import com.example.eshop.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserEventRepository extends JpaRepository<UserEvent, Long> {
    List<UserEvent> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserEvent> findTop100ByAnonymousIdOrderByCreatedAtDesc(String anonymousId);

    @Query("select e from UserEvent e where e.user.id = :userId or e.anonymousId = :anonymousId order by e.createdAt desc")
    List<UserEvent> findRecentByUserOrAnonymous(@Param("userId") Long userId,
                                                @Param("anonymousId") String anonymousId);
}

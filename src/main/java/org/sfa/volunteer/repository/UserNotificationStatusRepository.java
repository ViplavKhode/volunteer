package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.User;
import org.sfa.volunteer.model.UserNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserNotificationStatusRepository extends JpaRepository<UserNotificationStatus, String> {
    UserNotificationStatus  findByUserId(String userId);

    @Query("SELECT s.lastAccessedAt FROM UserNotificationStatus s WHERE s.id = :userId")
    Optional<LocalDateTime> findLastAccessedTimeByUser(@Param("userId") String userId);
}

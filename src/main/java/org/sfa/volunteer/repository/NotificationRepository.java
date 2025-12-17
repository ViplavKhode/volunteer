package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.Notifications;
import org.sfa.volunteer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notifications, Long> {

// "Count all notifications for this user that were created AFTER the last accessed time/ watermark"
     long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime lastAccessed);

//return the notifications list in the descending order
     List<Notifications> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId, LocalDateTime lastAccessed);

}

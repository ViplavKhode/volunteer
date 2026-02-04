package org.sfa.volunteer.repository;

import org.sfa.volunteer.model.Notifications;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notifications, Long> {

     long countByUser_IdAndCreatedAtAfter(String userId, LocalDateTime lastAccessed);

//     Page<Notifications> findByUserIdAndCreatedAtAfter(
//             String userId,
//             LocalDateTime lastAccessed,
//             Pageable pageable
//     );


     Page<Notifications> findByUserIdAndCreatedAtLessThanEqual(
             String userId,
             Optional<LocalDateTime> lastAccessed,
             Pageable pageable
     );

}


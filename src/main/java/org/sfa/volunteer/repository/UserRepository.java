package org.sfa.volunteer.repository;
import org.sfa.volunteer.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByPrimaryEmailAddress(String email);

    @Query("SELECT u.language1 FROM User u WHERE u.id = :userId")
    String getLanguagePreference1(@Param("userId") String userId);

    @Query("SELECT u.language2 FROM User u WHERE u.id = :userId")
    String getLanguagePreference2(@Param("userId") String userId);

    @Query("SELECT u.language3 FROM User u WHERE u.id = :userId")
    String getLanguagePreference3(@Param("userId") String userId);
}

package com.projet.hirevisionai.Repository;



import com.projet.hirevisionai.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    List<User> findTop5ByOrderByCreatedAtDesc();
    long countByCreatedAtAfter(LocalDateTime date);
    List<User> findByCreatedAtAfter(LocalDateTime date);

}
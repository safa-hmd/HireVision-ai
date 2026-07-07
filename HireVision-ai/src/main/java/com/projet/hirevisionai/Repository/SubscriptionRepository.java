package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Entity.Subscription;
import com.projet.hirevisionai.Entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdUser(Long userId);

    Optional<Subscription> findByUserIdUserAndStatus(Long userId, SubscriptionStatus status);

    long countByStatusAndPlan(SubscriptionStatus status, PlanType plan);

    long countByStatus(SubscriptionStatus status);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE'")
    long countActivePayingUsers();
}

package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Payment;
import com.projet.hirevisionai.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserIdUser(Long userId);

    List<Payment> findTop10ByOrderByPaymentDateDesc();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'PAID' AND p.paymentDate BETWEEN :start AND :end")
    Double sumPaidAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByStatusAndPaymentDateBetween(PaymentStatus status, LocalDateTime start, LocalDateTime end);
}

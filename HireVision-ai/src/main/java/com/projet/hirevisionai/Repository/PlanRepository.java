package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Plan;
import com.projet.hirevisionai.Entity.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, PlanType> {
}

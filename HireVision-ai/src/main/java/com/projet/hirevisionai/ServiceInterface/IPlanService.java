package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Entity.PlanType;

import java.util.List;

public interface IPlanService {
    List<PlanDTO> getAllPlans();
    PlanDTO updatePlan(PlanType key, PlanDTO dto);
}

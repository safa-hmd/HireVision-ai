package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Entity.Plan;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Repository.PlanRepository;
import com.projet.hirevisionai.ServiceInterface.IPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements IPlanService {

    private final PlanRepository planRepository;

    @Override
    public List<PlanDTO> getAllPlans() {
        seedDefaultsIfEmpty();
        return planRepository.findAll().stream()
                .sorted((a, b) -> Double.compare(a.getPrice(), b.getPrice()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PlanDTO updatePlan(PlanType key, PlanDTO dto) {
        Plan plan = planRepository.findById(key).orElseGet(() -> Plan.builder().key(key).build());
        plan.setName(dto.getName());
        plan.setPrice(dto.getPrice());
        plan.setTagline(dto.getTagline());
        plan.setHighlighted(dto.isHighlighted());
        plan.setFeatures(dto.getFeatures());
        return toDto(planRepository.save(plan));
    }

    private void seedDefaultsIfEmpty() {
        if (planRepository.count() > 0) return;

        planRepository.save(Plan.builder()
                .key(PlanType.PRO)
                .name("Pro")
                .price(29)
                .tagline("Pour un candidat qui postule activement")
                .highlighted(false)
                .features(Arrays.asList(
                        "Analyses de CV illimitées",
                        "Matching avec les offres d'emploi",
                        "Simulations d'entretien illimitées",
                        "Plan de carrière personnalisé"))
                .build());

        planRepository.save(Plan.builder()
                .key(PlanType.PREMIUM)
                .name("Premium")
                .price(59)
                .tagline("Pour maximiser vos chances")
                .highlighted(true)
                .features(Arrays.asList(
                        "Tout le plan Pro",
                        "Analyse vocale et comportementale avancée",
                        "Feedback IA détaillé après chaque entretien",
                        "Support prioritaire"))
                .build());
    }

    private PlanDTO toDto(Plan p) {
        return PlanDTO.builder()
                .key(p.getKey())
                .name(p.getName())
                .price(p.getPrice())
                .tagline(p.getTagline())
                .highlighted(p.isHighlighted())
                .features(p.getFeatures())
                .build();
    }
}

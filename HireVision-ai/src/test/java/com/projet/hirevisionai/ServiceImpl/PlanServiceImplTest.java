package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Entity.Plan;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanServiceImpl planService;

    @Test
    void getAllPlansTest_shouldSeedDefaults_whenRepositoryEmpty() {
        when(planRepository.count()).thenReturn(0L);
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        Plan pro = Plan.builder().key(PlanType.PRO).name("Pro").price(29).build();
        Plan premium = Plan.builder().key(PlanType.PREMIUM).name("Premium").price(59).build();
        when(planRepository.findAll()).thenReturn(Arrays.asList(premium, pro));

        List<PlanDTO> result = planService.getAllPlans();

        verify(planRepository, times(2)).save(any(Plan.class));
        assertThat(result).hasSize(2);
        // trié par prix croissant
        assertThat(result.get(0).getKey()).isEqualTo(PlanType.PRO);
        assertThat(result.get(1).getKey()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void getAllPlansTest_shouldNotSeed_whenRepositoryNotEmpty() {
        when(planRepository.count()).thenReturn(2L);
        Plan pro = Plan.builder().key(PlanType.PRO).name("Pro").price(29).build();
        when(planRepository.findAll()).thenReturn(List.of(pro));

        List<PlanDTO> result = planService.getAllPlans();

        verify(planRepository, never()).save(any(Plan.class));
        assertThat(result).hasSize(1);
    }

    @Test
    void updatePlanTest_shouldUpdateExistingPlan() {
        Plan existing = Plan.builder().key(PlanType.PRO).name("Pro").price(29).build();
        PlanDTO dto = PlanDTO.builder()
                .key(PlanType.PRO)
                .name("Pro Plus")
                .price(39)
                .tagline("Nouvelle offre")
                .highlighted(true)
                .features(List.of("Feature 1"))
                .build();

        when(planRepository.findById(PlanType.PRO)).thenReturn(Optional.of(existing));
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanDTO result = planService.updatePlan(PlanType.PRO, dto);

        assertThat(result.getName()).isEqualTo("Pro Plus");
        assertThat(result.getPrice()).isEqualTo(39);
        assertThat(result.isHighlighted()).isTrue();
    }

    @Test
    void updatePlanTest_shouldCreatePlan_whenNotExists() {
        PlanDTO dto = PlanDTO.builder().key(PlanType.PREMIUM).name("Premium").price(59).build();

        when(planRepository.findById(PlanType.PREMIUM)).thenReturn(Optional.empty());
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanDTO result = planService.updatePlan(PlanType.PREMIUM, dto);

        assertThat(result.getKey()).isEqualTo(PlanType.PREMIUM);
        assertThat(result.getName()).isEqualTo("Premium");
    }
}

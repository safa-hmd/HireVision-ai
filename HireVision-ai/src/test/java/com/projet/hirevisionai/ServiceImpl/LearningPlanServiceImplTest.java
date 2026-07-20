package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.LearningPlanDTO;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.LearningPlan;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Repository.LearningPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningPlanServiceImplTest {

    @Mock
    private LearningPlanRepository learningPlanRepository;

    @InjectMocks
    private LearningPlanServiceImpl learningPlanService;

    @Test
    void getByUserIdTest_shouldCombineMissedSkillAndInterviewSources() {
        MissedSkill missedSkill = MissedSkill.builder().id(1L).skillName("Docker").build();
        Interview interview = Interview.builder().id(1L).build();

        LearningPlan fromMissedSkill = LearningPlan.builder().id(1L).title("Apprendre Docker").missedSkill(missedSkill).build();
        LearningPlan fromInterview = LearningPlan.builder().id(2L).title("Revoir Spring Boot").interview(interview).build();

        when(learningPlanRepository.findByMissedSkillMatchingResultCvUserIdUser(5L))
                .thenReturn(List.of(fromMissedSkill));
        when(learningPlanRepository.findByInterviewUserIdUser(5L))
                .thenReturn(List.of(fromInterview));

        List<LearningPlanDTO> result = learningPlanService.getByUserId(5L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LearningPlanDTO::getTitle)
                .containsExactlyInAnyOrder("Apprendre Docker", "Revoir Spring Boot");
    }

    @Test
    void getByUserIdTest_shouldReturnEmptyList_whenNoPlansExist() {
        when(learningPlanRepository.findByMissedSkillMatchingResultCvUserIdUser(5L)).thenReturn(List.of());
        when(learningPlanRepository.findByInterviewUserIdUser(5L)).thenReturn(List.of());

        List<LearningPlanDTO> result = learningPlanService.getByUserId(5L);

        assertThat(result).isEmpty();
    }
}

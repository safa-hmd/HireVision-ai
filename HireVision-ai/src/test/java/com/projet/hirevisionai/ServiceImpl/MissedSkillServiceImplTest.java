package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.MissedSkillDTO;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Repository.MissedSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissedSkillServiceImplTest {

    @Mock
    private MissedSkillRepository missedSkillRepository;

    @InjectMocks
    private MissedSkillServiceImpl missedSkillService;

    @Test
    void getByUserIdTest_shouldReturnMappedList() {
        MatchingResult matchingResult = MatchingResult.builder().id(10L).score(75f).build();
        MissedSkill ms = MissedSkill.builder().id(1L).skillName("Docker").matchingResult(matchingResult).build();

        when(missedSkillRepository.findByMatchingResultCvUserIdUser(5L)).thenReturn(List.of(ms));

        List<MissedSkillDTO> result = missedSkillService.getByUserId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkillName()).isEqualTo("Docker");
    }

    @Test
    void getByUserIdTest_shouldReturnEmptyList_whenNoMissedSkills() {
        when(missedSkillRepository.findByMatchingResultCvUserIdUser(5L)).thenReturn(List.of());

        assertThat(missedSkillService.getByUserId(5L)).isEmpty();
    }
}

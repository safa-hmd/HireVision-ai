package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.JobMatchRequestDTO;
import com.projet.hirevisionai.Dto.MatchingResultDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.JobOfferRepository;
import com.projet.hirevisionai.Repository.MatchingResultRepository;
import com.projet.hirevisionai.Repository.MissedSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingResultServiceImplTest {

    @Mock
    private MatchingResultRepository matchingResultRepository;
    @Mock
    private CvRepository cvRepository;
    @Mock
    private MissedSkillRepository missedSkillRepository;
    @Mock
    private JobOfferRepository jobOfferRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MatchingResultServiceImpl matchingResultService;

    private CV cv;

    @BeforeEach
    void setUp() {
        cv = CV.builder().id(1L).build();
    }

    @Test
    void getByCvIdTest_shouldReturnMappedList() {
        MatchingResult mr = MatchingResult.builder().id(1L).cv(cv).build();
        when(matchingResultRepository.findByCvId(1L)).thenReturn(List.of(mr));

        assertThat(matchingResultService.getByCvId(1L)).hasSize(1);
    }

    @Test
    void getByUserIdTest_shouldReturnMappedList() {
        MatchingResult mr = MatchingResult.builder().id(1L).cv(cv).build();
        when(matchingResultRepository.findByCvUserIdUser(5L)).thenReturn(List.of(mr));

        assertThat(matchingResultService.getByUserId(5L)).hasSize(1);
    }

    @Test
    void getBestByCvIdTest_shouldReturn_whenFound() {
        MatchingResult mr = MatchingResult.builder().id(1L).score(95f).cv(cv).build();
        when(matchingResultRepository.findTopByCvIdOrderByScoreDesc(1L)).thenReturn(Optional.of(mr));

        assertThat(matchingResultService.getBestByCvId(1L).getScore()).isEqualTo(95f);
    }

    @Test
    void getBestByCvIdTest_shouldThrow_whenNotFound() {
        when(matchingResultRepository.findTopByCvIdOrderByScoreDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingResultService.getBestByCvId(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void matchAndSaveTest_shouldCallPythonService_andPersistMissedSkills() {
        JobMatchRequestDTO request = new JobMatchRequestDTO(1L, List.of("Java"), List.of("Java", "Docker"), null);

        Map<String, Object> pythonResponse = Map.of(
                "score", 60,
                "missing", List.of("Docker"),
                "matched", List.of("Java"),
                "label", "Compatible",
                "message", "Bon profil",
                "compatible", true
        );
        ResponseEntity<Map> response = new ResponseEntity<>(pythonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("http://localhost:8000/match-job"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(response);
        when(cvRepository.findById(1L)).thenReturn(Optional.of(cv));
        when(matchingResultRepository.save(any(MatchingResult.class))).thenAnswer(inv -> {
            MatchingResult mr = inv.getArgument(0);
            mr.setId(1L);
            return mr;
        });
        when(missedSkillRepository.save(any(MissedSkill.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchingResultDTO result = matchingResultService.matchAndSave(request);

        assertThat(result.getLabel()).isEqualTo("Compatible");
        assertThat(result.getMatched()).containsExactly("Java");
        assertThat(result.getMissingSkills()).containsExactly("Docker");
        verify(missedSkillRepository, times(1)).save(any(MissedSkill.class));
    }

    @Test
    void matchAndSaveTest_shouldUseJobOfferSkills_whenJobOfferIdProvided() {
        JobMatchRequestDTO request = new JobMatchRequestDTO(1L, List.of("Java"), null, 10L);

        JobOffer jobOffer = JobOffer.builder().id(10L).title("Offre Java").requiredSkills(List.of()).build();
        Map<String, Object> pythonResponse = Map.of("score", 70, "missing", List.of(), "matched", List.of("Java"));
        ResponseEntity<Map> response = new ResponseEntity<>(pythonResponse, HttpStatus.OK);

        when(jobOfferRepository.findById(10L)).thenReturn(Optional.of(jobOffer));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);
        when(cvRepository.findById(1L)).thenReturn(Optional.of(cv));
        when(matchingResultRepository.save(any(MatchingResult.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchingResultDTO result = matchingResultService.matchAndSave(request);

        assertThat(result.getJobOfferId()).isEqualTo(10L);
    }

    @Test
    void matchAndSaveTest_shouldThrow_whenJobOfferNotFound() {
        JobMatchRequestDTO request = new JobMatchRequestDTO(1L, List.of("Java"), null, 99L);
        when(jobOfferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingResultService.matchAndSave(request)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void matchAndSaveTest_shouldThrow_whenPythonServiceFails() {
        JobMatchRequestDTO request = new JobMatchRequestDTO(1L, List.of("Java"), List.of("Java"), null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> matchingResultService.matchAndSave(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("microservice Python");
    }
}

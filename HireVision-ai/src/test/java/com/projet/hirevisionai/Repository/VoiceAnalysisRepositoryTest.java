package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Entity.VoiceAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
class VoiceAnalysisRepositoryTest {

    @Autowired
    private VoiceAnalysisRepository voiceAnalysisRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Interview interview;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        interview = Interview.builder().startDate(LocalDateTime.now()).durationMinutes(30).user(user).build();
        entityManager.persistAndFlush(interview);
    }

    @Test
    void findByInterviewId_shouldReturnAnalysis_whenExists() {
        entityManager.persistAndFlush(VoiceAnalysis.builder()
                .clarityScore(70f).paceScore(60f).tonalVariationScore(80f)
                .audioPath("audio.mp3").interview(interview).build());

        Optional<VoiceAnalysis> result = voiceAnalysisRepository.findByInterviewId(interview.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAudioPath()).isEqualTo("audio.mp3");
    }

    @Test
    void existsByInterviewId_shouldReturnFalse_whenNoAnalysis() {
        assertThat(voiceAnalysisRepository.existsByInterviewId(interview.getId())).isFalse();
    }

    @Test
    void findByInterviewUserIdUser_shouldReturnAnalysesOfUser() {
        entityManager.persistAndFlush(VoiceAnalysis.builder()
                .clarityScore(70f).paceScore(60f).tonalVariationScore(80f).interview(interview).build());

        List<VoiceAnalysis> result = voiceAnalysisRepository.findByInterviewUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void findAvgClarityScoreByUserId_shouldComputeAverage() {
        entityManager.persistAndFlush(VoiceAnalysis.builder()
                .clarityScore(85f).paceScore(60f).tonalVariationScore(80f).interview(interview).build());

        Double avg = voiceAnalysisRepository.findAvgClarityScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(85.0, within(0.01));
    }

    @Test
    void findAvgPaceScoreByUserId_shouldComputeAverage() {
        entityManager.persistAndFlush(VoiceAnalysis.builder()
                .clarityScore(85f).paceScore(45f).tonalVariationScore(80f).interview(interview).build());

        Double avg = voiceAnalysisRepository.findAvgPaceScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(45.0, within(0.01));
    }
}

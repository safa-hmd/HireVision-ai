package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Answer;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
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
class AnswerRepositoryTest {

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Interview interview;
    private Question question;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        interview = Interview.builder().startDate(LocalDateTime.now()).durationMinutes(30).user(user).build();
        entityManager.persistAndFlush(interview);

        question = Question.builder().content("Q1")
                .difficulty(Question.Difficulty.EASY).interview(interview).build();
        entityManager.persistAndFlush(question);
    }

    @Test
    void findByQuestionId_shouldReturnAnswer_whenExists() {
        entityManager.persistAndFlush(Answer.builder().answerText("Ma réponse").score(7f).question(question).build());

        Optional<Answer> result = answerRepository.findByQuestionId(question.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAnswerText()).isEqualTo("Ma réponse");
    }

    @Test
    void findByQuestionId_shouldReturnEmpty_whenNoAnswer() {
        Optional<Answer> result = answerRepository.findByQuestionId(question.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void existsByQuestionId_shouldReturnTrue_whenAnswered() {
        entityManager.persistAndFlush(Answer.builder().answerText("Réponse").score(5f).question(question).build());

        boolean exists = answerRepository.existsByQuestionId(question.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByQuestionId_shouldReturnFalse_whenNotAnswered() {
        boolean exists = answerRepository.existsByQuestionId(question.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void findByQuestionInterviewId_shouldReturnAllAnswersOfInterview() {
        entityManager.persistAndFlush(Answer.builder().answerText("R1").score(6f).question(question).build());

        List<Answer> result = answerRepository.findByQuestionInterviewId(interview.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findAverageScoreByInterviewId_shouldComputeAverageAcrossQuestions() {
        entityManager.persistAndFlush(Answer.builder().answerText("R1").score(8f).question(question).build());

        Question question2 = Question.builder().content("Q2")
                .difficulty(Question.Difficulty.MEDIUM).interview(interview).build();
        entityManager.persistAndFlush(question2);
        entityManager.persistAndFlush(Answer.builder().answerText("R2").score(4f).question(question2).build());

        Double avg = answerRepository.findAverageScoreByInterviewId(interview.getId());

        assertThat(avg).isCloseTo(6.0, within(0.01));
    }

    @Test
    void findAverageScoreByUserId_shouldComputeAverageAcrossUserInterviews() {
        entityManager.persistAndFlush(Answer.builder().answerText("R1").score(10f).question(question).build());

        Double avg = answerRepository.findAverageScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(10.0, within(0.01));
    }
}

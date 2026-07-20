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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
class QuestionRepositoryTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Interview interview;

    @BeforeEach
    void setUp() {
        User user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        interview = Interview.builder().startDate(LocalDateTime.now()).durationMinutes(30).user(user).build();
        entityManager.persistAndFlush(interview);
    }

    @Test
    void findByInterviewId_shouldReturnAllQuestionsOfInterview() {
        entityManager.persistAndFlush(Question.builder().content("Q1")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        entityManager.persistAndFlush(Question.builder().content("Q2")
                .difficulty(Question.Difficulty.HARD).interview(interview).build());

        List<Question> result = questionRepository.findByInterviewId(interview.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByDifficulty_shouldFilterByDifficultyLevel() {
        entityManager.persistAndFlush(Question.builder().content("Facile")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        entityManager.persistAndFlush(Question.builder().content("Difficile")
                .difficulty(Question.Difficulty.HARD).interview(interview).build());

        List<Question> result = questionRepository.findByDifficulty(Question.Difficulty.HARD);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Difficile");
    }

    @Test
    void findByInterviewIdAndDifficulty_shouldCombineBothFilters() {
        entityManager.persistAndFlush(Question.builder().content("Facile")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        entityManager.persistAndFlush(Question.builder().content("Moyenne")
                .difficulty(Question.Difficulty.MEDIUM).interview(interview).build());

        List<Question> result = questionRepository.findByInterviewIdAndDifficulty(
                interview.getId(), Question.Difficulty.MEDIUM);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Moyenne");
    }

    @Test
    void countByInterviewId_shouldReturnCorrectCount() {
        entityManager.persistAndFlush(Question.builder().content("Q1")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        entityManager.persistAndFlush(Question.builder().content("Q2")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());

        long count = questionRepository.countByInterviewId(interview.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findUnansweredByInterviewId_shouldExcludeQuestionsWithAnswer() {
        Question answered = Question.builder().content("Répondue")
                .difficulty(Question.Difficulty.EASY).interview(interview).build();
        entityManager.persistAndFlush(answered);
        entityManager.persistAndFlush(Answer.builder().answerText("Ma réponse").score(8f).question(answered).build());

        entityManager.persistAndFlush(Question.builder().content("Sans réponse")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());

        List<Question> result = questionRepository.findUnansweredByInterviewId(interview.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Sans réponse");
    }

    @Test
    void countGroupedByDifficulty_shouldGroupAndCountByLevel() {
        entityManager.persistAndFlush(Question.builder().content("Q1")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        entityManager.persistAndFlush(Question.builder().content("Q2")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        entityManager.persistAndFlush(Question.builder().content("Q3")
                .difficulty(Question.Difficulty.HARD).interview(interview).build());

        List<Object[]> result = questionRepository.countGroupedByDifficulty();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(row -> tuple(row[0], row[1]))
                .containsExactlyInAnyOrder(
                        tuple(Question.Difficulty.EASY, 2L),
                        tuple(Question.Difficulty.HARD, 1L));
    }

    @Test
    void findAllByOrderByIdDesc_shouldReturnMostRecentFirst() {
        Question first = entityManager.persistAndFlush(Question.builder().content("Premier")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());
        Question second = entityManager.persistAndFlush(Question.builder().content("Second")
                .difficulty(Question.Difficulty.EASY).interview(interview).build());

        List<Question> result = questionRepository.findAllByOrderByIdDesc();

        assertThat(result.get(0).getId()).isEqualTo(second.getId());
        assertThat(result.get(1).getId()).isEqualTo(first.getId());
    }
}

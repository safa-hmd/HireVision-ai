package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private Interview interview;

    @BeforeEach
    void setUp() {
        interview = Interview.builder().id(1L).build();
    }

    @Test
    void getByDifficultyTest_shouldReturnMappedList() {
        Question question = Question.builder().id(1L).difficulty(Question.Difficulty.HARD).interview(interview).build();
        when(questionRepository.findByDifficulty(Question.Difficulty.HARD)).thenReturn(List.of(question));

        assertThat(questionService.getByDifficulty(Question.Difficulty.HARD)).hasSize(1);
    }

    @Test
    void getByDifficultyTest_shouldReturnEmptyList_whenNoneMatch() {
        when(questionRepository.findByDifficulty(Question.Difficulty.EASY)).thenReturn(List.of());

        assertThat(questionService.getByDifficulty(Question.Difficulty.EASY)).isEmpty();
    }

    @Test
    void getAllTest_shouldReturnMappedList() {
        Question question = Question.builder().id(1L).interview(interview).build();
        when(questionRepository.findAllByOrderByIdDesc()).thenReturn(List.of(question));

        List<QuestionDTO> result = questionService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void deleteTest_shouldRemove_whenExists() {
        when(questionRepository.existsById(1L)).thenReturn(true);

        questionService.delete(1L);

        verify(questionRepository).deleteById(1L);
    }

    @Test
    void deleteTest_shouldThrow_whenNotExists() {
        when(questionRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> questionService.delete(1L)).isInstanceOf(RuntimeException.class);

        verify(questionRepository, never()).deleteById(anyLong());
    }
}

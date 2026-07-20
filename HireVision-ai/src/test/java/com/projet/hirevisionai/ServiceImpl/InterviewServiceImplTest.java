package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.InterviewDTO;
import com.projet.hirevisionai.Dto.RecentInterviewDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceImplTest {

    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CvRepository cvRepository; // conservé pour l'injection même si non utilisé (create() a été retiré)

    @InjectMocks
    private InterviewServiceImpl interviewService;

    private User user;
    private CV cv;

    @BeforeEach
    void setUp() {
        user = User.builder().idUser(1L).fullName("Jean Dupont").build();
        cv = CV.builder().id(1L).build();
    }

    @Test
    void getByIdTest_shouldReturn_whenFound() {
        Interview interview = Interview.builder().id(1L).user(user).cv(cv).build();
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));

        assertThat(interviewService.getById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getByIdTest_shouldThrow_whenNotFound() {
        when(interviewRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getById(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getByUserIdTest_shouldReturnMappedList() {
        Interview interview = Interview.builder().id(1L).user(user).build();
        when(interviewRepository.findByUserIdUser(1L)).thenReturn(List.of(interview));

        assertThat(interviewService.getByUserId(1L)).hasSize(1);
    }

    @Test
    void getByUserIdSortedByDateTest_shouldReturnMappedList() {
        Interview interview = Interview.builder().id(1L).user(user).build();
        when(interviewRepository.findByUserIdUserOrderByStartDateDesc(1L)).thenReturn(List.of(interview));

        assertThat(interviewService.getByUserIdSortedByDate(1L)).hasSize(1);
    }

    @Test
    void getByDateRangeTest_shouldReturnMappedList() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Interview interview = Interview.builder().id(1L).user(user).build();
        when(interviewRepository.findByUserIdAndDateRange(1L, start, end)).thenReturn(List.of(interview));

        assertThat(interviewService.getByDateRange(1L, start, end)).hasSize(1);
    }

    @Test
    void countByUserIdTest_shouldDelegateToRepository() {
        when(interviewRepository.countByUserIdUser(1L)).thenReturn(4L);

        assertThat(interviewService.countByUserId(1L)).isEqualTo(4L);
    }

    @Test
    void deleteTest_shouldRemove_whenExists() {
        when(interviewRepository.existsById(1L)).thenReturn(true);

        interviewService.delete(1L);

        verify(interviewRepository).deleteById(1L);
    }

    @Test
    void deleteTest_shouldThrow_whenNotExists() {
        when(interviewRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> interviewService.delete(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllForAdminTest_shouldReturnListWithoutFeedback() {
        Interview interview = Interview.builder().id(1L).user(user).startDate(LocalDateTime.now()).build();
        when(interviewRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(interview));

        List<RecentInterviewDTO> result = interviewService.getAllForAdmin();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCandidateName()).isEqualTo("Jean Dupont");
        assertThat(result.get(0).getDifficulty()).isEqualTo("-");
    }
}

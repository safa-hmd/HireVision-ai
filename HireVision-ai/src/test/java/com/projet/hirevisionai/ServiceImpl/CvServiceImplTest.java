package com.projet.hirevisionai.ServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.CvAnalysisDTO;
import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Dto.CvUploadResponseDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.SkillRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CvServiceImplTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CvRepository cvRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SkillRepository skillRepository;

    private ObjectMapper objectMapper;

    private CvServiceImpl cvService;

    private User user;
    private CV cv;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cvService = new CvServiceImpl(restTemplate, cvRepository, userRepository, skillRepository, objectMapper);
        user = User.builder().idUser(1L).fullName("Jean Dupont").build();
        cv = CV.builder().id(1L).filePath("uploads/cvs/test.pdf").uploadDate(LocalDate.now()).user(user).build();
    }

    @Test
    void uploadTest_shouldSaveCv_whenUserExists() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "contenu".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cvRepository.save(any(CV.class))).thenAnswer(inv -> inv.getArgument(0));

        CvDTO result = cvService.upload(file, 1L);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getFilePath()).contains("cv.pdf");
    }

    @Test
    void uploadTest_shouldThrow_whenUserNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "contenu".getBytes());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cvService.upload(file, 99L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getByIdTest_shouldReturn_whenFound() {
        when(cvRepository.findById(1L)).thenReturn(Optional.of(cv));

        assertThat(cvService.getById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getByIdTest_shouldThrow_whenNotFound() {
        when(cvRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cvService.getById(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getByUserIdTest_shouldReturnMappedList() {
        when(cvRepository.findByUserIdUser(1L)).thenReturn(List.of(cv));

        assertThat(cvService.getByUserId(1L)).hasSize(1);
    }

    @Test
    void getLatestByUserIdTest_shouldReturn_whenFound() {
        when(cvRepository.findTopByUserIdUserOrderByUploadDateDesc(1L)).thenReturn(Optional.of(cv));

        assertThat(cvService.getLatestByUserId(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getLatestByUserIdTest_shouldThrow_whenNoneFound() {
        when(cvRepository.findTopByUserIdUserOrderByUploadDateDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cvService.getLatestByUserId(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteTest_shouldRemove_whenExists() {
        when(cvRepository.existsById(1L)).thenReturn(true);

        cvService.delete(1L);

        verify(cvRepository).deleteById(1L);
    }

    @Test
    void deleteTest_shouldThrow_whenNotExists() {
        when(cvRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> cvService.delete(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getLatestAnalysisTest_shouldParseStoredJson_whenPresent() throws Exception {
        CvAnalysisDTO analysis = CvAnalysisDTO.builder().skills(List.of("Java")).summary("Bon profil").build();
        cv.setAnalysisJson(objectMapper.writeValueAsString(analysis));

        when(cvRepository.findTopByUserIdUserOrderByUploadDateDesc(1L)).thenReturn(Optional.of(cv));

        CvUploadResponseDTO result = cvService.getLatestAnalysis(1L);

        assertThat(result.getAnalysis()).isNotNull();
        assertThat(result.getAnalysis().getSkills()).containsExactly("Java");
    }

    @Test
    void getLatestAnalysisTest_shouldReturnEmptyResponse_whenNoCvFound() {
        when(cvRepository.findTopByUserIdUserOrderByUploadDateDesc(1L)).thenReturn(Optional.empty());

        CvUploadResponseDTO result = cvService.getLatestAnalysis(1L);

        assertThat(result.getCv()).isNull();
        assertThat(result.getAnalysis()).isNull();
    }

    @Test
    void getLatestAnalysisTest_shouldReturnNullAnalysis_whenJsonIsInvalid() {
        cv.setAnalysisJson("{invalid-json");
        when(cvRepository.findTopByUserIdUserOrderByUploadDateDesc(1L)).thenReturn(Optional.of(cv));

        CvUploadResponseDTO result = cvService.getLatestAnalysis(1L);

        assertThat(result.getCv()).isNotNull();
        assertThat(result.getAnalysis()).isNull();
    }
}

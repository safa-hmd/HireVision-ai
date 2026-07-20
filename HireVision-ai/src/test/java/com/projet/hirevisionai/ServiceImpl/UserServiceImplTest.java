package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().idUser(1L).fullName("Jean Dupont").email("jean@test.com")
                .role(Role.CANDIDATE).age(30).build();
    }

    @Test
    void getById_shouldReturnUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDTO result = userService.getById(1L);

        assertThat(result.getFullName()).isEqualTo("Jean Dupont");
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAll_shouldReturnMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThat(userService.getAll()).hasSize(1);
    }

    @Test
    void update_shouldModifyUser_whenFound() {
        UserDTO dto = UserDTO.builder()
                .fullName("Nouveau Nom")
                .email("nouveau@test.com")
                .age(35)
                .phone("12345678")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.update(1L, dto);

        assertThat(result.getFullName()).isEqualTo("Nouveau Nom");
        assertThat(result.getAge()).isEqualTo(35);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(1L, UserDTO.builder().build()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void delete_shouldRemove_whenExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenNotExists() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void uploadPicture_shouldSetProfilePicture_whenUserExists() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "contenu".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.uploadPicture(1L, file);

        assertThat(result.getProfilePicture()).contains("photo.png");
    }

    @Test
    void uploadPicture_shouldThrow_whenUserNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "contenu".getBytes());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadPicture(1L, file)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void analyzeGithub_shouldThrow_whenGithubNotSet() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.analyzeGithub(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GitHub");
    }

    @Test
    void analyzeGithub_shouldExtractUsername_andCallRestTemplate() {
        user.setGithub("https://github.com/jdupont");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(restTemplate.getForObject(anyString(), eq(Object.class))).thenReturn("resultat");

        Object result = userService.analyzeGithub(1L);

        assertThat(result).isEqualTo("resultat");
        verify(restTemplate).getForObject("http://localhost:8000/analyze-github?username=jdupont", Object.class);
    }

    @Test
    void analyzeGithub_shouldThrow_whenRestTemplateFails() {
        user.setGithub("https://github.com/jdupont");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(restTemplate.getForObject(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("Service indisponible"));

        assertThatThrownBy(() -> userService.analyzeGithub(1L)).isInstanceOf(RuntimeException.class);
    }
}

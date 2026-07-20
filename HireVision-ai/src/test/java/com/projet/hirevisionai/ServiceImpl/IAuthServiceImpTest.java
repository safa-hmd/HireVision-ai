package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.Security.CustomUserDetailsService;
import com.projet.hirevisionai.Security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IAuthServiceImpTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private IAuthServiceImp authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().idUser(1L).email("jean@test.com").fullName("Jean Dupont")
                .password("encoded").role(Role.CANDIDATE).build();
    }

    @Test
    void register_shouldCreateUser_whenDataIsValid() {
        RegisterRequest request = new RegisterRequest("Jean Dupont", "jean@test.com", "motdepasse", 25, Role.CANDIDATE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("motdepasse")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(request);

        assertThat(result.getEmail()).isEqualTo("jean@test.com");
        assertThat(result.getRole()).isEqualTo(Role.CANDIDATE);
    }

    @Test
    void register_shouldThrow_whenEmailIsBlank() {
        RegisterRequest request = new RegisterRequest("Jean", "", "motdepasse", 25, Role.CANDIDATE);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email required");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest("Jean", "jean@test.com", "motdepasse", 25, Role.CANDIDATE);
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void register_shouldThrow_whenPasswordTooShort() {
        RegisterRequest request = new RegisterRequest("Jean", "jean2@test.com", "123", 25, Role.CANDIDATE);
        when(userRepository.findByEmail("jean2@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 characters");
    }

    @Test
    void register_shouldThrow_whenRoleMissing() {
        RegisterRequest request = new RegisterRequest("Jean", "jean3@test.com", "motdepasse", 25, null);
        when(userRepository.findByEmail("jean3@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role required");
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("jean@test.com", "motdepasse");

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"));
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("jean@test.com").password("encoded").authorities(authorities).build();

        when(userDetailsService.loadUserByUsername("jean@test.com")).thenReturn(userDetails);
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse result = authService.login(request);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.email()).isEqualTo("jean@test.com");
        assertThat(result.role()).isEqualTo("ROLE_CANDIDATE");
        assertThat(result.idUser()).isEqualTo(1L);
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void completeGoogleRegister_shouldCreateUser_whenNotExisting() {
        CompleteGoogleRegisterRequest request = new CompleteGoogleRegisterRequest("google@test.com", "Google User", Role.CANDIDATE);

        when(userRepository.findByEmail("google@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse result = authService.completeGoogleRegister(request);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.email()).isEqualTo("google@test.com");
        assertThat(result.role()).isEqualTo("ROLE_CANDIDATE");
    }

    @Test
    void completeGoogleRegister_shouldThrow_whenEmailAlreadyUsed() {
        CompleteGoogleRegisterRequest request = new CompleteGoogleRegisterRequest("jean@test.com", "Jean", Role.CANDIDATE);
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.completeGoogleRegister(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void forgotPassword_shouldGenerateTokenAndSendEmail_whenUserExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("jean@test.com");
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(request);

        assertThat(user.getResetToken()).isNotBlank();
        verify(emailService).sendResetEmail(eq("jean@test.com"), anyString());
    }

    @Test
    void forgotPassword_shouldThrow_whenUserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("inconnu@test.com");
        when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(request)).isInstanceOf(IllegalArgumentException.class);
        verify(emailService, never()).sendResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenValid() {
        user.setResetToken("valid-token");
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "nouveaumdp");

        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nouveaumdp")).thenReturn("newEncoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("newEncoded");
        assertThat(user.getResetToken()).isNull();
    }

    @Test
    void resetPassword_shouldThrow_whenTokenInvalid() {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "nouveaumdp");
        when(userRepository.findByResetToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetPassword_shouldThrow_whenTokenExpired() {
        user.setResetToken("expired-token");
        user.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5));
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "nouveaumdp");

        when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    void resetPassword_shouldThrow_whenNewPasswordTooShort() {
        user.setResetToken("valid-token");
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "123");

        when(userRepository.findByResetToken("valid-token")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 caractères");
    }
}

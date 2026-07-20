package com.projet.hirevisionai.Security;

import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.Security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du handler de succès OAuth2 (connexion Google) : création
 * automatique d'un compte CANDIDATE si l'email n'existe pas encore, réutilisation
 * du compte existant sinon, et construction de l'URL de redirection avec le JWT.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuth2User oAuth2User;

    private OAuth2AuthSuccessHandler handler() {
        return new OAuth2AuthSuccessHandler(userRepository, jwtService);
    }

    @Test
    void onAuthenticationSuccess_shouldRedirectWithToken_whenUserAlreadyExists() throws Exception {
        User existing = User.builder()
                .idUser(7L).email("adem@test.com").fullName("Adem")
                .role(Role.CANDIDATE).enabled(true).build();

        when(oAuth2User.getAttribute("email")).thenReturn("adem@test.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Adem");
        when(userRepository.findByEmail("adem@test.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("jwt-token-123");

        var authentication = mock(org.springframework.security.core.Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        handler().onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any());
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(urlCaptor.capture());

        String redirectUrl = urlCaptor.getValue();
        assertTrue(redirectUrl.contains("token=jwt-token-123"));
        assertTrue(redirectUrl.contains("email=adem@test.com"));
        assertTrue(redirectUrl.contains("role=ROLE_CANDIDATE"));
        assertTrue(redirectUrl.contains("id=7"));
    }

    @Test
    void onAuthenticationSuccess_shouldCreateCandidateAccount_whenUserDoesNotExist() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn("newuser@test.com");
        when(oAuth2User.getAttribute("name")).thenReturn("New User");
        when(userRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());

        User saved = User.builder()
                .idUser(42L).email("newuser@test.com").fullName("New User")
                .role(Role.CANDIDATE).enabled(true).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("jwt-token-new");

        var authentication = mock(org.springframework.security.core.Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        handler().onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertEquals("newuser@test.com", createdUser.getEmail());
        assertEquals(Role.CANDIDATE, createdUser.getRole());
        assertTrue(createdUser.isEnabled());

        verify(response).sendRedirect(contains("token=jwt-token-new"));
    }

    @Test
    void onAuthenticationSuccess_shouldUseDefaultName_whenFullNameIsNull() throws Exception {
        when(oAuth2User.getAttribute("email")).thenReturn("noname@test.com");
        when(oAuth2User.getAttribute("name")).thenReturn(null);
        when(userRepository.findByEmail("noname@test.com")).thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        var authentication = mock(org.springframework.security.core.Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        handler().onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("Utilisateur Google", userCaptor.getValue().getFullName());
    }
}

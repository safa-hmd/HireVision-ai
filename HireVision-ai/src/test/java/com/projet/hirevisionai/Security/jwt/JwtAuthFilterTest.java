package com.projet.hirevisionai.Security.jwt;

import com.projet.hirevisionai.Security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du filtre JwtAuthFilter, isolé de Spring Security à l'aide
 * de mocks Mockito. Ce filtre est LA porte d'entrée de l'authentification par
 * token sur chaque requête HTTP (hors OPTIONS) : toute régression ici impacte
 * silencieusement tous les endpoints protégés.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldSkipAuthentication_whenMethodIsOptions() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldContinueWithoutAuth_whenNoAuthorizationHeader() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldContinueWithoutAuth_whenHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldSetAuthentication_whenTokenIsValid() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.value");
        when(jwtService.extractEmail("valid.token.value")).thenReturn("adem@test.com");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();
        when(userDetailsService.loadUserByUsername("adem@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid.token.value", userDetails)).thenReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(userDetails, auth.getPrincipal());
    }

    @Test
    void doFilter_shouldNotSetAuthentication_whenTokenIsInvalid() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token.value");
        when(jwtService.extractEmail("expired.token.value")).thenReturn("adem@test.com");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();
        when(userDetailsService.loadUserByUsername("adem@test.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("expired.token.value", userDetails)).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldContinueWithoutAuth_whenTokenIsMalformed() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed.token");
        when(jwtService.extractEmail("malformed.token")).thenThrow(new RuntimeException("bad token"));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_shouldNotOverrideExistingAuthentication_whenAlreadyAuthenticated() throws Exception {
        UserDetails existing = org.springframework.security.core.userdetails.User
                .withUsername("already@test.com")
                .password("pwd")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken(existing, null, existing.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.value");
        when(jwtService.extractEmail("valid.token.value")).thenReturn("adem@test.com");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
        assertEquals(existingAuth, SecurityContextHolder.getContext().getAuthentication());
    }
}

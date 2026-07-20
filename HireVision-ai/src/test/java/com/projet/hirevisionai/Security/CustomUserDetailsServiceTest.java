package com.projet.hirevisionai.Security;

import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de CustomUserDetailsService : conversion d'un User applicatif
 * en UserDetails Spring Security (mapping du rôle en autorité ROLE_*, mapping
 * du compte désactivé, et gestion du cas utilisateur introuvable).
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        User user = User.builder()
                .idUser(1L)
                .email("adem@test.com")
                .password("encodedPassword")
                .role(Role.CANDIDATE)
                .enabled(true)
                .build();
        when(userRepository.findByEmail("adem@test.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("adem@test.com");

        assertEquals("adem@test.com", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CANDIDATE")));
    }

    @Test
    void loadUserByUsername_shouldMarkDisabled_whenUserIsNotEnabled() {
        User user = User.builder()
                .idUser(2L)
                .email("inactive@test.com")
                .password("pwd")
                .role(Role.ADMIN)
                .enabled(false)
                .build();
        when(userRepository.findByEmail("inactive@test.com")).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("inactive@test.com");

        assertFalse(result.isEnabled());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("unknown@test.com"));
    }
}

package com.projet.hirevisionai.Security.jwt;

import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires purs (pas de contexte Spring) pour JwtService : génération,
 * extraction et validation des tokens JWT. Zone sensible car c'est elle qui
 * décide si un token est valide ou non pour tout le reste de l'application.
 */
class JwtServiceTest {

    private static final String SECRET = "MySuperSecretKeyForJwtAuthSpringSecurity2026!";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ONE_HOUR_MS);
    }

    private User buildUser() {
        return User.builder()
                .idUser(1L)
                .email("adem@test.com")
                .fullName("Adem Ben")
                .password("encoded")
                .role(Role.CANDIDATE)
                .enabled(true)
                .build();
    }

    @Test
    void generateToken_fromUser_shouldEmbedEmailAsSubject() {
        User user = buildUser();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("adem@test.com", jwtService.extractEmail(token));
    }

    @Test
    void generateToken_fromUserDetails_shouldEmbedUsernameAsSubject() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("someone@test.com")
                .password("pwd")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        String token = jwtService.generateToken(userDetails);

        assertEquals("someone@test.com", jwtService.extractEmail(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenEmailMatchesAndNotExpired() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatchToken() {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        UserDetails otherUser = org.springframework.security.core.userdetails.User
                .withUsername("someoneelse@test.com")
                .password("pwd")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1L); // expire après 1ms
        User user = buildUser();
        String token = shortLivedJwtService.generateToken(user);

        Thread.sleep(20);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();

        assertFalse(shortLivedJwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsMalformed() {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();

        assertFalse(jwtService.isTokenValid("token.invalide.malforme", userDetails));
    }

    @Test
    void extractEmail_shouldThrow_whenTokenSignedWithDifferentSecret() {
        // Longueur volontairement proche de SECRET (même catégorie HS256) : jjwt choisit
        // l'algorithme HMAC en fonction de la taille de la clé, donc une clé "différente"
        // trop longue ferait basculer la signature en HS384 et la vérification échouerait
        // avec WeakKeyException (clé trop faible pour HS384) au lieu de SignatureException.
        JwtService otherJwtService = new JwtService("CleDifferenteMaisSuffisammentLonguePourHS2026!", ONE_HOUR_MS);
        User user = buildUser();
        String token = otherJwtService.generateToken(user);

        assertThrows(SignatureException.class, () -> jwtService.extractEmail(token));
    }

    @Test
    void extractEmail_shouldThrow_whenTokenIsExpired() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1L);
        User user = buildUser();
        String token = shortLivedJwtService.generateToken(user);

        Thread.sleep(20);

        assertThrows(ExpiredJwtException.class, () -> shortLivedJwtService.extractEmail(token));
    }
}

package com.projet.hirevisionai.Security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de PasswordConfig : vérifie que l'encodeur exposé est bien
 * un BCryptPasswordEncoder fonctionnel (hash non réversible, vérification OK,
 * salage différent à chaque appel).
 */
class PasswordConfigTest {

    private final PasswordConfig passwordConfig = new PasswordConfig();

    @Test
    void passwordEncoder_shouldReturnBCryptPasswordEncoderInstance() {
        PasswordEncoder encoder = passwordConfig.passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void passwordEncoder_shouldHashAndMatchPasswordCorrectly() {
        PasswordEncoder encoder = passwordConfig.passwordEncoder();

        String rawPassword = "MonMotDePasse123!";
        String encoded = encoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded);
        assertTrue(encoder.matches(rawPassword, encoded));
        assertFalse(encoder.matches("MauvaisMotDePasse", encoded));
    }

    @Test
    void passwordEncoder_shouldProduceDifferentHashes_forSamePasswordDueToSalting() {
        PasswordEncoder encoder = passwordConfig.passwordEncoder();

        String rawPassword = "MonMotDePasse123!";
        String encoded1 = encoder.encode(rawPassword);
        String encoded2 = encoder.encode(rawPassword);

        assertNotEquals(encoded1, encoded2);
        assertTrue(encoder.matches(rawPassword, encoded1));
        assertTrue(encoder.matches(rawPassword, encoded2));
    }
}

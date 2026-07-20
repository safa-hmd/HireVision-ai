package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Settings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SettingsRepository n'a pas de méthode custom non plus. Settings est une
 * entité "singleton" (une seule ligne, id = 1L). On vérifie que le CRUD de
 * base fonctionne avec ce genre d'entité à id fixe (pas auto-généré).
 */
@DataJpaTest
class SettingsRepositoryTest {

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_shouldReturnSettings_whenExists() {
        Settings settings = Settings.builder()
                .id(1L)
                .platformName("HireVision")
                .maxCvPerUser(3)
                .twoFactor(true)
                .build();
        entityManager.persistAndFlush(settings);

        Optional<Settings> result = settingsRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getPlatformName()).isEqualTo("HireVision");
        assertThat(result.get().isTwoFactor()).isTrue();
    }

    @Test
    void findById_shouldReturnEmpty_whenNoSettingsExist() {
        Optional<Settings> result = settingsRepository.findById(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldUpdateExistingSettings() {
        Settings settings = Settings.builder().id(1L).platformName("Old Name").build();
        entityManager.persistAndFlush(settings);

        settings.setPlatformName("New Name");
        settingsRepository.save(settings);
        entityManager.flush();
        entityManager.clear();

        Settings updated = settingsRepository.findById(1L).orElseThrow();
        assertThat(updated.getPlatformName()).isEqualTo("New Name");
    }
}

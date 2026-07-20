package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.SettingsDTO;
import com.projet.hirevisionai.Entity.Settings;
import com.projet.hirevisionai.Repository.SettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceImplTest {

    @Mock
    private SettingsRepository settingsRepository;

    @InjectMocks
    private SettingsServiceImpl settingsService;

    @Test
    void getSettings_shouldCreateDefaults_whenNoneExist() {
        when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(Settings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsDTO result = settingsService.getSettings();

        assertThat(result.getPlatformName()).isEqualTo("HireVision AI");
        verify(settingsRepository).save(any(Settings.class));
    }

    @Test
    void getSettings_shouldReturnExisting_whenPresent() {
        Settings existing = Settings.builder().id(1L).platformName("Custom Platform").build();
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));

        SettingsDTO result = settingsService.getSettings();

        assertThat(result.getPlatformName()).isEqualTo("Custom Platform");
        verify(settingsRepository, never()).save(any());
    }

    @Test
    void updateSettings_shouldPersistNewValues() {
        Settings existing = Settings.builder().id(1L).platformName("Old Name").build();
        SettingsDTO dto = SettingsDTO.builder()
                .platformName("New Name")
                .siteUrl("https://new.site")
                .supportEmail("support@new.site")
                .timezone("Africa/Tunis (UTC+1)")
                .language("Français")
                .currency("TND")
                .freeInterviewsPerMonth(3)
                .maxInterviewDuration(60)
                .maxCvSizeMb(10)
                .maxCvPerUser(5)
                .sessionDuration("2 heures")
                .maxLoginAttempts(3)
                .dataRetentionDays(180)
                .build();

        when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(Settings.class))).thenAnswer(inv -> inv.getArgument(0));

        SettingsDTO result = settingsService.updateSettings(dto);

        assertThat(result.getPlatformName()).isEqualTo("New Name");
        assertThat(result.getMaxLoginAttempts()).isEqualTo(3);
        verify(settingsRepository).save(any(Settings.class));
    }
}

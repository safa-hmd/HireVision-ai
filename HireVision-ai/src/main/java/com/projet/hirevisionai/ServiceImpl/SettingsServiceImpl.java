package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.SettingsDTO;
import com.projet.hirevisionai.Entity.Settings;
import com.projet.hirevisionai.Repository.SettingsRepository;
import com.projet.hirevisionai.ServiceInterface.ISettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements ISettingsService {

    private static final Long SINGLETON_ID = 1L;

    private final SettingsRepository settingsRepository;

    @Override
    public SettingsDTO getSettings() {
        return toDto(getOrCreateDefault());
    }

    @Override
    public SettingsDTO updateSettings(SettingsDTO dto) {
        Settings settings = getOrCreateDefault();

        settings.setPlatformName(dto.getPlatformName());
        settings.setSiteUrl(dto.getSiteUrl());
        settings.setSupportEmail(dto.getSupportEmail());
        settings.setTimezone(dto.getTimezone());
        settings.setLanguage(dto.getLanguage());
        settings.setCurrency(dto.getCurrency());
        settings.setFreeInterviewsPerMonth(dto.getFreeInterviewsPerMonth());
        settings.setMaxInterviewDuration(dto.getMaxInterviewDuration());
        settings.setMaxCvSizeMb(dto.getMaxCvSizeMb());
        settings.setMaxCvPerUser(dto.getMaxCvPerUser());

        settings.setNotifNewUser(dto.isNotifNewUser());
        settings.setNotifPaymentReceived(dto.isNotifPaymentReceived());
        settings.setNotifDailyReport(dto.isNotifDailyReport());
        settings.setNotifPaymentFailed(dto.isNotifPaymentFailed());
        settings.setNotifUserReport(dto.isNotifUserReport());

        settings.setTwoFactor(dto.isTwoFactor());
        settings.setSocialLogin(dto.isSocialLogin());
        settings.setVideoEncryption(dto.isVideoEncryption());
        settings.setGdpr(dto.isGdpr());
        settings.setSessionDuration(dto.getSessionDuration());
        settings.setMaxLoginAttempts(dto.getMaxLoginAttempts());
        settings.setDataRetentionDays(dto.getDataRetentionDays());

        return toDto(settingsRepository.save(settings));
    }

    private Settings getOrCreateDefault() {
        return settingsRepository.findById(SINGLETON_ID).orElseGet(() -> {
            Settings defaults = Settings.builder()
                    .id(SINGLETON_ID)
                    .platformName("HireVision AI")
                    .siteUrl("https://hirevision.ai")
                    .supportEmail("support@hirevision.ai")
                    .timezone("Africa/Tunis (UTC+1)")
                    .language("Français")
                    .currency("TND (د.ت)")
                    .freeInterviewsPerMonth(5)
                    .maxInterviewDuration(90)
                    .maxCvSizeMb(5)
                    .maxCvPerUser(10)
                    .notifNewUser(true)
                    .notifPaymentReceived(true)
                    .notifDailyReport(true)
                    .notifPaymentFailed(true)
                    .notifUserReport(false)
                    .twoFactor(true)
                    .socialLogin(true)
                    .videoEncryption(true)
                    .gdpr(true)
                    .sessionDuration("1 heure")
                    .maxLoginAttempts(5)
                    .dataRetentionDays(365)
                    .build();
            return settingsRepository.save(defaults);
        });
    }

    private SettingsDTO toDto(Settings s) {
        return SettingsDTO.builder()
                .platformName(s.getPlatformName())
                .siteUrl(s.getSiteUrl())
                .supportEmail(s.getSupportEmail())
                .timezone(s.getTimezone())
                .language(s.getLanguage())
                .currency(s.getCurrency())
                .freeInterviewsPerMonth(s.getFreeInterviewsPerMonth())
                .maxInterviewDuration(s.getMaxInterviewDuration())
                .maxCvSizeMb(s.getMaxCvSizeMb())
                .maxCvPerUser(s.getMaxCvPerUser())
                .notifNewUser(s.isNotifNewUser())
                .notifPaymentReceived(s.isNotifPaymentReceived())
                .notifDailyReport(s.isNotifDailyReport())
                .notifPaymentFailed(s.isNotifPaymentFailed())
                .notifUserReport(s.isNotifUserReport())
                .twoFactor(s.isTwoFactor())
                .socialLogin(s.isSocialLogin())
                .videoEncryption(s.isVideoEncryption())
                .gdpr(s.isGdpr())
                .sessionDuration(s.getSessionDuration())
                .maxLoginAttempts(s.getMaxLoginAttempts())
                .dataRetentionDays(s.getDataRetentionDays())
                .build();
    }
}

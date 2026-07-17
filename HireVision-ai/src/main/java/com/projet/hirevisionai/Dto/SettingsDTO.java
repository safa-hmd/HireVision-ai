package com.projet.hirevisionai.Dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsDTO {

    private String platformName;
    private String siteUrl;
    private String supportEmail;
    private String timezone;
    private String language;
    private String currency;
    private int freeInterviewsPerMonth;
    private int maxInterviewDuration;
    private int maxCvSizeMb;
    private int maxCvPerUser;

    private boolean notifNewUser;
    private boolean notifPaymentReceived;
    private boolean notifDailyReport;
    private boolean notifPaymentFailed;
    private boolean notifUserReport;

    private boolean twoFactor;
    private boolean socialLogin;
    private boolean videoEncryption;
    private boolean gdpr;
    private String sessionDuration;
    private int maxLoginAttempts;
    private int dataRetentionDays;
}

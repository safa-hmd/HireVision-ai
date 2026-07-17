package com.projet.hirevisionai.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_settings")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class Settings {

    @Id
    private Long id; // toujours 1L -> ligne unique (singleton)

    // --- Général ---
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

    // --- Notifications ---
    private boolean notifNewUser;
    private boolean notifPaymentReceived;
    private boolean notifDailyReport;
    private boolean notifPaymentFailed;
    private boolean notifUserReport;

    // --- Sécurité ---
    private boolean twoFactor;
    private boolean socialLogin;
    private boolean videoEncryption;
    private boolean gdpr;
    private String sessionDuration;
    private int maxLoginAttempts;
    private int dataRetentionDays;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

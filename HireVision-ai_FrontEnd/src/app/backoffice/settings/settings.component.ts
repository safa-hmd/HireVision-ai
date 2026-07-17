import { Component, OnInit } from '@angular/core';
import { SettingsDTO, SettingsService } from '../../services/settings.service';
import { LanguageService, LangCode } from '../../services/language.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {

  activeTab: 'general' | 'notifs' | 'security' = 'general';

  tabs = [
    { key: 'general', label: 'Général' },
    { key: 'notifs', label: 'Notifications' },
    { key: 'security', label: 'Sécurité' },
  ] as const;

  loading = true;

  // --- Général ---
  general = {
    platformName: '',
    siteUrl: '',
    supportEmail: '',
    timezone: '',
    language: '',
    currency: '',
    freeInterviewsPerMonth: 0,
    maxInterviewDuration: 0,
    maxCvSizeMb: 0,
    maxCvPerUser: 0
  };

  // --- Notifications ---
  notifToggles = {
    newUser: true,
    paymentReceived: true,
    dailyReport: true,
    paymentFailed: true,
    userReport: false
  };

  // --- Sécurité ---
  securityToggles = {
    twoFactor: true,
    socialLogin: true,
    videoEncryption: true,
    gdpr: true
  };

  sessionDuration = '1 heure';
  maxLoginAttempts = 5;
  dataRetentionDays = 365;

  // --- Toast ---
  toastMsg = '';
  toastType: 'success' | 'warning' | 'danger' = 'success';
  private toastTimer: any;

  constructor(
    private settingsService: SettingsService,
    private languageService: LanguageService
  ) {}

  ngOnInit(): void {
    this.settingsService.getSettings().subscribe({
      next: (s) => this.applySettings(s),
      error: () => this.showToast('Impossible de charger les paramètres', 'danger'),
      complete: () => this.loading = false
    });
  }

  private applySettings(s: SettingsDTO): void {
    this.general = {
      platformName: s.platformName,
      siteUrl: s.siteUrl,
      supportEmail: s.supportEmail,
      timezone: s.timezone,
      language: s.language,
      currency: s.currency,
      freeInterviewsPerMonth: s.freeInterviewsPerMonth,
      maxInterviewDuration: s.maxInterviewDuration,
      maxCvSizeMb: s.maxCvSizeMb,
      maxCvPerUser: s.maxCvPerUser
    };
    this.notifToggles = {
      newUser: s.notifNewUser,
      paymentReceived: s.notifPaymentReceived,
      dailyReport: s.notifDailyReport,
      paymentFailed: s.notifPaymentFailed,
      userReport: s.notifUserReport
    };
    this.securityToggles = {
      twoFactor: s.twoFactor,
      socialLogin: s.socialLogin,
      videoEncryption: s.videoEncryption,
      gdpr: s.gdpr
    };
    this.sessionDuration = s.sessionDuration;
    this.maxLoginAttempts = s.maxLoginAttempts;
    this.dataRetentionDays = s.dataRetentionDays;
  }

  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
  }

  save(): void {
    const payload: SettingsDTO = {
      ...this.general,
      notifNewUser: this.notifToggles.newUser,
      notifPaymentReceived: this.notifToggles.paymentReceived,
      notifDailyReport: this.notifToggles.dailyReport,
      notifPaymentFailed: this.notifToggles.paymentFailed,
      notifUserReport: this.notifToggles.userReport,
      twoFactor: this.securityToggles.twoFactor,
      socialLogin: this.securityToggles.socialLogin,
      videoEncryption: this.securityToggles.videoEncryption,
      gdpr: this.securityToggles.gdpr,
      sessionDuration: this.sessionDuration,
      maxLoginAttempts: this.maxLoginAttempts,
      dataRetentionDays: this.dataRetentionDays
    };

    this.settingsService.updateSettings(payload).subscribe({
      next: (s) => {
        this.applySettings(s);
        const code = this.mapLanguageLabelToCode(s.language);
        this.languageService.setLanguage(code);
        this.showToast('Paramètres sauvegardés !');
      },
      error: () => this.showToast('Échec de la sauvegarde', 'danger')
    });
  }

  private mapLanguageLabelToCode(label: string): LangCode {
    switch (label) {
      case 'English': return 'en';
      case 'العربية': return 'ar';
      case 'Français':
      default: return 'fr';
    }
  }

  showToast(msg: string, type: 'success' | 'warning' | 'danger' = 'success'): void {
    clearTimeout(this.toastTimer);
    this.toastMsg = msg;
    this.toastType = type;
    this.toastTimer = setTimeout(() => this.toastMsg = '', 2500);
  }
}
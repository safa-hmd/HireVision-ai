import { Component } from '@angular/core';

interface AiToggles {
  eyeContact: boolean;
  posture: boolean;
  emotions: boolean;
  voice: boolean;
  stress: boolean;
}

interface NotifToggles {
  newUser: boolean;
  paymentReceived: boolean;
  dailyReport: boolean;
  paymentFailed: boolean;
  userReport: boolean;
}

interface SecurityToggles {
  twoFactor: boolean;
  socialLogin: boolean;
  videoEncryption: boolean;
  gdpr: boolean;
}

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent {

  activeTab: 'general' | 'ai' | 'notifs' | 'security' | 'integrations' = 'general';

  tabs = [
    { key: 'general', label: 'Général' },
    { key: 'ai', label: 'IA & Modèle' },
    { key: 'notifs', label: 'Notifications' },
    { key: 'security', label: 'Sécurité' },
    { key: 'integrations', label: 'Intégrations' },
  ] as const;

  // --- Général ---
  general = {
    platformName: 'HireVision AI',
    siteUrl: 'https://hirevision.ai',
    supportEmail: 'support@hirevision.ai',
    timezone: 'Europe/Paris (UTC+2)',
    language: 'Français',
    currency: 'EUR (€)',
    freeInterviewsPerMonth: 5,
    maxInterviewDuration: 90,
    maxCvSizeMb: 5,
    maxCvPerUser: 10
  };

  // --- IA ---
  ai = {
    model: 'claude-sonnet-4',
    temperature: 0.7,
    maxTokens: 1024,
    responseLanguage: 'Automatique'
  };

  aiToggles: AiToggles = {
    eyeContact: true,
    posture: true,
    emotions: true,
    voice: true,
    stress: false
  };

  // --- Notifications ---
  notifToggles: NotifToggles = {
    newUser: true,
    paymentReceived: true,
    dailyReport: true,
    paymentFailed: true,
    userReport: false
  };

  emailConfig = {
    smtpServer: 'smtp.sendgrid.net',
    port: '587',
    sender: 'noreply@hirevision.ai',
    apiKey: '••••••••••••••••'
  };

  // --- Sécurité ---
  securityToggles: SecurityToggles = {
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

  setTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
  }

  save(): void {
    // TODO: appeler ton SettingsService.update(payload) ici
    const payload = {
      general: this.general,
      ai: this.ai,
      aiToggles: this.aiToggles,
      notifToggles: this.notifToggles,
      emailConfig: this.emailConfig,
      securityToggles: this.securityToggles,
      sessionDuration: this.sessionDuration,
      maxLoginAttempts: this.maxLoginAttempts,
      dataRetentionDays: this.dataRetentionDays
    };
    console.log('Sauvegarde des paramètres', payload);
    this.showToast('Paramètres sauvegardés !');
  }

  sendTestEmail(): void {
    this.showToast('Email de test envoyé !');
  }

  disconnectIntegration(name: string): void {
    this.showToast(`${name} déconnecté`, 'warning');
  }

  showToast(msg: string, type: 'success' | 'warning' | 'danger' = 'success'): void {
    clearTimeout(this.toastTimer);
    this.toastMsg = msg;
    this.toastType = type;
    this.toastTimer = setTimeout(() => this.toastMsg = '', 2500);
  }
}
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface SettingsDTO {
  platformName: string;
  siteUrl: string;
  supportEmail: string;
  timezone: string;
  language: string;
  currency: string;
  freeInterviewsPerMonth: number;
  maxInterviewDuration: number;
  maxCvSizeMb: number;
  maxCvPerUser: number;

  notifNewUser: boolean;
  notifPaymentReceived: boolean;
  notifDailyReport: boolean;
  notifPaymentFailed: boolean;
  notifUserReport: boolean;

  twoFactor: boolean;
  socialLogin: boolean;
  videoEncryption: boolean;
  gdpr: boolean;
  sessionDuration: string;
  maxLoginAttempts: number;
  dataRetentionDays: number;
}

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private baseUrl = 'http://localhost:8086/HireVision/settings';

  constructor(private http: HttpClient) {}

  getSettings(): Observable<SettingsDTO> {
    return this.http.get<SettingsDTO>(this.baseUrl);
  }

  updateSettings(dto: SettingsDTO): Observable<SettingsDTO> {
    return this.http.put<SettingsDTO>(this.baseUrl, dto);
  }
}

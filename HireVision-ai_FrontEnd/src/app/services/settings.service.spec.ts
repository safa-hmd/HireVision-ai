import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SettingsService, SettingsDTO } from './settings.service';

describe('SettingsService', () => {
  let service: SettingsService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/settings';

  const mockSettings: SettingsDTO = {
    platformName: 'HireVision AI',
    siteUrl: 'https://hirevision.ai',
    supportEmail: 'support@hirevision.ai',
    timezone: 'Africa/Tunis (UTC+1)',
    language: 'Français',
    currency: 'TND (د.ت)',
    freeInterviewsPerMonth: 5,
    maxInterviewDuration: 90,
    maxCvSizeMb: 5,
    maxCvPerUser: 10,
    notifNewUser: true,
    notifPaymentReceived: true,
    notifDailyReport: true,
    notifPaymentFailed: true,
    notifUserReport: false,
    twoFactor: true,
    socialLogin: true,
    videoEncryption: true,
    gdpr: true,
    sessionDuration: '1 heure',
    maxLoginAttempts: 5,
    dataRetentionDays: 365
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SettingsService]
    });
    service = TestBed.inject(SettingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getSettings should GET /settings', () => {
    service.getSettings().subscribe(res => expect(res).toEqual(mockSettings));

    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('GET');
    call.flush(mockSettings);
  });

  it('updateSettings should PUT /settings with the full DTO', () => {
    const updated = { ...mockSettings, platformName: 'HireVision AI Pro' };

    service.updateSettings(updated).subscribe(res => expect(res).toEqual(updated));

    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(updated);
    call.flush(updated);
  });
});

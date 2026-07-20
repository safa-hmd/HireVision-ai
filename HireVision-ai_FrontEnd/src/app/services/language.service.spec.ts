import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { LanguageService } from './language.service';
import { SettingsService, SettingsDTO } from './settings.service';

describe('LanguageService', () => {
  let service: LanguageService;
  let translateSpy: jasmine.SpyObj<TranslateService>;
  let settingsSpy: jasmine.SpyObj<SettingsService>;

  const STORAGE_KEY = 'hirevision_lang';

  beforeEach(() => {
    translateSpy = jasmine.createSpyObj('TranslateService', ['addLangs', 'use']);
    settingsSpy = jasmine.createSpyObj('SettingsService', ['getSettings']);

    TestBed.configureTestingModule({
      providers: [
        LanguageService,
        { provide: TranslateService, useValue: translateSpy },
        { provide: SettingsService, useValue: settingsSpy }
      ]
    });

    localStorage.removeItem(STORAGE_KEY);
    document.documentElement.lang = '';
    document.documentElement.dir = '';
  });

  afterEach(() => localStorage.removeItem(STORAGE_KEY));

  it('should be created and register the supported languages', () => {
    service = TestBed.inject(LanguageService);
    expect(service).toBeTruthy();
    expect(translateSpy.addLangs).toHaveBeenCalledWith(['fr', 'en', 'ar']);
  });

  it('init() should apply the language saved in localStorage without calling the backend', () => {
    localStorage.setItem(STORAGE_KEY, 'en');
    service = TestBed.inject(LanguageService);

    service.init();

    expect(translateSpy.use).toHaveBeenCalledWith('en');
    expect(service.currentLang).toBe('en');
    expect(document.documentElement.lang).toBe('en');
    expect(document.documentElement.dir).toBe('ltr');
    expect(settingsSpy.getSettings).not.toHaveBeenCalled();
  });

  it('init() should fetch the platform default language when nothing is saved locally', () => {
    settingsSpy.getSettings.and.returnValue(of({ language: 'العربية' } as SettingsDTO));
    service = TestBed.inject(LanguageService);

    service.init();

    expect(translateSpy.use).toHaveBeenCalledWith('ar');
    expect(service.currentLang).toBe('ar');
    expect(document.documentElement.dir).toBe('rtl');
  });

  it('init() should default to French when the settings call fails', () => {
    settingsSpy.getSettings.and.returnValue(throwError(() => new Error('network error')));
    service = TestBed.inject(LanguageService);

    service.init();

    expect(translateSpy.use).toHaveBeenCalledWith('fr');
    expect(service.currentLang).toBe('fr');
  });

  it('setLanguage() should persist the choice and apply it immediately', () => {
    service = TestBed.inject(LanguageService);

    service.setLanguage('en');

    expect(localStorage.getItem(STORAGE_KEY)).toBe('en');
    expect(translateSpy.use).toHaveBeenCalledWith('en');
    expect(service.currentLang).toBe('en');
  });

  it('an unrecognized settings language should map to French', () => {
    settingsSpy.getSettings.and.returnValue(of({ language: 'Klingon' } as SettingsDTO));
    service = TestBed.inject(LanguageService);

    service.init();

    expect(translateSpy.use).toHaveBeenCalledWith('fr');
  });
});

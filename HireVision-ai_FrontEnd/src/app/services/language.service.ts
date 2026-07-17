import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { SettingsService } from './settings.service';

export type LangCode = 'fr' | 'en' | 'ar';

const STORAGE_KEY = 'hirevision_lang';
const RTL_LANGS: LangCode[] = ['ar'];

// Mappe la valeur affichée dans le select "Langue par défaut" du backoffice
// (Settings.general.language) vers un code de langue i18n.
function mapSettingsLanguageToCode(label: string | undefined): LangCode {
  switch (label) {
    case 'English': return 'en';
    case 'العربية': return 'ar';
    case 'Français':
    default: return 'fr';
  }
}

@Injectable({ providedIn: 'root' })
export class LanguageService {

  readonly supportedLangs: LangCode[] = ['fr', 'en', 'ar'];
  currentLang: LangCode = 'fr';

  constructor(
    private translate: TranslateService,
    private settingsService: SettingsService
  ) {
    this.translate.addLangs(this.supportedLangs);
  }

  /** À appeler une fois au démarrage de l'app (APP_INITIALIZER ou ngOnInit du root component). */
  init(): void {
    const saved = localStorage.getItem(STORAGE_KEY) as LangCode | null;

    if (saved && this.supportedLangs.includes(saved)) {
      this.applyLang(saved);
      return;
    }

    // Pas de choix utilisateur en local -> on prend la langue par défaut définie dans le backoffice
    this.settingsService.getSettings().subscribe({
      next: (s) => this.applyLang(mapSettingsLanguageToCode(s.language)),
      error: () => this.applyLang('fr')
    });
  }

  setLanguage(lang: LangCode): void {
    localStorage.setItem(STORAGE_KEY, lang);
    this.applyLang(lang);
  }

  private applyLang(lang: LangCode): void {
    this.currentLang = lang;
    this.translate.use(lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = RTL_LANGS.includes(lang) ? 'rtl' : 'ltr';
  }
}

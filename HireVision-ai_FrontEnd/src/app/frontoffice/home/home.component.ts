import { Component, AfterViewInit, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService }        from '../../services/auth.service';
import { UserService }        from '../../services/user.service';
import { CvService }          from '../../services/cv.service';
import { MatchingService, MatchingResultDTO } from '../../services/matching.service';
import { PlanCarriereService, MissedSkillDTO, LearningPlanDTO } from '../../services/plan-carriere.service';

declare const lucide: any;
declare function drawLineChart(id: string, data: any[]): void;
declare function drawBarChart(id: string, data: any[]): void;
declare function drawRadarChart(id: string, data: any[]): void;

interface RecentInterview {
  id:        number;
  label:     string;
  dateLabel: string;
  score:     number;
}

interface RoadmapPreviewItem {
  title:     string;
  weekLabel: string;
  source:    string;
}

export interface DrsAxis {
  label: string;
  icon:  string;
  color: string;
  value: number;
}

export interface Badge {
  id:       string;
  icon:     string;
  label:    string;
  desc:     string;
  unlocked: boolean;
  color:    string;
}

@Component({
  selector:    'app-home',
  templateUrl: './home.component.html',
  styleUrls:   ['./home.component.css']
})
export class HomeComponent implements OnInit, AfterViewInit {

  userName  = 'Candidat';
  isLoading = true;

  // ── Stats cards ──
  profileCompletion  = 0;
  interviewsCount    = 0;
  averageScore       = 0;
  matchingOffersCount = 0;
  hasCv              = false;

  // ── Developer Readiness Score ──
  drsScore  = 0;
  drsLevel  = '';
  drsColor  = '#2563eb';
  drsAxes: DrsAxis[] = [];

  // ── Badges ──
  badges: Badge[] = [];

  // ── Charts data ──
  performanceChartData: { label: string; value: number }[] = [];
  private skillsChartData: { label: string; value: number }[] = [];;
  private cvSkillsRadarData: { label: string; value: number }[] = [];

  // ── Activité récente ──
  recentInterviews: RecentInterview[] = [];

  // ── CV / Matching / AI Insights ──
  hasCvAnalysis   = false;
  cvGlobalScore   = 0;
  cvProfile       = '';;
  bestMatch: MatchingResultDTO | null = null;
  strengths: string[] = [];
  weaknesses: string[] = [];

  // ── Roadmap preview ──
  roadmapPreview: RoadmapPreviewItem[] = [];
  missedSkillsCount = 0;

  // ── SVG progress ring ──
  get drsCircumference(): number { return 2 * Math.PI * 54; }
  get drsDashOffset(): number {
    return this.drsCircumference - (this.drsScore / 100) * this.drsCircumference;
  }

  private readonly javaUrl = 'http://localhost:8086/HireVision';

  constructor(
    private authService:         AuthService,
    private userService:         UserService,
    private cvService:           CvService,
    private matchingService:     MatchingService,
    private planCarriereService: PlanCarriereService,
    private http:                HttpClient
  ) {}

  ngOnInit(): void {
    this.userName = this.authService.getName() || 'Candidat';
    this.loadDashboard();
  }

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  // ─────────────────────────────────────────────────
  // CHARGEMENT DES DONNÉES
  // ─────────────────────────────────────────────────
  private loadDashboard(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) { this.isLoading = false; return; }

    forkJoin({
      user:       this.userService.getById(userId).pipe(catchError(() => of(null))),
      cvs:        this.cvService.getByUserId(userId).pipe(catchError(() => of([] as any[]))),
      cvAnalysis: this.cvService.getLatestAnalysis(userId).pipe(catchError(() => of(null))),
      matches:    this.matchingService.getByUserId(userId).pipe(catchError(() => of([] as MatchingResultDTO[]))),
      interviews: this.http.get<any[]>(`${this.javaUrl}/interviews/user/${userId}/sorted`).pipe(catchError(() => of([] as any[]))),
      count:      this.http.get<number>(`${this.javaUrl}/interviews/user/${userId}/count`).pipe(catchError(() => of(0))),
      avgScore:   this.http.get<number>(`${this.javaUrl}/answers/user/${userId}/average-score`).pipe(catchError(() => of(0))),
      avgTech:    this.http.get<number>(`${this.javaUrl}/feedbacks/user/${userId}/avg-technical`).pipe(catchError(() => of(0))),
      avgComm:    this.http.get<number>(`${this.javaUrl}/feedbacks/user/${userId}/avg-communication`).pipe(catchError(() => of(0))),
      avgConf:    this.http.get<number>(`${this.javaUrl}/feedbacks/user/${userId}/avg-confidence`).pipe(catchError(() => of(0))),
      avgPosture: this.http.get<number>(`${this.javaUrl}/behavior-analysis/user/${userId}/avg-posture`).pipe(catchError(() => of(0))),
      avgEyeCtc:  this.http.get<number>(`${this.javaUrl}/behavior-analysis/user/${userId}/avg-eye-contact`).pipe(catchError(() => of(0))),
      avgClarity: this.http.get<number>(`${this.javaUrl}/voice-analysis/user/${userId}/avg-clarity`).pipe(catchError(() => of(0))),
      missedSkills:  this.planCarriereService.getMissedSkills(userId).pipe(catchError(() => of([] as MissedSkillDTO[]))),
      learningPlans: this.planCarriereService.getLearningPlans(userId).pipe(catchError(() => of([] as LearningPlanDTO[]))),
    }).subscribe(res => {
      this.hasCv             = (res.cvs?.length || 0) > 0;
      this.profileCompletion = this.computeProfileCompletion(res.user, this.hasCv);
      this.matchingOffersCount = res.matches?.length || 0;
      this.averageScore      = Math.round(res.avgScore || 0);
      this.interviewsCount   = res.count || (res.interviews || []).length || 0;

      // ── Skills chart (bar) ──
      this.skillsChartData = [
        { label: 'Technique',      value: Math.round(res.avgTech    || 0) },
        { label: 'Communication',  value: Math.round(res.avgComm    || 0) },
        { label: 'Confiance',      value: Math.round(res.avgConf    || 0) },
        { label: 'Posture',        value: Math.round(res.avgPosture || 0) },
        { label: 'Contact Visuel', value: Math.round(res.avgEyeCtc  || 0) },
        { label: 'Clarté Vocale',  value: Math.round(res.avgClarity || 0) },
      ];

      // ── CV radar ──
      const analysis: any = (res.cvAnalysis as any)?.analysis;
      this.hasCvAnalysis = !!analysis;
      if (analysis) {
        this.cvGlobalScore = Math.round(analysis.global_score || 0);
        this.cvProfile     = analysis.profile || '';
        const scores: { [k: string]: number } = analysis.skill_scores || {};
        this.cvSkillsRadarData = Object.entries(scores)
          .sort((a, b) => (b[1] as number) - (a[1] as number))
          .slice(0, 6)
          .map(([label, value]) => ({ label, value: Math.round(value as number) }));
      }

      // ── Best match ──
      const sortedMatches = [...(res.matches || [])].sort((a, b) => (b.score || 0) - (a.score || 0));
      this.bestMatch = sortedMatches[0] || null;

      // ── AI Insights ──
      this.strengths  = this.cvSkillsRadarData.filter(s => s.value >= 70).slice(0, 3).map(s => s.label);
      this.weaknesses = (this.bestMatch?.missingSkills || []).slice(0, 3);
      if (!this.weaknesses.length) {
        this.weaknesses = this.cvSkillsRadarData.filter(s => s.value < 50).slice(0, 3).map(s => s.label);
      }

      // ── Roadmap preview ──
      this.missedSkillsCount = (res.missedSkills || []).length;
      this.roadmapPreview = [...(res.learningPlans || [])]
        .sort((a, b) => (a.weekNumber ?? 99) - (b.weekNumber ?? 99))
        .slice(0, 3)
        .map(p => ({
          title:     p.title,
          weekLabel: p.weekNumber ? `Semaine ${p.weekNumber}` : 'Recommandé',
          source:    p.source === 'INTERVIEW' ? 'Entretien' : 'Matching CV'
        }));

      // ── Developer Readiness Score ──
      this.computeDRS(res);

      // ── Badges ──
      this.computeBadges(res);

      const recent = (res.interviews || []).slice(0, 5);
      this.loadRecentActivityAndPerf(recent);
    });
  }

  // ─────────────────────────────────────────────────
  // DEVELOPER READINESS SCORE
  // ─────────────────────────────────────────────────
  private computeDRS(res: any): void {
    const tech    = Math.round(res.avgTech    || 0);
    const comm    = Math.round(res.avgComm    || 0);
    const conf    = Math.round(res.avgConf    || 0);
    const clarity = Math.round(res.avgClarity || 0);
    const cvScore = this.cvGlobalScore || 0;
    const matchScore = this.bestMatch?.score || 0;

    // Backend Skills = CV score (mesure compétences techniques globales)
    const backendSkills  = cvScore;
    // Frontend Skills = moyenne tech + CV si profil front
    const frontendSkills = Math.round((tech * 0.6 + cvScore * 0.4));
    // Database Skills = proxy via CV global (à affiner avec un endpoint dédié)
    const dbSkills       = Math.round(cvScore * 0.85);
    // DevOps Skills = si compétences manquantes contiennent Docker/CI-CD → faible
    const devopsKeywords = ['docker', 'ci/cd', 'kubernetes', 'azure', 'devops', 'pipeline', 'jenkins'];
    const missingLower   = (this.bestMatch?.missingSkills || []).map((s: string) => s.toLowerCase());
    const devopsGap      = devopsKeywords.filter(kw => missingLower.some((ms: string) => ms.includes(kw))).length;
    const devopsSkills   = Math.max(10, Math.round(matchScore * 0.7 - devopsGap * 12));
    // Soft Skills = communication + confiance + clarté
    const softSkills     = Math.round((comm + conf + clarity) / 3);
    // Interview Readiness = score moyen entretiens
    const interviewScore = this.averageScore;

    this.drsAxes = [
      { label: 'Backend Skills',     icon: 'server',         color: '#2563eb', value: backendSkills  },
      { label: 'Frontend Skills',    icon: 'monitor',        color: '#7c3aed', value: frontendSkills },
      { label: 'Database Skills',    icon: 'database',       color: '#06b6d4', value: dbSkills       },
      { label: 'DevOps Skills',      icon: 'git-branch',     color: '#f59e0b', value: devopsSkills   },
      { label: 'Soft Skills',        icon: 'message-circle', color: '#10b981', value: softSkills     },
      { label: 'Interview Readiness',icon: 'mic',            color: '#ef4444', value: interviewScore },
    ];

    // Score global pondéré
    const weights = [0.25, 0.20, 0.15, 0.15, 0.15, 0.10];
    const values  = [backendSkills, frontendSkills, dbSkills, devopsSkills, softSkills, interviewScore];
    this.drsScore = Math.round(values.reduce((acc, v, i) => acc + v * weights[i], 0));

    // Niveau et couleur
    if      (this.drsScore >= 80) { this.drsLevel = 'Expert';        this.drsColor = '#10b981'; }
    else if (this.drsScore >= 65) { this.drsLevel = 'Confirmé';      this.drsColor = '#2563eb'; }
    else if (this.drsScore >= 50) { this.drsLevel = 'Intermédiaire'; this.drsColor = '#f59e0b'; }
    else                          { this.drsLevel = 'Débutant';      this.drsColor = '#ef4444'; }
  }

  // ─────────────────────────────────────────────────
  // BADGES GAMIFICATION
  // ─────────────────────────────────────────────────
  private computeBadges(res: any): void {
    const cvCount        = (res.cvs || []).length;
    const interviewCount = res.count || (res.interviews || []).length || 0;
    const matchCount     = (res.matches || []).length;
    const bestMatchScore = this.bestMatch?.score || 0;
    const hasLinkedin    = !!res.user?.linkedin;
    const hasGithub      = !!res.user?.github;
    const devopsKeywords = ['docker', 'kubernetes', 'ci/cd', 'azure', 'jenkins', 'pipeline'];
    const cvSkills       = this.cvSkillsRadarData.map(s => s.label.toLowerCase());
    const hasDevops      = devopsKeywords.some(kw => cvSkills.some(s => s.includes(kw)));

    this.badges = [
      {
        id: 'cv-uploaded', icon: '📄', color: '#2563eb',
        label: 'CV Uploadé',
        desc: 'Votre CV a été analysé par l\'IA',
        unlocked: cvCount > 0
      },
      {
        id: 'first-interview', icon: '🎤', color: '#7c3aed',
        label: 'Premier Entretien',
        desc: 'Vous avez passé votre premier entretien simulé',
        unlocked: interviewCount >= 1
      },
      {
        id: 'job-matcher', icon: '🎯', color: '#06b6d4',
        label: 'Job Matcher',
        desc: 'Vous avez obtenu un matching > 70%',
        unlocked: bestMatchScore >= 70
      },
      {
        id: 'strong-score', icon: '⭐', color: '#f59e0b',
        label: 'Top Score',
        desc: 'Score moyen d\'entretien > 80%',
        unlocked: this.averageScore >= 80
      },
      {
        id: 'consistent', icon: '🔥', color: '#ef4444',
        label: 'Assidu',
        desc: 'Vous avez passé 3 entretiens ou plus',
        unlocked: interviewCount >= 3
      },
      {
        id: 'full-profile', icon: '🌍', color: '#10b981',
        label: 'Profil Complet',
        desc: 'LinkedIn et GitHub renseignés',
        unlocked: hasLinkedin && hasGithub
      },
      {
        id: 'devops-starter', icon: '🛡️', color: '#64748b',
        label: 'DevOps Starter',
        desc: 'Compétences DevOps détectées dans le CV',
        unlocked: hasDevops
      },
      {
        id: 'drs-expert', icon: '🚀', color: '#2563eb',
        label: 'Developer Ready',
        desc: 'Developer Readiness Score ≥ 65',
        unlocked: this.drsScore >= 65
      },
    ];
  }

  /** Récupère le score moyen de chaque entretien récent,
   *  puis alimente la liste "Activité récente" et le line-chart. */
  private loadRecentActivityAndPerf(interviews: any[]): void {
    if (!interviews.length) {
      this.recentInterviews    = [];
      this.performanceChartData = [];
      this.finishLoading();
      return;
    }

    const ids  = interviews.map(iv => iv.id);
    const obs$ = ids.map(id =>
      this.http.get<number>(`${this.javaUrl}/answers/interview/${id}/average-score`)
        .pipe(catchError(() => of(0)))
    );

    forkJoin(obs$).subscribe((scoresArr: number[]) => {
      const scores: Record<number, number> = {};
      ids.forEach((id, i) => { scores[id] = scoresArr[i] || 0; });

      this.recentInterviews = interviews.map(iv => ({
        id:        iv.id,
        label:     'Entretien',
        dateLabel: this.formatRelativeDate(iv.startDate || iv.date),
        score:     Math.round(scores[iv.id] || 0)
      }));

      this.performanceChartData = [...interviews].reverse().map(iv => ({
        label: this.formatShortDate(iv.startDate || iv.date),
        value: Math.round(scores[iv.id] || 0)
      }));

      this.finishLoading();
    });
  }

  private finishLoading(): void {
    this.isLoading = false;
    setTimeout(() => {
      lucide.createIcons();
      this.animateNumbers();
      this.animateDrsRing();
      if (this.performanceChartData.length) {
        drawLineChart('chart-perf', this.performanceChartData);
      }
      drawBarChart('chart-skills', this.skillsChartData);
      if (this.cvSkillsRadarData.length) {
        drawRadarChart('chart-cv-radar', this.cvSkillsRadarData);
      }
    }, 60);
  }

  // ─────────────────────────────────────────────────
  // HELPERS
  // ─────────────────────────────────────────────────

  private computeProfileCompletion(user: any, hasCv: boolean): number {
    const fields = user ? [user.phone, user.title, user.linkedin, user.github, user.profilePicture] : [];
    const filled = fields.filter(f => !!f && String(f).trim().length > 0).length;
    const total  = fields.length + 1; // +1 pour le CV
    const score  = filled + (hasCv ? 1 : 0);
    return Math.round((score / total) * 100);
  }

  /** Anime le SVG circle du DRS ring en augmentant le stroke-dashoffset. */
  private animateDrsRing(): void {
    const circle = document.querySelector('.drs-ring-fill') as SVGCircleElement;
    if (!circle) return;
    const circumference = 2 * Math.PI * 54;
    const targetOffset  = circumference - (this.drsScore / 100) * circumference;
    circle.style.strokeDasharray  = `${circumference}`;
    circle.style.strokeDashoffset = `${circumference}`;
    circle.style.transition = 'stroke-dashoffset 1.2s cubic-bezier(0.4,0,0.2,1)';
    setTimeout(() => { circle.style.strokeDashoffset = `${targetOffset}`; }, 80);
  }

  /** Retourne la classe CSS de couleur selon un score (0-100). */
  scoreClass(value: number): string {
    if (value >= 75) return 'score-excellent';
    if (value >= 55) return 'score-good';
    if (value >= 35) return 'score-medium';
    return 'score-low';
  }

  private formatRelativeDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now  = new Date();
    const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1);
    const sameDay = (a: Date, b: Date) => a.toDateString() === b.toDateString();
    const time = date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }).replace(':', 'h');
    if (sameDay(date, now))       return `Aujourd'hui, ${time}`;
    if (sameDay(date, yesterday)) return `Hier, ${time}`;
    return `${date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}, ${time}`;
  }

  private formatShortDate(dateStr: string | undefined): string {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  private animateNumbers(): void {
    document.querySelectorAll('[data-countup]').forEach((el: Element) => {
      const htmlEl   = el as HTMLElement;
      const target   = parseFloat(htmlEl.dataset['countup'] || '0');
      const suffix   = htmlEl.dataset['suffix'] || '';
      const duration = 800;
      const start    = performance.now();
      const update = (now: number) => {
        const progress = Math.min((now - start) / duration, 1);
        const eased    = 1 - Math.pow(1 - progress, 3);
        const value    = target % 1 === 0
          ? Math.floor(eased * target)
          : (eased * target).toFixed(1);
        htmlEl.textContent = value + suffix;
        if (progress < 1) requestAnimationFrame(update);
      };
      requestAnimationFrame(update);
    });
  }
}
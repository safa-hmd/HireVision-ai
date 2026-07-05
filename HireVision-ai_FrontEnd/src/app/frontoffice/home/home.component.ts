import { Component, AfterViewInit, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService }     from '../../services/auth.service';
import { UserService }     from '../../services/user.service';
import { CvService }       from '../../services/cv.service';
import { MatchingService } from '../../services/matching.service';

declare const lucide: any;
declare function drawLineChart(id: string, data: any[]): void;
declare function drawBarChart(id: string, data: any[]): void;

interface RecentInterview {
  id:        number;
  label:     string;
  dateLabel: string;
  score:     number;
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

  // ── Charts data (rendus après affichage du DOM) ──
  performanceChartData: { label: string; value: number }[] = [];
  private skillsChartData: { label: string; value: number }[] = [];

  // ── Activité récente ──
  recentInterviews: RecentInterview[] = [];

  private readonly javaUrl = 'http://localhost:8086/HireVision';

  constructor(
    private authService:    AuthService,
    private userService:    UserService,
    private cvService:      CvService,
    private matchingService: MatchingService,
    private http:           HttpClient
  ) {}

  ngOnInit(): void {
    this.userName = this.authService.getName() || 'Candidat';
    this.loadDashboard();
  }

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  // ─────────────────────────────────────────────────
  // CHARGEMENT DES DONNÉES (100% via Spring Boot)
  // ─────────────────────────────────────────────────
  private loadDashboard(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) { this.isLoading = false; return; }

    forkJoin({
      user:       this.userService.getById(userId).pipe(catchError(() => of(null))),
      cvs:        this.cvService.getByUserId(userId).pipe(catchError(() => of([] as any[]))),
      matches:    this.matchingService.getByUserId(userId).pipe(catchError(() => of([] as any[]))),
      interviews: this.http.get<any[]>(`${this.javaUrl}/interviews/user/${userId}/sorted`).pipe(catchError(() => of([] as any[]))),
      count:      this.http.get<number>(`${this.javaUrl}/interviews/user/${userId}/count`).pipe(catchError(() => of(0))),
      avgScore:   this.http.get<number>(`${this.javaUrl}/answers/user/${userId}/average-score`).pipe(catchError(() => of(0))),
      avgTech:    this.http.get<number>(`${this.javaUrl}/feedbacks/user/${userId}/avg-technical`).pipe(catchError(() => of(0))),
      avgComm:    this.http.get<number>(`${this.javaUrl}/feedbacks/user/${userId}/avg-communication`).pipe(catchError(() => of(0))),
      avgConf:    this.http.get<number>(`${this.javaUrl}/feedbacks/user/${userId}/avg-confidence`).pipe(catchError(() => of(0))),
      avgPosture: this.http.get<number>(`${this.javaUrl}/behavior-analysis/user/${userId}/avg-posture`).pipe(catchError(() => of(0))),
      avgEyeCtc:  this.http.get<number>(`${this.javaUrl}/behavior-analysis/user/${userId}/avg-eye-contact`).pipe(catchError(() => of(0))),
      avgClarity: this.http.get<number>(`${this.javaUrl}/voice-analysis/user/${userId}/avg-clarity`).pipe(catchError(() => of(0))),
    }).subscribe(res => {
      this.hasCv             = (res.cvs?.length || 0) > 0;
      this.profileCompletion = this.computeProfileCompletion(res.user, this.hasCv);
      this.matchingOffersCount = res.matches?.length || 0;
      this.averageScore      = Math.round(res.avgScore || 0);
      this.interviewsCount   = res.count || (res.interviews || []).length || 0;

      this.skillsChartData = [
        { label: 'Technique',      value: Math.round(res.avgTech    || 0) },
        { label: 'Communication',  value: Math.round(res.avgComm    || 0) },
        { label: 'Confiance',      value: Math.round(res.avgConf    || 0) },
        { label: 'Posture',        value: Math.round(res.avgPosture || 0) },
        { label: 'Contact Visuel', value: Math.round(res.avgEyeCtc  || 0) },
        { label: 'Clarté Vocale',  value: Math.round(res.avgClarity || 0) },
      ];

      const recent = (res.interviews || []).slice(0, 5);
      this.loadRecentActivityAndPerf(recent);
    });
  }

  /** Récupère le score moyen de chaque entretien récent (1 appel/entretien),
   *  puis alimente à la fois la liste "Activité récente" et le line-chart. */
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

      // Liste (ordre du plus récent au plus ancien, tel que retourné par /sorted)
      this.recentInterviews = interviews.map(iv => ({
        id:        iv.id,
        label:     'Entretien',
        dateLabel: this.formatRelativeDate(iv.startDate || iv.date),
        score:     Math.round(scores[iv.id] || 0)
      }));

      // Chart (ordre chronologique croissant : du plus ancien au plus récent)
      this.performanceChartData = [...interviews].reverse().map(iv => ({
        label: this.formatShortDate(iv.startDate || iv.date),
        value: Math.round(scores[iv.id] || 0)
      }));

      this.finishLoading();
    });
  }

  private finishLoading(): void {
    this.isLoading = false;
    // Laisser Angular rendre le DOM (canvas, [attr.data-countup]) avant de dessiner
    setTimeout(() => {
      lucide.createIcons();
      this.animateNumbers();
      if (this.performanceChartData.length) {
        drawLineChart('chart-perf', this.performanceChartData);
      }
      drawBarChart('chart-skills', this.skillsChartData);
    }, 60);
  }

  // ─────────────────────────────────────────────────
  // HELPERS
  // ─────────────────────────────────────────────────

  /** Heuristique simple de complétion de profil : champs renseignés + CV présent. */
  private computeProfileCompletion(user: any, hasCv: boolean): number {
    const fields = user ? [user.phone, user.title, user.linkedin, user.github, user.profilePicture] : [];
    const filled = fields.filter(f => !!f && String(f).trim().length > 0).length;
    const total  = fields.length + 1; // +1 pour le CV
    const score  = filled + (hasCv ? 1 : 0);
    return Math.round((score / total) * 100);
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
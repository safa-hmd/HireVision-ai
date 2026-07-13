import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

declare const lucide: any;
declare function showToast(message: string, type?: string): void;

interface RadarPoint { x: number; y: number; }
interface RadarLabel { x: number; y: number; label: string; anchor: string; }
interface RadarAxisLine { x1: number; y1: number; x2: number; y2: number; }

@Component({
  selector:    'app-interview-feedback',
  templateUrl: './interview-feedback.component.html',
  styleUrls:   ['./interview-feedback.component.css']
})
export class InterviewFeedbackComponent implements OnInit, AfterViewInit {

  results:   any     = null;
  feedback:  any     = null;
  isLoading: boolean = true;

  // Tous les appels IA passent par Spring Boot
  private readonly javaUrl = 'http://localhost:8086/HireVision';

  get globalScore(): number { return this.results?.avg_scores?.global || 0; }
  get scoreColor():  string {
    if (this.globalScore >= 75) return '#10B981';
    if (this.globalScore >= 50) return '#F59E0B';
    return '#EF4444';
  }
  get scoreBannerBg(): string {
    if (this.globalScore >= 75) return '#ECFDF5';
    if (this.globalScore >= 50) return '#FFFBEB';
    return '#FEF2F2';
  }
  get scoreBannerBorder(): string {
    if (this.globalScore >= 75) return '#A7F3D0';
    if (this.globalScore >= 50) return '#FDE68A';
    return '#FECACA';
  }
  get questions_total(): number { return this.results?.questions_total || 15; }

  // ── Graphique "Scores par Question" ──
  get questionBars(): { label: string; value: number }[] {
    return (this.results?.answers || []).map((a: any, i: number) => ({
      label: 'Q' + (i + 1),
      value: a.transcript === '(passé)' ? 0 : (a.evaluation?.score_global || 0)
    }));
  }

  // ── Radar "Vue d'ensemble des performances" ──
  radarPolygonPoints = '';
  radarGridPolygons: string[] = [];
  radarAxisLines: RadarAxisLine[] = [];
  radarLabels: RadarLabel[] = [];
  radarPointsArr: RadarPoint[] = [];
  private readonly radarCx = 140;
  private readonly radarCy = 135;
  private readonly radarR  = 90;

  constructor(private router: Router, private http: HttpClient) {}

  ngOnInit(): void {
    const stored = sessionStorage.getItem('interview_results');
    if (!stored) { this.router.navigate(['/frontoffice/interviewPrep']); return; }
    this.results = JSON.parse(stored);
    this.buildRadarChart();
    this.loadFeedback();
  }

  ngAfterViewInit(): void {
    setTimeout(() => lucide?.createIcons(), 100);
  }

  loadFeedback(): void {
    // Spring Boot → Python : génération feedback + plan d'apprentissage
    this.http.post<any>(`${this.javaUrl}/interview/feedback`, {
      specialty:  this.results.specialty.title,
      answers:    this.results.answers.map((a: any) => ({
        question:   a.question?.question || '',
        transcript: a.transcript        || '',
        score:      a.evaluation?.score_global || 0
      })),
      avg_scores: this.results.avg_scores
    }).subscribe({
      next: (fb) => {
        this.feedback  = fb;
        this.isLoading = false;
        setTimeout(() => lucide?.createIcons(), 100);
        this.persistLearningPlan(fb?.plan_apprentissage);
      },
      error: () => {
        this.feedback  = this.buildFallbackFeedback();
        this.isLoading = false;
        setTimeout(() => lucide?.createIcons(), 100);
        this.persistLearningPlan(this.feedback?.plan_apprentissage);
      }
    });
  }

  // Sauvegarde le plan d'apprentissage IA en DB (visible ensuite sur "Plan de Carrière")
  private persistLearningPlan(planItems: any[] | undefined): void {
    const interviewId = this.results?.interviewId;
    if (!interviewId || !planItems || !planItems.length) return;

    const items = planItems.map(p => ({
      semaine:   p.semaine,
      theme:     p.theme,
      ressource: p.ressource,
      action:    p.action
    }));

    this.http.post(`${this.javaUrl}/learning-plans/from-interview/${interviewId}`, items)
      .subscribe({
        error: () => { /* non bloquant : la page feedback reste utilisable */ }
      });
  }

  private buildFallbackFeedback(): any {
    const score = this.globalScore;
    return {
      titre: score >= 75 ? 'Excellent travail ! 🎉'
           : score >= 50 ? 'Bon effort !'
           : 'À améliorer',
      message_global: score >= 75
        ? 'Vous avez obtenu des résultats au-dessus de la moyenne. Vos connaissances techniques et vos compétences de communication sont impressionnantes.'
        : score >= 50
        ? 'Vous avez de bonnes bases. Quelques points à approfondir.'
        : 'La préparation est la clé. Continuez à pratiquer pour progresser.',
      points_forts:      ['Excellente maîtrise des concepts Java et principes POO', 'Communication globalement claire', 'Réponses cohérentes'],
      axes_amelioration: ['Approfondissez les concepts techniques avancés', 'Donnez plus d\'exemples concrets', 'Structurez vos réponses avec la méthode STAR'],
      conseil_final:     'La pratique régulière d\'entretiens simulés est le meilleur moyen de progresser !',
      recommandation:    score >= 60 ? 'En bonne voie pour le poste' : 'Préparation supplémentaire recommandée',
      plan_apprentissage: [
        { semaine: 1, theme: 'Révision des bases',   ressource: 'Documentation officielle', action: 'Lire et résumer les concepts clés' },
        { semaine: 2, theme: 'Projets pratiques',    ressource: 'GitHub / Exercices',       action: 'Coder un mini-projet en 48h' },
        { semaine: 3, theme: 'Entretiens simulés',   ressource: 'HireVision AI',            action: 'Refaire 3 simulations d\'entretien' }
      ]
    };
  }

  // ─────────────────────────────────────────────────
  // RADAR CHART (SVG) — hexagone à 6 axes
  // ─────────────────────────────────────────────────
  private buildRadarChart(): void {
    const b = this.results?.behavior || {};
    const s = this.results?.avg_scores || {};

    const metrics = [
      { label: 'Technique',      value: s.technique       || 0 },
      { label: 'Communication',  value: s.communication   || 0 },
      { label: 'Confiance',      value: s.confiance        || 0 },
      { label: 'Contact Visuel', value: b.contactVisuel    || 0 },
      { label: 'Posture',        value: b.posture          || 0 },
      { label: 'Clarté',         value: b.clarteVocale     || 0 },
    ];

    const n     = metrics.length;
    const cx    = this.radarCx;
    const cy    = this.radarCy;
    const R     = this.radarR;
    const step  = (2 * Math.PI) / n;

    const pts: string[]        = [];
    const ptsArr: RadarPoint[] = [];
    const labels: RadarLabel[] = [];
    const axes: RadarAxisLine[] = [];

    metrics.forEach((m, i) => {
      const angle = -Math.PI / 2 + i * step;
      const r     = R * (Math.max(m.value, 3) / 100);
      const x     = cx + r * Math.cos(angle);
      const y     = cy + r * Math.sin(angle);
      pts.push(`${x.toFixed(1)},${y.toFixed(1)}`);
      ptsArr.push({ x, y });

      const lx = cx + (R + 28) * Math.cos(angle);
      const ly = cy + (R + 28) * Math.sin(angle);
      let anchor = 'middle';
      if (Math.cos(angle) > 0.35)      anchor = 'start';
      else if (Math.cos(angle) < -0.35) anchor = 'end';
      labels.push({ x: lx, y: ly, label: m.label, anchor });

      axes.push({
        x1: cx, y1: cy,
        x2: cx + R * Math.cos(angle),
        y2: cy + R * Math.sin(angle)
      });
    });

    this.radarPolygonPoints = pts.join(' ');
    this.radarPointsArr     = ptsArr;
    this.radarLabels        = labels;
    this.radarAxisLines     = axes;

    this.radarGridPolygons = [0.25, 0.5, 0.75, 1].map(fraction => {
      const gp: string[] = [];
      for (let i = 0; i < n; i++) {
        const angle = -Math.PI / 2 + i * step;
        const r     = R * fraction;
        gp.push(`${(cx + r * Math.cos(angle)).toFixed(1)},${(cy + r * Math.sin(angle)).toFixed(1)}`);
      }
      return gp.join(' ');
    });
  }

  formatDuration(s: number): string {
    const m   = Math.floor(s / 60);
    const sec = s % 60;
    return m > 0 ? `${m}m ${sec}s` : `${sec}s`;
  }

  getNiveauColor(n: string): string {
    if (n === 'Excellent') return '#10B981';
    if (n === 'Bien')      return '#3B82F6';
    if (n === 'Moyen')     return '#F59E0B';
    return '#EF4444';
  }

  stressColor(v: number): string {
    if (v <= 35) return '#10B981';
    if (v <  60) return '#F59E0B';
    return '#EF4444';
  }

  watchVideo(): void {
    showToast?.('Aucun enregistrement vidéo disponible pour cette session.', 'info');
  }

  printReport(): void {
    const original = document.title;
    const specialty = (this.results?.specialty?.title || 'Entretien').replace(/[^\w\-]+/g, '_');
    document.title = `Rapport_Feedback_${specialty}`;
    window.print();
    setTimeout(() => { document.title = original; }, 500);
  }

  restart(): void {
    sessionStorage.removeItem('interview_results');
    this.router.navigate(['/frontoffice/interviewPrep']);
  }
}
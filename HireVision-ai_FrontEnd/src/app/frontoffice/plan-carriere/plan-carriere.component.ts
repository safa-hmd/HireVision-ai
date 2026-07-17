import { AfterViewInit, Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { LearningPlanDTO, MissedSkillDTO, PlanCarriereService } from 'src/app/services/plan-carriere.service';

declare const lucide: any;
declare function animateNumbers(): void;

interface WeeklyStep {
  weekNumber: number;
  items: LearningPlanDTO[];
}

@Component({
  selector: 'app-plan-carriere',
  templateUrl: './plan-carriere.component.html',
  styleUrls: ['./plan-carriere.component.css']
})
export class PlanCarriereComponent implements OnInit, AfterViewInit {

  isLoading = true;

  missedSkills: MissedSkillDTO[] = [];
  learningPlans: LearningPlanDTO[] = [];

  // Dérivés pour le template
  weeklySteps: WeeklyStep[] = [];
  recommendedCourses: LearningPlanDTO[] = [];

  masteredCount = 0;
  toLearnCount = 0;
  inProgressCount = 0;
  progressPercent = 0;

  constructor(
    private planCarriereService: PlanCarriereService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      this.isLoading = false;
      return;
    }

    this.planCarriereService.getMissedSkills(userId).subscribe({
      next: (skills: MissedSkillDTO[]) => {
        this.missedSkills = skills || [];
        this.computeStats();
        this.tryFinishLoading();
      },
      error: () => this.tryFinishLoading()
    });

    this.planCarriereService.getLearningPlans(userId).subscribe({
      next: (plans: LearningPlanDTO[]) => {
        this.learningPlans = plans || [];
        this.buildWeeklySteps();
        this.buildRecommendedCourses();
        this.computeStats();
        this.tryFinishLoading();
      },
      error: () => this.tryFinishLoading()
    });
  }

  ngAfterViewInit(): void {
    lucide?.createIcons();
  }

  private loadedCount = 0;
  private tryFinishLoading(): void {
    this.loadedCount++;
    if (this.loadedCount >= 2) {
      this.isLoading = false;
      setTimeout(() => {
        animateNumbers?.();
        lucide?.createIcons();
      }, 50);
    }
  }

  // Compétences manquantes triées (Haute → Moyenne → Basse)
  get sortedMissedSkills(): MissedSkillDTO[] {
    const order: { [key: string]: number } = { HAUTE: 0, MOYENNE: 1, BASSE: 2 };
    return [...this.missedSkills].sort((a, b) => order[a.priority] - order[b.priority]);
  }

  private buildWeeklySteps(): void {
    const interviewPlans = this.learningPlans.filter(p => p.source === 'INTERVIEW' && p.weekNumber);
    const byWeek = new Map<number, LearningPlanDTO[]>();
    for (const p of interviewPlans) {
      const week = p.weekNumber as number;
      if (!byWeek.has(week)) byWeek.set(week, []);
      byWeek.get(week)!.push(p);
    }
    this.weeklySteps = Array.from(byWeek.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([weekNumber, items]) => ({ weekNumber, items }));
  }

  private buildRecommendedCourses(): void {
    // Les plans issus du job matching pointent vers des ressources/cours concrets
    this.recommendedCourses = this.learningPlans.filter(p => p.source === 'JOB_MATCHING');
  }

  private computeStats(): void {
    // "À apprendre" = compétences manquantes détectées lors du matching CV/offre
    this.toLearnCount = this.missedSkills.length;

    // "En cours" = compétences manquantes pour lesquelles une ressource
    // d'apprentissage a déjà été générée (donc déjà entamées)
    const missedSkillIdsWithPlan = new Set(
      this.learningPlans.filter(p => p.source === 'JOB_MATCHING' && p.missedSkillId).map(p => p.missedSkillId)
    );
    this.inProgressCount = missedSkillIdsWithPlan.size;

    // "Maîtrisées" = semaines de plan d'entretien déjà couvertes
    this.masteredCount = new Set(
      this.learningPlans.filter(p => p.source === 'INTERVIEW').map(p => p.weekNumber)
    ).size;

    const total = this.masteredCount + this.inProgressCount + this.toLearnCount;
    this.progressPercent = total > 0 ? Math.round((this.masteredCount / total) * 100) : 0;
  }

  priorityBadgeClass(priority: string): string {
    if (priority === 'HAUTE') return 'badge badge-danger';
    if (priority === 'MOYENNE') return 'badge badge-warning';
    return 'badge badge-muted';
  }

  priorityLabel(priority: string): string {
    if (priority === 'HAUTE') return 'Haute Priorité';
    if (priority === 'MOYENNE') return 'Priorité Moyenne';
    return 'Basse Priorité';
  }

  printReport(): void {
    const original = document.title;
    document.title = `Plan_Carriere_IA_${new Date().toISOString().slice(0, 10)}`;
    window.print();
    setTimeout(() => { document.title = original; }, 500);
  }
}
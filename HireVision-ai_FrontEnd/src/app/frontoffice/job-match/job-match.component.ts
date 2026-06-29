import { AfterViewInit, Component } from '@angular/core';
import { MatchingService, MatchingResultDTO } from '../../services/matching.service';
import { CvService } from '../../services/cv.service';
import { AuthService } from '../../services/auth.service';

declare const lucide: any;
declare function animateNumbers(): void;
declare function drawDoughnutChart(canvasId: string, data: any[]): void;
declare function showToast(message: string, type?: string): void;

@Component({
  selector: 'app-job-match',
  templateUrl: './job-match.component.html',
  styleUrls: ['./job-match.component.css']
})
export class JobMatchComponent implements AfterViewInit {

  jobDescription = '';
  showResults    = false;
  isLoading      = false;
  result: MatchingResultDTO | null = null;

  constructor(
    private matchingService: MatchingService,
    private cvService: CvService,
    private authService: AuthService
  ) {}

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  extractSkillsFromText(text: string): string[] {
    const knownSkills = [
      'java', 'spring', 'spring boot', 'angular', 'react', 'vue', 'python',
      'php', 'symfony', 'mysql', 'postgresql', 'oracle', 'mongodb', 'docker',
      'kubernetes', 'azure', 'aws', 'gcp', 'git', 'github', 'ci/cd', 'jenkins',
      'typescript', 'javascript', 'node', 'nodejs', 'rest', 'api', 'microservices',
      'html', 'css', 'bootstrap', 'tailwind', 'flutter', 'dart', 'c++', 'c#',
      '.net', 'kotlin', 'linux', 'agile', 'scrum', 'jira', 'mlflow',
      'tensorflow', 'pytorch', 'redis', 'graphql', 'kafka'
    ];
    const textLower = text.toLowerCase();
    return knownSkills.filter(s => textLower.includes(s));
  }

  analyzeJob(): void {
    if (!this.jobDescription.trim()) {
      showToast('Veuillez entrer une description de poste', 'warning');
      return;
    }

    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      showToast('Utilisateur non connecté', 'danger');
      return;
    }

    const jobSkills = this.extractSkillsFromText(this.jobDescription);
    if (jobSkills.length === 0) {
      showToast('Aucune compétence détectée dans la description', 'warning');
      return;
    }

    this.isLoading = true;
    showToast('Analyse en cours...', 'info');

    this.cvService.getLatest(userId).subscribe({
      next: (cv) => {
        const cvSkills = cv.skillNames?.length ? cv.skillNames : [];

        this.matchingService.matchAndSave({
          cvId: cv.id!,
          cvSkills,
          jobSkills
        }).subscribe({
          next: (res) => {
            this.result      = res;
            this.showResults = true;
            this.isLoading   = false;
            setTimeout(() => {
              animateNumbers();
              drawDoughnutChart('chart-doughnut', [
                { label: 'Correspondantes', value: res.score,       color: '#10B981' },
                { label: 'Manquantes',      value: 100 - res.score, color: '#EF4444' }
              ]);
              lucide.createIcons();
              showToast('Analyse terminée !', 'success');
            }, 100);
          },
          error: () => {
            this.isLoading = false;
            showToast('Erreur lors de l\'analyse', 'danger');
          }
        });
      },
      error: () => {
        this.isLoading = false;
        showToast('Aucun CV trouvé. Uploadez d\'abord votre CV.', 'warning');
      }
    });
  }

  getScoreColor(): string {
    if (!this.result) return 'var(--text-muted)';
    if (this.result.score >= 70) return 'var(--success)';
    if (this.result.score >= 45) return 'var(--warning)';
    return 'var(--danger)';
  }

  reset(): void {
    this.showResults    = false;
    this.result         = null;
    this.jobDescription = '';
    setTimeout(() => lucide.createIcons(), 50);
  }
}
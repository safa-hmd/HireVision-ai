import { AfterViewInit, Component, OnInit } from '@angular/core';
import { MatchingService, MatchingResultDTO } from '../../services/matching.service';
import { CvService } from '../../services/cv.service';
import { AuthService } from '../../services/auth.service';
import { JobOfferService, JobOfferDTO } from '../../services/job-offer.service';

declare const lucide: any;
declare function animateNumbers(): void;
declare function drawDoughnutChart(canvasId: string, data: any[]): void;
declare function showToast(message: string, type?: string): void;

export interface SkillComparison {
  skill:  string;
  status: 'match' | 'missing' | 'partial';
}

@Component({
  selector: 'app-job-match',
  templateUrl: './job-match.component.html',
  styleUrls: ['./job-match.component.css']
})
export class JobMatchComponent implements AfterViewInit, OnInit {

  jobDescription = '';
  showResults    = false;
  isLoading      = false;
  result: MatchingResultDTO | null = null;

  jobOffers: JobOfferDTO[] = [];
  selectedJobOfferId: number | null = null;

  // ── Tableau de comparaison visuel ──
  skillComparison: SkillComparison[] = [];
  aiAdvice = '';

  constructor(
    private matchingService: MatchingService,
    private cvService: CvService,
    private authService: AuthService,
    private jobOfferService: JobOfferService
  ) {}

  ngOnInit(): void {
    this.jobOfferService.getActive().subscribe({
      next: (offers) => this.jobOffers = offers,
      error: () => this.jobOffers = []
    });
  }

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  onJobOfferSelected(): void {
    const offer = this.jobOffers.find(o => o.id === this.selectedJobOfferId);
    this.jobDescription = offer ? (offer.description || offer.title) : '';
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
    if (!this.selectedJobOfferId && !this.jobDescription.trim()) {
      showToast('Veuillez entrer une description de poste ou choisir une offre', 'warning');
      return;
    }

    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      showToast('Utilisateur non connecté', 'danger');
      return;
    }

    const jobSkills = this.selectedJobOfferId ? [] : this.extractSkillsFromText(this.jobDescription);
    if (!this.selectedJobOfferId && jobSkills.length === 0) {
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
          jobSkills,
          jobOfferId: this.selectedJobOfferId
        }).subscribe({
          next: (res) => {
            this.result      = res;
            this.showResults = true;
            this.isLoading   = false;

            // Build skill comparison table
            this.buildSkillComparison(res);

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

  /** Build visual skill comparison table from matching results. */
  private buildSkillComparison(res: MatchingResultDTO): void {
    const matched = (res.matched || []).map(s => s.toLowerCase());
    const missing = (res.missingSkills || []).map(s => s.toLowerCase());

    // Merge all skills preserving original casing
    const allOriginal = [...(res.matched || []), ...(res.missingSkills || [])];
    const seen = new Set<string>();
    this.skillComparison = [];

    allOriginal.forEach(skill => {
      const lower = skill.toLowerCase();
      if (seen.has(lower)) return;
      seen.add(lower);

      if (matched.includes(lower)) {
        this.skillComparison.push({ skill, status: 'match' });
      } else if (missing.includes(lower)) {
        this.skillComparison.push({ skill, status: 'missing' });
      }
    });

    // Sort: matches first, then missing
    this.skillComparison.sort((a, b) => {
      const order = { match: 0, partial: 1, missing: 2 };
      return order[a.status] - order[b.status];
    });

    // Generate AI advice
    const missingNames = (res.missingSkills || []).slice(0, 5).join(', ');
    if (res.score >= 80) {
      this.aiAdvice = 'Votre profil correspond très bien à cette offre. Vous pouvez postuler avec confiance !';
    } else if (res.score >= 55) {
      this.aiAdvice = `Bon profil, mais renforcez : ${missingNames || 'certaines compétences'}. Commencez par des projets pratiques.`;
    } else {
      this.aiAdvice = `Plusieurs compétences manquantes : ${missingNames}. Consultez votre roadmap pour un plan d'apprentissage ciblé.`;
    }
  }

  getScoreColor(): string {
    if (!this.result) return 'var(--text-muted)';
    if (this.result.score >= 70) return 'var(--success)';
    if (this.result.score >= 45) return 'var(--warning)';
    return 'var(--danger)';
  }

  getStatusIcon(status: string): string {
    if (status === 'match')   return 'check-circle';
    if (status === 'partial') return 'alert-circle';
    return 'x-circle';
  }

  getStatusColor(status: string): string {
    if (status === 'match')   return '#10B981';
    if (status === 'partial') return '#F59E0B';
    return '#EF4444';
  }

  getStatusLabel(status: string): string {
    if (status === 'match')   return 'Match ✅';
    if (status === 'partial') return 'Partiel ⚠️';
    return 'Manquant ❌';
  }

  reset(): void {
    this.showResults    = false;
    this.result         = null;
    this.jobDescription = '';
    this.skillComparison = [];
    this.aiAdvice       = '';
    setTimeout(() => lucide.createIcons(), 50);
  }

  printReport(): void {
    const original = document.title;
    const cleanTitle = (this.result?.jobOfferTitle || 'Poste').replace(/[^\w\-]+/g, '_');
    document.title = `Rapport_Compatibilite_${cleanTitle}`;
    window.print();
    setTimeout(() => { document.title = original; }, 500);
  }
}
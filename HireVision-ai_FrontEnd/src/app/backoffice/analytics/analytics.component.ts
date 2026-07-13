import { AfterViewInit, Component, OnInit } from '@angular/core';
import { AnalyticsService, AnalyticsOverview } from '../../services/analytics.service';

declare const lucide: any;
declare function animateNumbers(): void;
declare function drawBarChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function drawDoughnutChart(canvasId: string, data: any[]): void;
declare function drawLineChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function showToast(message: string, type?: string): void;

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent implements OnInit, AfterViewInit {

  stats: AnalyticsOverview | null = null;
  loading = true;
  error = false;

  private readonly difficultyColors: Record<string, string> = {
    'Facile': '#10B981',
    'Moyen': '#F59E0B',
    'Difficile': '#EF4444',
    'Non défini': '#94A3B8'
  };

  private readonly scoreBucketColors: Record<string, string> = {
    '0-25%': '#EF4444',
    '25-50%': '#F59E0B',
    '50-75%': '#3B82F6',
    '75-100%': '#10B981'
  };

  constructor(private analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  ngAfterViewInit(): void {
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  loadStats(): void {
    this.loading = true;
    this.error = false;
    this.analyticsService.getOverview().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loading = false;
        setTimeout(() => this.renderCharts(), 0);
      },
      error: (err) => {
        console.error('Erreur de chargement des analytics', err);
        this.loading = false;
        this.error = true;
      }
    });
  }

  refreshData(): void {
    this.loadStats();
    showToast('Analytics actualisées !');
  }

  private renderCharts(): void {
    if (!this.stats) return;

    if (typeof lucide !== 'undefined') lucide.createIcons();
    animateNumbers();

    drawLineChart(
      'chart-score-trend',
      this.stats.scoreTrendLast6Months.map(p => ({ label: p.label, value: p.avgScore })),
      'Score moyen',
      '#4F46E5'
    );

    drawBarChart(
      'chart-cv-uploads',
      this.stats.cvUploadsLast7Days.map(d => ({ label: d.label, value: d.value })),
      'CV analysés',
      '#10B981'
    );

    drawDoughnutChart(
      'chart-matching-distribution',
      this.stats.matchingScoreDistribution.map(c => ({
        label: c.label,
        value: c.value,
        color: this.scoreBucketColors[c.label] || '#4F46E5'
      }))
    );

    drawDoughnutChart(
      'chart-difficulty',
      this.stats.interviewsByDifficulty.map(c => ({
        label: c.label,
        value: c.value,
        color: this.difficultyColors[c.label] || '#4F46E5'
      }))
    );
  }
}

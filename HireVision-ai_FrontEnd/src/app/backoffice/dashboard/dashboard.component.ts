import { AfterViewInit, Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AdminStatsService, DashboardStats } from '../../services/admin-stats.service';
import { SubscriptionService, SubscriptionStats } from '../../services/subscription.service';

declare const lucide: any;
declare function animateNumbers(): void;
declare function drawBarChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function drawDoughnutChart(canvasId: string, data: any[]): void;
declare function drawLineChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function showToast(message: string, type?: string): void;

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, AfterViewInit {

  stats: DashboardStats | null = null;
  subStats: SubscriptionStats | null = null;
  loading = true;
  error = false;

  private readonly difficultyColors: Record<string, string> = {
    'Facile': '#10B981',
    'Moyen': '#F59E0B',
    'Difficile': '#EF4444',
    'Non défini': '#94A3B8'
  };

  private readonly planColors: Record<string, string> = {
    'Gratuit': '#94A3B8',
    'Pro': '#4F46E5',
    'Premium': '#F59E0B'
  };

  constructor(
    private adminStatsService: AdminStatsService,
    private subscriptionService: SubscriptionService
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  ngAfterViewInit(): void {
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  loadStats(): void {
    this.loading = true;
    this.error = false;
    forkJoin({
      dashboard: this.adminStatsService.getOverview(),
      subscriptions: this.subscriptionService.getStatsOverview()
    }).subscribe({
      next: ({ dashboard, subscriptions }) => {
        this.stats = dashboard;
        this.subStats = subscriptions;
        this.loading = false;
        setTimeout(() => this.renderCharts(), 0);
      },
      error: (err) => {
        console.error('Erreur de chargement des stats dashboard', err);
        this.loading = false;
        this.error = true;
      }
    });
  }

  refreshData(): void {
    this.loadStats();
    showToast('Données actualisées !');
  }

  formatChange(value: number): string {
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value}%`;
  }

  private renderCharts(): void {
    if (!this.stats) return;

    if (typeof lucide !== 'undefined') lucide.createIcons();
    animateNumbers();

    drawLineChart(
      'chart-users',
      this.stats.newUsersLast7Days.map(d => ({ label: d.label, value: d.value })),
      'Inscrits',
      '#4F46E5'
    );

    drawDoughnutChart(
      'chart-categories',
      this.stats.interviewsByDifficulty.map(c => ({
        label: c.label,
        value: c.value,
        color: this.difficultyColors[c.label] || '#4F46E5'
      }))
    );

    if (this.subStats) {
      drawBarChart(
        'chart-revenue',
        this.subStats.revenueLast6Months.map(d => ({ label: d.label, value: d.value })),
        'Revenus €',
        '#4F46E5'
      );

      drawDoughnutChart(
        'chart-plans',
        this.subStats.planDistribution.map(p => ({
          label: p.label,
          value: p.value,
          color: this.planColors[p.label] || '#4F46E5'
        }))
      );
    }
  }
}

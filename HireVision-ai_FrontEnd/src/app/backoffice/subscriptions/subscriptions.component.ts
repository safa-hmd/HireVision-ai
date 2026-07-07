import { AfterViewInit, Component, OnInit } from '@angular/core';
import { SubscriptionService, SubscriptionStats } from '../../services/subscription.service';

declare const lucide: any;
declare function animateNumbers(): void;
declare function drawBarChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function drawDoughnutChart(canvasId: string, data: any[]): void;
declare function showToast(message: string, type?: string): void;

@Component({
  selector: 'app-subscriptions',
  templateUrl: './subscriptions.component.html',
  styleUrls: ['./subscriptions.component.css']
})
export class SubscriptionsComponent implements OnInit, AfterViewInit {

  stats: SubscriptionStats | null = null;
  loading = true;
  error = false;

  private readonly planColors: Record<string, string> = {
    'Gratuit': '#94A3B8',
    'Pro': '#4F46E5',
    'Premium': '#F59E0B'
  };

  constructor(private subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  ngAfterViewInit(): void {
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  loadStats(): void {
    this.loading = true;
    this.error = false;
    this.subscriptionService.getStatsOverview().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
        setTimeout(() => this.renderCharts(), 0);
      },
      error: (err) => {
        console.error('Erreur de chargement des stats abonnements', err);
        this.loading = false;
        this.error = true;
      }
    });
  }

  exportData(): void {
    showToast('Export généré !');
  }

  planCount(label: string): number {
    return this.stats?.planDistribution.find(p => p.label === label)?.value ?? 0;
  }

  statusBadgeClass(status: string): string {
    if (status === 'PAID') return 'badge-success';
    if (status === 'FAILED') return 'badge-danger';
    return 'badge-warning';
  }

  statusLabel(status: string): string {
    if (status === 'PAID') return 'Payé';
    if (status === 'FAILED') return 'Échoué';
    return 'En attente';
  }

  private renderCharts(): void {
    if (!this.stats) return;

    if (typeof lucide !== 'undefined') lucide.createIcons();
    animateNumbers();

    drawBarChart(
      'chart-rev',
      this.stats.revenueLast6Months.map(d => ({ label: d.label, value: d.value })),
      'Revenus €',
      '#4F46E5'
    );

    drawDoughnutChart(
      'chart-pie',
      this.stats.planDistribution.map(p => ({
        label: p.label,
        value: p.value,
        color: this.planColors[p.label] || '#4F46E5'
      }))
    );
  }
}

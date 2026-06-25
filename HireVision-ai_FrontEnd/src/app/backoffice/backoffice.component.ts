import { AfterViewInit, Component } from '@angular/core';

declare const lucide: any;
declare function animateNumbers(): void;
declare function drawBarChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function drawDoughnutChart(canvasId: string, data: any[]): void;
declare function drawLineChart(canvasId: string, data: any[], label?: string, color?: string): void;
declare function showToast(message: string, type?: string): void;

@Component({
  selector: 'app-backoffice',
  templateUrl: './backoffice.component.html',
  styleUrls: ['./backoffice.component.css']
})
export class BackofficeComponent implements AfterViewInit {
  ngAfterViewInit(): void {
    lucide.createIcons();
    animateNumbers();

    drawLineChart('chart-users', [
      { label: 'Lun', value: 28 },
      { label: 'Mar', value: 35 },
      { label: 'Mer', value: 22 },
      { label: 'Jeu', value: 41 },
      { label: 'Ven', value: 38 },
      { label: 'Sam', value: 18 },
      { label: 'Dim', value: 30 }
    ], 'Inscrits', '#4F46E5');

    drawDoughnutChart('chart-categories', [
      { label: 'Java', value: 35, color: '#4F46E5' },
      { label: 'Spring Boot', value: 25, color: '#10B981' },
      { label: 'Angular', value: 18, color: '#F59E0B' },
      { label: 'DevOps', value: 14, color: '#EF4444' },
      { label: 'RH', value: 8, color: '#7C3AED' }
    ]);

    drawBarChart('chart-revenue', [
      { label: 'Jan', value: 12000 },
      { label: 'Fév', value: 14500 },
      { label: 'Mar', value: 13200 },
      { label: 'Avr', value: 16800 },
      { label: 'Mai', value: 15400 },
      { label: 'Jun', value: 18450 }
    ], 'Revenus €', '#4F46E5');

    drawDoughnutChart('chart-plans', [
      { label: 'Gratuit', value: 45, color: '#94A3B8' },
      { label: 'Pro', value: 35, color: '#4F46E5' },
      { label: 'Premium', value: 20, color: '#F59E0B' }
    ]);
  }

  refreshData(): void {
    showToast('Données actualisées !');
  }
}

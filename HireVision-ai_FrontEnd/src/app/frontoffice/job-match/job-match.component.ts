import { AfterViewInit, Component } from '@angular/core';

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
  showResults = false;

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  analyzeJob(): void {
    if (!this.jobDescription.trim()) {
      showToast('Veuillez entrer une description de poste', 'warning');
      return;
    }

    showToast('Analyse en cours...', 'info');
    setTimeout(() => {
      this.showResults = true;
      setTimeout(() => {
        animateNumbers();
        drawDoughnutChart('chart-doughnut', [
          { label: 'Compétences correspondantes', value: 65, color: '#10B981' },
          { label: 'Compétences manquantes', value: 35, color: '#EF4444' }
        ]);
        showToast('Analyse terminée !', 'success');
        lucide.createIcons();
      });
    }, 600);
  }
}

import { Component, AfterViewInit, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';

declare const lucide: any;
declare function drawLineChart(id: string, data: any[]): void;
declare function drawBarChart(id: string, data: any[]): void;

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, AfterViewInit {

  userName = 'Jean';

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.userName = this.authService.getName() || 'Candidat';
  }

  ngAfterViewInit(): void {
    // 1. Icônes Lucide
    lucide.createIcons();

    // 2. Count-up animation
    this.animateNumbers();

    // 3. Charts
    drawLineChart('chart-perf', [
      { label: 'Lun', value: 65 }, { label: 'Mar', value: 72 },
      { label: 'Mer', value: 68 }, { label: 'Jeu', value: 78 },
      { label: 'Ven', value: 82 }, { label: 'Sam', value: 75 },
      { label: 'Dim', value: 85 }
    ]);
    drawBarChart('chart-skills', [
      { label: 'Java',    value: 85 },
      { label: 'Spring',  value: 75 },
      { label: 'Angular', value: 70 },
      { label: 'DevOps',  value: 60 }
    ]);
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

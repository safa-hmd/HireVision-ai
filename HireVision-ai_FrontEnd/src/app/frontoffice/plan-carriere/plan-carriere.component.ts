import { AfterViewInit, Component } from '@angular/core';

declare const lucide: any;
declare function animateNumbers(): void;

@Component({
  selector: 'app-plan-carriere',
  templateUrl: './plan-carriere.component.html',
  styleUrls: ['./plan-carriere.component.css']
})
export class PlanCarriereComponent implements AfterViewInit {
  ngAfterViewInit(): void {
    animateNumbers();
    lucide.createIcons();
  }
}

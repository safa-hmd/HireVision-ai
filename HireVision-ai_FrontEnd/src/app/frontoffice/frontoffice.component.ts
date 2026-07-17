import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-frontoffice',
  templateUrl: './frontoffice.component.html',
  styleUrls: ['./frontoffice.component.css']
})
export class FrontofficeComponent implements OnInit {
  showMenu = true;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateMenuVisibility(event.urlAfterRedirects || event.url);
    });
    this.updateMenuVisibility(this.router.url);
  }

  private updateMenuVisibility(url: string): void {
    this.showMenu = !url.includes('interview-session') && !url.includes('welcome');
  }
}

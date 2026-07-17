import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-not-found',
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.css']
})
export class NotFoundComponent {
  currentUrl: string;

  constructor(private router: Router, private auth: AuthService) {
    this.currentUrl = this.router.url;
  }


  goHome(): void {
    if (this.auth.isLoggedIn()) {
      const role = this.auth.normalizeRole(this.auth.getRole());
      this.router.navigate([role === 'ADMIN' ? '/backoffice/dashboard' : '/frontoffice/home']);
    } else {
      this.router.navigate(['/frontoffice/welcome']);
    }
  }
}

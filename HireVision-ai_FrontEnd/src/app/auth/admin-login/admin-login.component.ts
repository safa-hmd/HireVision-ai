import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-admin-login',
  templateUrl: './admin-login.component.html',
  styleUrls: ['./admin-login.component.css']
})
export class AdminLoginComponent {

  email    = '';
  password = '';
  loading  = false;
  errorMsg = '';

  emailTouched    = false;
  passwordTouched = false;

  constructor(private authService: AuthService, private router: Router) {}

  get emailInvalid(): boolean {
    return this.emailTouched && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email);
  }

  get passwordInvalid(): boolean {
    return this.passwordTouched && !this.password;
  }

  handleLogin(): void {
    this.emailTouched    = true;
    this.passwordTouched = true;
    this.errorMsg = '';

    if (this.emailInvalid || this.passwordInvalid || !this.email || !this.password) {
      return;
    }

    this.loading = true;

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (res) => {
        this.loading = false;
        const role = this.authService.normalizeRole(res.role);

        if (role === 'ADMIN') {
          this.router.navigate(['/backoffice']);
        } else {
          // Compte valide mais pas administrateur : on ne garde pas la session ouverte ici
          this.authService.logout();
          this.errorMsg = 'Ce compte ne dispose pas des droits administrateur.';
        }
      },
      error: () => {
        this.loading  = false;
        this.errorMsg = 'Email ou mot de passe incorrect.';
      }
    });
  }

  goToUserLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
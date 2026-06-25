import { Component } from '@angular/core';
import { AuthService } from 'src/app/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  email    = '';
  password = '';
  errorMsg = '';
  loading  = false;
  showPwd  = false;

  constructor(private authService: AuthService, private router: Router) {}

  handleLogin() {
    this.errorMsg = '';
    if (!this.email || !this.password) {
      this.errorMsg = 'Email et mot de passe requis.'; return;
    }

    this.loading = true;

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (res) => {
        this.loading = false;
        const role = res.role?.replace('ROLE_', '');
        if (role === 'ADMIN') {
          this.router.navigate(['/backoffice']);
        } else {
          this.router.navigate(['/frontoffice/home']);
        }
      },
      error: () => {
        this.loading  = false;
        this.errorMsg = 'Email ou mot de passe incorrect.';
      }
    });
  }

  loginWithGoogle(): void {
    this.authService.loginWithGoogle();
  }
}
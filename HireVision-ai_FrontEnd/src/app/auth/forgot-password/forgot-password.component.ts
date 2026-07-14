import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {

  email: string = '';
  loading: boolean = false;
  errorMsg: string = '';
  emailSent: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  handleForgot(): void {
    this.errorMsg = '';

    if (!this.email || !this.email.trim()) {
      this.errorMsg = 'Veuillez entrer une adresse email.';
      return;
    }

    this.loading = true;

    this.authService.forgotPassword(this.email).subscribe({
      next: () => {
        this.loading = false;
        this.emailSent = true;

        // ⚠️ Redirection auto vers reset-password (utile en dev,
        // mais en prod l'utilisateur doit cliquer sur le lien reçu par email)
        setTimeout(() => {
          this.router.navigate(['/reset-password']);
        }, 1500);
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = (err?.error as string) || 'Aucun compte associé à cet email.';
      }
    });
  }

  resendEmail(): void {
    this.emailSent = false;
    this.handleForgot();
  }
}
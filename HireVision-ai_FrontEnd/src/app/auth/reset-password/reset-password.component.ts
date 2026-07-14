import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {

  token: string = '';
  newPassword: string = '';
  confirmPassword: string = '';

  showNewPwd: boolean = false;
  showConfirmPwd: boolean = false;

  loading: boolean = false;
  success: boolean = false;
  errorMsg: string = '';
  matchError: boolean = false;

  // règles de complexité
  hasLength: boolean = false;
  hasUpper: boolean = false;
  hasNumber: boolean = false;

  strengthLevel: number = 0; // 0 à 4
  strengthLabel: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      if (!this.token) {
        this.errorMsg = 'Lien invalide ou expiré. Merci de refaire une demande.';
      }
    });
  }

  togglePwd(field: 'new' | 'confirm'): void {
    if (field === 'new') {
      this.showNewPwd = !this.showNewPwd;
    } else {
      this.showConfirmPwd = !this.showConfirmPwd;
    }
  }

  updateStrength(): void {
    const pwd = this.newPassword;

    this.hasLength = pwd.length >= 8;
    this.hasUpper = /[A-Z]/.test(pwd);
    this.hasNumber = /[0-9]/.test(pwd);

    let score = 0;
    if (this.hasLength) score++;
    if (this.hasUpper) score++;
    if (this.hasNumber) score++;
    if (pwd.length >= 12) score++;

    this.strengthLevel = score;

    switch (score) {
      case 0:
      case 1:
        this.strengthLabel = 'Faible';
        break;
      case 2:
        this.strengthLabel = 'Moyen';
        break;
      case 3:
        this.strengthLabel = 'Bon';
        break;
      case 4:
        this.strengthLabel = 'Excellent';
        break;
    }
  }

  handleReset(): void {
    this.errorMsg = '';
    this.matchError = false;

    if (!this.token) {
      this.errorMsg = 'Lien invalide ou expiré. Merci de refaire une demande.';
      return;
    }

    if (!this.hasLength || !this.hasUpper || !this.hasNumber) {
      this.errorMsg = 'Le mot de passe ne respecte pas les critères requis.';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.matchError = true;
      return;
    }

    this.loading = true;

    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = (err?.error as string) || 'Une erreur est survenue. Réessayez.';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
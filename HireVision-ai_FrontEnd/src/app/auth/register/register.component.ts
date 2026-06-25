import { Component } from '@angular/core';
import { AuthService } from 'src/app/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {

  firstName = '';
  lastName  = '';
  email     = '';
  age: number | null = null;
  password  = '';
  password2 = '';
  showPwd   = false;
  showPwd2  = false;
  loading   = false;
  errorMsg  = '';
  successMsg = '';

  score = 0;
  strengthColor = '#E2E8F0';
  strengthLabel = '';

  constructor(private authService: AuthService, private router: Router) {}

  checkStrength() {
    const v = this.password;
    const colors = ['#EF4444', '#F59E0B', '#3B82F6', '#10B981'];
    const labels = ['Faible', 'Moyen', 'Bien', 'Fort'];
    let s = 0;
    if (v.length >= 6)           s++;
    if (/[A-Z]/.test(v))         s++;
    if (/[0-9]/.test(v))         s++;
    if (/[^A-Za-z0-9]/.test(v))  s++;
    this.score = s;
    this.strengthColor = s > 0 ? colors[s - 1] : '#E2E8F0';
    this.strengthLabel = s > 0 ? labels[s - 1] : '';
  }

  handleSubmit() {
    this.errorMsg   = '';
    this.successMsg = '';

    if (!this.firstName || !this.lastName) {
      this.errorMsg = 'Le prénom et le nom sont requis.'; return;
    }
    if (!this.email) {
      this.errorMsg = "L'email est requis."; return;
    }
    if (!this.age || this.age < 18) {
      this.errorMsg = 'Vous devez avoir au moins 18 ans.'; return;
    }
    if (this.password.length < 6) {
      this.errorMsg = 'Le mot de passe doit contenir au moins 6 caractères.'; return;
    }
    if (this.password !== this.password2) {
      this.errorMsg = 'Les mots de passe ne correspondent pas.'; return;
    }

    this.loading = true;

    const payload = {
      fullName: this.firstName + ' ' + this.lastName,
      email:    this.email,
      password: this.password,
      age:      this.age,
      role:     'CANDIDATE' as 'ADMIN' | 'CANDIDATE'
    };

    this.authService.register(payload).subscribe({
      next: () => {
        this.successMsg = 'Compte créé avec succès !';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/auth/login']), 1500);
      },
      error: (err) => {
        this.errorMsg = err.error || 'Une erreur est survenue.';
        this.loading = false;
      }
    });
  }
}
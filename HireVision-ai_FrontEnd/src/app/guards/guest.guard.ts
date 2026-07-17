import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * GuestGuard — empêche les utilisateurs DÉJÀ connectés d'accéder aux pages d'authentification.
 * Exemple : si connecté et tu tapes /auth/login → redirigé vers /frontoffice/home ou /backoffice
 */
@Injectable({ providedIn: 'root' })
export class GuestGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (!this.auth.isLoggedIn()) {
      return true; // Pas connecté → accès autorisé à login/register
    }

    // Déjà connecté → redirige selon le rôle
    const role = this.auth.normalizeRole(this.auth.getRole());
    if (role === 'ADMIN') {
      this.router.navigate(['/backoffice/dashboard']);
    } else {
      this.router.navigate(['/frontoffice/home']);
    }
    return false;
  }
}

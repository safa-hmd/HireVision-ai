import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * RoleGuard — protège les routes accessibles uniquement aux ADMINs.
 * Si l'utilisateur est connecté mais n'est pas ADMIN → redirige vers /frontoffice/home
 * Si l'utilisateur n'est pas connecté → redirige vers /auth/login
 */
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/auth/login']);
      return false;
    }

    const role = this.auth.normalizeRole(this.auth.getRole());
    if (role === 'ADMIN') {
      return true;
    }

    // Rôle insuffisant → retour au dashboard candidat
    this.router.navigate(['/frontoffice/home']);
    return false;
  }
}

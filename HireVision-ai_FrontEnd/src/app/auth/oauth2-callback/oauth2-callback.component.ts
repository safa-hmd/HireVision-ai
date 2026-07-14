import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, AuthResponse } from 'src/app/services/auth.service';

@Component({
  selector: 'app-oauth2-callback',
  templateUrl: './oauth2-callback.component.html',
  styleUrls: ['./oauth2-callback.component.css']
})
export class Oauth2CallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const token  = params.get('token');
    const email  = params.get('email');
    const role   = params.get('role');
    const id     = params.get('id');

    if (!token || !email || !role || !id) {
      // Redirection sans token exploitable → retour au login avec message d'erreur
      this.router.navigate(['/auth/login'], { queryParams: { oauthError: '1' } });
      return;
    }

    const response: AuthResponse = {
      token,
      email,
      role,
      idUser: Number(id)
    };
    this.authService.storeSession(response);

    const cleanRole = this.authService.normalizeRole(role);
    if (cleanRole === 'ADMIN') {
      this.router.navigate(['/backoffice']);
    } else {
      this.router.navigate(['/frontoffice/home']);
    }
  }
}
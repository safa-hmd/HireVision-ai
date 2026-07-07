import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  age: number;
   role: 'ADMIN' | 'CANDIDATE';
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  role: string;

  idUser: number;

}

@Injectable({
  providedIn: 'root', 
})
export class AuthService {

  private baseUrl = 'http://localhost:8086/HireVision'; // ✅ port + context path corrects

  constructor(private http: HttpClient) {}

  register(req: RegisterRequest): Observable<string> {
    return this.http.post(
      `${this.baseUrl}/auth/register`, req,
      { responseType: 'text' }
    );
  }

  isLoggedIn(): boolean {
  const token = localStorage.getItem('TokenUserConnect');
  if (!token) return false;
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const isExpired = payload.exp * 1000 < Date.now();
    if (isExpired) {
      this.logout(); // nettoie automatiquement
      return false;
    }
    return true;
  } catch (e) {
    this.logout(); // token corrompu → nettoie
    return false;
  }
}

getToken(): string | null {
  if (!this.isLoggedIn()) return null; // vérifie expiration
  return localStorage.getItem('TokenUserConnect');
}

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      `${this.baseUrl}/auth/login`, req
    ).pipe(


      tap(response => {
        localStorage.setItem('TokenUserConnect', response.token);
        localStorage.setItem('EmailUserConnect', response.email);
        localStorage.setItem('RoleUserConnect',  response.role);

       localStorage.setItem('UserIdConnect', String(response.idUser)); 


      })
    );
  }

  logout(): void {
    localStorage.removeItem('TokenUserConnect');
    localStorage.removeItem('EmailUserConnect');
    localStorage.removeItem('RoleUserConnect');
    localStorage.removeItem('UserIdConnect');
  }

  getName(): string | null {
    const role = this.normalizeRole(this.getRole());
    switch (role) {
      case 'PLAYER': return 'Player';
      case 'SPONSOR': return 'Sponsor';
      case 'ADMIN': return 'Admin';
      case 'COMMUNITY_MANAGER': return 'Manager';
      default:
        const email = this.getEmail();
        if (email) {
          const namePart = email.split('@')[0];
          return namePart.charAt(0).toUpperCase() + namePart.slice(1);
        }
        return null;
    }
  }

  getRole(): string | null {
    return localStorage.getItem('RoleUserConnect');
  }

  normalizeRole(role: string | null): string {
    return role ? role.replace('ROLE_', '') : '';
  }

  getEmail(): string | null {
    return localStorage.getItem('EmailUserConnect');
  }

  getUserId(): string | null {
    return localStorage.getItem('UserIdConnect');
  }

  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8086/HireVision/oauth2/authorization/google';
  }

  loginWithGithub(): void {
    window.location.href = 'http://localhost:8086/HireVision/oauth2/authorization/github';
  }

  forgotPassword(email: string): Observable<string> {
    return this.http.post(
      `http://localhost:8086/HireVision/auth/forgot-password`,
      { email },
      { responseType: 'text' }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<string> {
    return this.http.post(
      `http://localhost:8086/HireVision/auth/reset-password`,
      { token, newPassword },
      { responseType: 'text' }
    );
  }

  getCurrentUserEmail(): string {
    return localStorage.getItem('EmailUserConnect') || '';
  }

  getCurrentUserId(): number {
    const id = localStorage.getItem('UserIdConnect');
    return id ? parseInt(id) : 0;
  }

  getUserIdByEmail(): Observable<number> {
    const email = localStorage.getItem('EmailUserConnect');
    return this.http.get<number>(
      `${this.baseUrl}/auth/getUserId?email=${email}`
    );
  }
}
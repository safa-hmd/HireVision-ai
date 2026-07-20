import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, AuthResponse } from './auth.service';

/** Construit un JWT factice (header.payload.signature) avec l'exp voulue, pour tester isLoggedIn/getToken sans dépendre d'un vrai secret. */
function fakeJwt(expInSeconds: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: 'test@test.com', exp: expInSeconds }));
  return `${header}.${payload}.signature`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('register', () => {
    it('should POST to /auth/register and expect a text response', () => {
      const req = { fullName: 'Adem', email: 'adem@test.com', password: 'password123', age: 25, role: 'CANDIDATE' as const };

      service.register(req).subscribe(res => expect(res).toBe('OK'));

      const call = httpMock.expectOne(`${baseUrl}/auth/register`);
      expect(call.request.method).toBe('POST');
      expect(call.request.body).toEqual(req);
      expect(call.request.responseType).toBe('text');
      call.flush('OK');
    });
  });

  describe('login', () => {
    it('should POST to /auth/login and store the session on success', () => {
      const response: AuthResponse = { token: fakeJwt(Math.floor(Date.now() / 1000) + 3600), email: 'adem@test.com', role: 'ROLE_CANDIDATE', idUser: 42 };

      service.login({ email: 'adem@test.com', password: 'password123' }).subscribe(res => {
        expect(res).toEqual(response);
      });

      const call = httpMock.expectOne(`${baseUrl}/auth/login`);
      expect(call.request.method).toBe('POST');
      call.flush(response);

      expect(localStorage.getItem('TokenUserConnect')).toBe(response.token);
      expect(localStorage.getItem('EmailUserConnect')).toBe('adem@test.com');
      expect(localStorage.getItem('RoleUserConnect')).toBe('ROLE_CANDIDATE');
      expect(localStorage.getItem('UserIdConnect')).toBe('42');
    });
  });

  describe('completeGoogleRegister', () => {
    it('should POST and store the session on success', () => {
      const response: AuthResponse = { token: fakeJwt(Math.floor(Date.now() / 1000) + 3600), email: 'google@test.com', role: 'ROLE_ADMIN', idUser: 7 };

      service.completeGoogleRegister({ email: 'google@test.com', fullName: 'Google User', role: 'ADMIN' })
        .subscribe(res => expect(res).toEqual(response));

      const call = httpMock.expectOne(`${baseUrl}/auth/complete-google-register`);
      expect(call.request.method).toBe('POST');
      call.flush(response);

      expect(localStorage.getItem('UserIdConnect')).toBe('7');
    });
  });

  describe('isLoggedIn / getToken', () => {
    it('should return false when there is no token', () => {
      expect(service.isLoggedIn()).toBeFalse();
      expect(service.getToken()).toBeNull();
    });

    it('should return true and the token when it is valid and not expired', () => {
      const token = fakeJwt(Math.floor(Date.now() / 1000) + 3600);
      localStorage.setItem('TokenUserConnect', token);

      expect(service.isLoggedIn()).toBeTrue();
      expect(service.getToken()).toBe(token);
    });

    it('should return false and clear the session when the token is expired', () => {
      const token = fakeJwt(Math.floor(Date.now() / 1000) - 3600);
      localStorage.setItem('TokenUserConnect', token);
      localStorage.setItem('EmailUserConnect', 'adem@test.com');

      expect(service.isLoggedIn()).toBeFalse();
      expect(localStorage.getItem('TokenUserConnect')).toBeNull();
      expect(localStorage.getItem('EmailUserConnect')).toBeNull();
    });

    it('should return false and clear the session when the token is malformed', () => {
      localStorage.setItem('TokenUserConnect', 'not-a-valid-jwt');

      expect(service.isLoggedIn()).toBeFalse();
      expect(localStorage.getItem('TokenUserConnect')).toBeNull();
    });
  });

  describe('logout', () => {
    it('should clear every session key from localStorage', () => {
      localStorage.setItem('TokenUserConnect', 'x');
      localStorage.setItem('EmailUserConnect', 'x');
      localStorage.setItem('RoleUserConnect', 'x');
      localStorage.setItem('UserIdConnect', 'x');

      service.logout();

      expect(localStorage.getItem('TokenUserConnect')).toBeNull();
      expect(localStorage.getItem('EmailUserConnect')).toBeNull();
      expect(localStorage.getItem('RoleUserConnect')).toBeNull();
      expect(localStorage.getItem('UserIdConnect')).toBeNull();
    });
  });

  describe('normalizeRole', () => {
    it('should strip the ROLE_ prefix', () => {
      expect(service.normalizeRole('ROLE_ADMIN')).toBe('ADMIN');
    });

    it('should return an empty string for null', () => {
      expect(service.normalizeRole(null)).toBe('');
    });

    it('should leave a role without prefix untouched', () => {
      expect(service.normalizeRole('CANDIDATE')).toBe('CANDIDATE');
    });
  });

  describe('getName', () => {
    it('should return "Admin" when the role is ADMIN', () => {
      localStorage.setItem('RoleUserConnect', 'ROLE_ADMIN');
      expect(service.getName()).toBe('Admin');
    });

    it('should fall back to the capitalized email local-part for an unmapped role', () => {
      localStorage.setItem('RoleUserConnect', 'ROLE_CANDIDATE');
      localStorage.setItem('EmailUserConnect', 'adem.trabelsi@test.com');
      expect(service.getName()).toBe('Adem.trabelsi');
    });

    it('should return null when there is no role and no email', () => {
      expect(service.getName()).toBeNull();
    });
  });

  describe('getCurrentUserId', () => {
    it('should return 0 when nothing is stored', () => {
      expect(service.getCurrentUserId()).toBe(0);
    });

    it('should parse the stored id', () => {
      localStorage.setItem('UserIdConnect', '17');
      expect(service.getCurrentUserId()).toBe(17);
    });
  });

  describe('forgotPassword / resetPassword', () => {
    it('should POST the email for forgotPassword', () => {
      service.forgotPassword('adem@test.com').subscribe(res => expect(res).toBe('sent'));

      const call = httpMock.expectOne(`${baseUrl}/auth/forgot-password`);
      expect(call.request.method).toBe('POST');
      expect(call.request.body).toEqual({ email: 'adem@test.com' });
      call.flush('sent');
    });

    it('should POST the token and new password for resetPassword', () => {
      service.resetPassword('reset-tok', 'newPassword123').subscribe(res => expect(res).toBe('reset'));

      const call = httpMock.expectOne(`${baseUrl}/auth/reset-password`);
      expect(call.request.method).toBe('POST');
      expect(call.request.body).toEqual({ token: 'reset-tok', newPassword: 'newPassword123' });
      call.flush('reset');
    });
  });

  describe('getUserIdByEmail', () => {
    it('should GET using the stored email as a query param', () => {
      localStorage.setItem('EmailUserConnect', 'adem@test.com');

      service.getUserIdByEmail().subscribe(id => expect(id).toBe(42));

      const call = httpMock.expectOne(`${baseUrl}/auth/getUserId?email=adem@test.com`);
      expect(call.request.method).toBe('GET');
      call.flush(42);
    });
  });
});

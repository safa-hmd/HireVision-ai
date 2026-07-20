import { TestBed } from '@angular/core/testing';
import { HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JwtInterceptor } from './jwt.interceptor';
import { AuthService } from './auth.service';

describe('JwtInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getToken']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should add an Authorization header when a token is present', () => {
    authServiceSpy.getToken.and.returnValue('my-jwt-token');

    http.get('/api/whatever').subscribe();

    const call = httpMock.expectOne('/api/whatever');
    expect(call.request.headers.get('Authorization')).toBe('Bearer my-jwt-token');
    call.flush({});
  });

  it('should not add an Authorization header when there is no token', () => {
    authServiceSpy.getToken.and.returnValue(null);

    http.get('/api/whatever').subscribe();

    const call = httpMock.expectOne('/api/whatever');
    expect(call.request.headers.has('Authorization')).toBeFalse();
    call.flush({});
  });
});

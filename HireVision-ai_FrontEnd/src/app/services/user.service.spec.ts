import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserService, UserDTO, GitHubAnalysis } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/users';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getById should GET /users/{id}', () => {
    const mock = { idUser: 1, fullName: 'Adem' } as UserDTO;
    service.getById(1).subscribe(res => expect(res).toEqual(mock));
    httpMock.expectOne(`${baseUrl}/1`).flush(mock);
  });

  it('getAll should GET /users', () => {
    service.getAll().subscribe(res => expect(res).toEqual([]));
    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('update should PUT /users/{id}', () => {
    const dto = { fullName: 'Adem Ben', email: 'adem@test.com', age: 26 } as UserDTO;
    service.update(1, dto).subscribe(res => expect(res).toEqual({ ...dto, idUser: 1 }));

    const call = httpMock.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(dto);
    call.flush({ ...dto, idUser: 1 });
  });

  it('delete should DELETE /users/{id}', () => {
    service.delete(1).subscribe(res => expect(res).toBeNull());
    const call = httpMock.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('DELETE');
    call.flush(null);
  });

  it('uploadPicture should POST a multipart FormData with the file', () => {
    const file = new File(['img'], 'photo.png', { type: 'image/png' });
    const mock = { idUser: 1, profilePicture: 'user_1_photo.png' } as UserDTO;

    service.uploadPicture(1, file).subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/1/upload-picture`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body instanceof FormData).toBeTrue();
    expect((call.request.body as FormData).get('file')).toBe(file);
    call.flush(mock);
  });

  it('getPictureUrl should build the full picture URL', () => {
    expect(service.getPictureUrl('user_1_photo.png')).toBe(`${baseUrl}/pictures/user_1_photo.png`);
  });

  it('create should POST /users', () => {
    const dto = { fullName: 'New User', email: 'new@test.com', age: 30 } as UserDTO;
    service.create(dto).subscribe(res => expect(res).toEqual({ ...dto, idUser: 2 }));

    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('POST');
    call.flush({ ...dto, idUser: 2 });
  });

  it('analyzeGithub should GET /users/{id}/analyze-github', () => {
    const mock = { username: 'adem', total_repos: 12 } as GitHubAnalysis;
    service.analyzeGithub(1).subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/1/analyze-github`);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { InterviewService, InterviewDTO } from './interview.service';

describe('InterviewService', () => {
  let service: InterviewService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/interviews';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [InterviewService]
    });
    service = TestBed.inject(InterviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAll should GET /interviews', () => {
    service.getAll().subscribe(res => expect(res).toEqual([]));
    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('getById should GET /interviews/{id}', () => {
    const mock = { id: 3 } as InterviewDTO;
    service.getById(3).subscribe(res => expect(res).toEqual(mock));
    httpMock.expectOne(`${baseUrl}/3`).flush(mock);
  });

  it('getByUserId should GET /interviews/user/{userId}', () => {
    service.getByUserId(42).subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/user/42`).flush([]);
  });

  it('getHistoryByUserId should GET /interviews/user/{userId}/sorted', () => {
    service.getHistoryByUserId(42).subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/user/42/sorted`).flush([]);
  });

  it('getByDateRange should GET with start/end query params', () => {
    service.getByDateRange(42, '2026-01-01T00:00:00', '2026-01-31T23:59:59').subscribe(res => expect(res).toEqual([]));

    const call = httpMock.expectOne(
      r => r.url === `${baseUrl}/user/42/range`
        && r.params.get('start') === '2026-01-01T00:00:00'
        && r.params.get('end') === '2026-01-31T23:59:59'
    );
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('countByUserId should GET /interviews/user/{userId}/count', () => {
    service.countByUserId(42).subscribe(res => expect(res).toBe(7));
    httpMock.expectOne(`${baseUrl}/user/42/count`).flush(7);
  });

  it('delete should DELETE /interviews/{id}', () => {
    service.delete(3).subscribe(res => expect(res).toBeNull());
    const call = httpMock.expectOne(`${baseUrl}/3`);
    expect(call.request.method).toBe('DELETE');
    call.flush(null);
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MatchingService, JobMatchRequestDTO, MatchingResultDTO } from './matching.service';

describe('MatchingService', () => {
  let service: MatchingService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/matching-results';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MatchingService]
    });
    service = TestBed.inject(MatchingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('matchAndSave should POST /matching-results/match', () => {
    const request: JobMatchRequestDTO = { cvId: 1, cvSkills: ['Java'], jobSkills: ['Java', 'Docker'] };
    const mock: MatchingResultDTO = { id: 1, score: 72.5, cvId: 1, missingSkills: ['Docker'] };

    service.matchAndSave(request).subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/match`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush(mock);
  });

  it('getByCvId should GET /matching-results/cv/{cvId}', () => {
    service.getByCvId(1).subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/cv/1`).flush([]);
  });

  it('getByUserId should GET /matching-results/user/{userId}', () => {
    service.getByUserId(42).subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/user/42`).flush([]);
  });

  it('getBestByCvId should GET /matching-results/cv/{cvId}/best', () => {
    const mock: MatchingResultDTO = { id: 1, score: 90, cvId: 1 };
    service.getBestByCvId(1).subscribe(res => expect(res).toEqual(mock));
    httpMock.expectOne(`${baseUrl}/cv/1/best`).flush(mock);
  });
});

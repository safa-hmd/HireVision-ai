import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalyticsService, AnalyticsOverview } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/admin/analytics';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AnalyticsService]
    });
    service = TestBed.inject(AnalyticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getOverview should GET /admin/analytics/overview and return the analytics data', () => {
    const mock: AnalyticsOverview = {
      totalCvsAnalyzed: 55,
      totalMatchings: 40,
      avgMatchingScore: 66.6,
      cvUploadsLast7Days: [],
      matchingScoreDistribution: [],
      topMissingSkills: [{ label: 'Docker', value: 12 }],
      interviewsByDifficulty: [],
      scoreTrendLast6Months: [{ label: 'Jan', avgScore: 70 }]
    };

    service.getOverview().subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/overview`);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminStatsService, DashboardStats } from './admin-stats.service';

describe('AdminStatsService', () => {
  let service: AdminStatsService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/admin/stats';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminStatsService]
    });
    service = TestBed.inject(AdminStatsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getOverview should GET /admin/stats/overview and return the dashboard stats', () => {
    const mockStats: DashboardStats = {
      totalUsers: 120,
      totalUsersChangePercent: 5.2,
      totalInterviewsThisMonth: 34,
      interviewsChangePercent: 1.1,
      avgGlobalScore: 78.4,
      avgGlobalScoreChangePercent: 2.0,
      newUsersLast7Days: [{ label: 'Lun', value: 3 }],
      interviewsByDifficulty: [{ label: 'EASY', value: 10 }],
      recentUsers: [],
      recentInterviews: []
    };

    service.getOverview().subscribe(stats => expect(stats).toEqual(mockStats));

    const call = httpMock.expectOne(`${baseUrl}/overview`);
    expect(call.request.method).toBe('GET');
    call.flush(mockStats);
  });

  it('getOverview should propagate an HTTP error', () => {
    service.getOverview().subscribe({
      next: () => fail('should have failed'),
      error: (err) => expect(err.status).toBe(500)
    });

    httpMock.expectOne(`${baseUrl}/overview`).flush('error', { status: 500, statusText: 'Server Error' });
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { SubscriptionService, SubscriptionInfo, SubscriptionStats } from './subscription.service';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/subscriptions';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [SubscriptionService]
    });
    service = TestBed.inject(SubscriptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getStatsOverview should GET /subscriptions/admin/overview', () => {
    const mock = { mrr: 1000 } as SubscriptionStats;
    service.getStatsOverview().subscribe(res => expect(res).toEqual(mock));
    const call = httpMock.expectOne(`${baseUrl}/admin/overview`);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });

  it('subscribe should POST userId and plan', () => {
    const mock = { id: 1, plan: 'PRO', status: 'ACTIVE' } as SubscriptionInfo;

    service.subscribe(42, 'PRO').subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/subscribe`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual({ userId: 42, plan: 'PRO' });
    call.flush(mock);
  });

  it('cancel should POST to /subscriptions/cancel/{userId}', () => {
    const mock = { id: 1, status: 'CANCELED' } as SubscriptionInfo;

    service.cancel(42).subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/cancel/42`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual({});
    call.flush(mock);
  });

  it('getByUserId should GET /subscriptions/user/{userId}', () => {
    service.getByUserId(42).subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/user/42`).flush([]);
  });

  it('getPaymentsByUserId should GET /subscriptions/user/{userId}/payments', () => {
    service.getPaymentsByUserId(42).subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/user/42/payments`).flush([]);
  });
});

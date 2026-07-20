import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PlanService, PlanDTO } from './plan.service';

describe('PlanService', () => {
  let service: PlanService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/plans';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PlanService]
    });
    service = TestBed.inject(PlanService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAllPlans should GET /plans', () => {
    const mock: PlanDTO[] = [
      { key: 'PRO', name: 'Pro', price: 29, tagline: '', highlighted: false, features: [] },
      { key: 'PREMIUM', name: 'Premium', price: 59, tagline: '', highlighted: true, features: [] }
    ];

    service.getAllPlans().subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });

  it('updatePlan should PUT /plans/{key}', () => {
    const dto: PlanDTO = { key: 'PRO', name: 'Pro Plus', price: 39, tagline: 'Nouvelle offre', highlighted: true, features: ['Support prioritaire'] };

    service.updatePlan('PRO', dto).subscribe(res => expect(res).toEqual(dto));

    const call = httpMock.expectOne(`${baseUrl}/PRO`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(dto);
    call.flush(dto);
  });
});

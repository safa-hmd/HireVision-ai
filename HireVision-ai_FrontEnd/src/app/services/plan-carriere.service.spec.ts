import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PlanCarriereService, MissedSkillDTO, LearningPlanDTO } from './plan-carriere.service';

describe('PlanCarriereService', () => {
  let service: PlanCarriereService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PlanCarriereService]
    });
    service = TestBed.inject(PlanCarriereService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getMissedSkills should GET /missed-skills/user/{userId}', () => {
    const mock: MissedSkillDTO[] = [{ id: 1, skillName: 'Docker', priority: 'HAUTE', estimatedWeeks: 1, matchingResultId: 1 }];

    service.getMissedSkills(42).subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/missed-skills/user/42`);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });

  it('getLearningPlans should GET /learning-plans/user/{userId}', () => {
    const mock: LearningPlanDTO[] = [{ id: 1, title: 'Docker en 1 semaine', content: '...', resourceUrl: 'https://...', weekNumber: 1, source: 'JOB_MATCHING', missedSkillId: 1, interviewId: null }];

    service.getLearningPlans(42).subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(`${baseUrl}/learning-plans/user/42`);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });
});

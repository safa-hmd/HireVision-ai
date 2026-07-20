import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { QuestionService, QuestionDTO } from './question.service';

describe('QuestionService', () => {
  let service: QuestionService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/questions';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [QuestionService]
    });
    service = TestBed.inject(QuestionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getAll should GET /questions', () => {
    const mock: QuestionDTO[] = [{ id: 1, content: 'Qu\'est-ce que Java ?', difficulty: 'EASY', interviewId: 1 }];

    service.getAll().subscribe(res => expect(res).toEqual(mock));

    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('GET');
    call.flush(mock);
  });

  it('getByDifficulty should GET /questions/difficulty/{difficulty}', () => {
    service.getByDifficulty('HARD').subscribe(res => expect(res).toEqual([]));

    const call = httpMock.expectOne(`${baseUrl}/difficulty/HARD`);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('delete should DELETE /questions/{id}', () => {
    service.delete(1).subscribe(res => expect(res).toBeNull());

    const call = httpMock.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('DELETE');
    call.flush(null);
  });
});

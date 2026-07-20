import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { JobOfferService, JobOfferDTO } from './job-offer.service';

describe('JobOfferService', () => {
  let service: JobOfferService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/job-offers';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [JobOfferService]
    });
    service = TestBed.inject(JobOfferService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('create should POST the job offer', () => {
    const dto: JobOfferDTO = { title: 'Dev Java', requiredSkills: ['Java'] };
    const created = { ...dto, id: 1 };

    service.create(dto).subscribe(res => expect(res).toEqual(created));

    const call = httpMock.expectOne(baseUrl);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(dto);
    call.flush(created);
  });

  it('update should PUT /job-offers/{id}', () => {
    const dto: JobOfferDTO = { title: 'Dev Java Senior', requiredSkills: ['Java'] };

    service.update(1, dto).subscribe(res => expect(res).toEqual({ ...dto, id: 1 }));

    const call = httpMock.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('PUT');
    call.flush({ ...dto, id: 1 });
  });

  it('delete should DELETE /job-offers/{id}', () => {
    service.delete(1).subscribe(res => expect(res).toBeNull());
    const call = httpMock.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('DELETE');
    call.flush(null);
  });

  it('getById should GET /job-offers/{id}', () => {
    const mock = { id: 1, title: 'Dev Java', requiredSkills: [] } as JobOfferDTO;
    service.getById(1).subscribe(res => expect(res).toEqual(mock));
    httpMock.expectOne(`${baseUrl}/1`).flush(mock);
  });

  it('getAll should GET /job-offers', () => {
    service.getAll().subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(baseUrl).flush([]);
  });

  it('getActive should GET /job-offers/active', () => {
    service.getActive().subscribe(res => expect(res).toEqual([]));
    httpMock.expectOne(`${baseUrl}/active`).flush([]);
  });

  it('search should GET /job-offers/search with a keyword param', () => {
    service.search('Java').subscribe(res => expect(res).toEqual([]));
    const call = httpMock.expectOne(r => r.url === `${baseUrl}/search` && r.params.get('keyword') === 'Java');
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });
});

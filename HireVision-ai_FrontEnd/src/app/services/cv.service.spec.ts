import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CvService, CvDTO, CvUploadResponse } from './cv.service';

describe('CvService', () => {
  let service: CvService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8086/HireVision/cvs';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CvService]
    });
    service = TestBed.inject(CvService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('uploadAndAnalyze should POST a multipart FormData with file and userId', () => {
    const file = new File(['contenu'], 'cv.pdf', { type: 'application/pdf' });
    const mockResponse = { cv: { id: 1 } as CvDTO, analysis: {} } as CvUploadResponse;

    service.uploadAndAnalyze(file, 42).subscribe(res => expect(res).toEqual(mockResponse));

    const call = httpMock.expectOne(`${baseUrl}/upload-and-analyze`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body instanceof FormData).toBeTrue();
    const body = call.request.body as FormData;
    expect(body.get('file')).toBe(file);
    expect(body.get('userId')).toBe('42');
    call.flush(mockResponse);
  });

  it('upload should POST a multipart FormData and return the CvDTO', () => {
    const file = new File(['contenu'], 'cv.pdf', { type: 'application/pdf' });
    const mockCv: CvDTO = { id: 1, filePath: 'cv.pdf', uploadDate: '2026-01-01', userId: 42, skillNames: ['Java'] };

    service.upload(file, 42).subscribe(res => expect(res).toEqual(mockCv));

    const call = httpMock.expectOne(`${baseUrl}/upload`);
    expect(call.request.method).toBe('POST');
    call.flush(mockCv);
  });

  it('getById should GET /cvs/{id}', () => {
    const mockCv = { id: 5 } as CvDTO;
    service.getById(5).subscribe(res => expect(res).toEqual(mockCv));
    httpMock.expectOne(`${baseUrl}/5`).flush(mockCv);
  });

  it('getByUserId should GET /cvs/user/{userId}', () => {
    service.getByUserId(42).subscribe(res => expect(res).toEqual([]));
    const call = httpMock.expectOne(`${baseUrl}/user/42`);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('getLatest should GET /cvs/user/{userId}/latest', () => {
    const mockCv = { id: 9 } as CvDTO;
    service.getLatest(42).subscribe(res => expect(res).toEqual(mockCv));
    httpMock.expectOne(`${baseUrl}/user/42/latest`).flush(mockCv);
  });

  it('getLatestAnalysis should GET /cvs/user/{userId}/latest-analysis', () => {
    const mock = { cv: {}, analysis: {} } as CvUploadResponse;
    service.getLatestAnalysis(42).subscribe(res => expect(res).toEqual(mock));
    httpMock.expectOne(`${baseUrl}/user/42/latest-analysis`).flush(mock);
  });

  it('delete should DELETE /cvs/{id}', () => {
    service.delete(5).subscribe(res => expect(res).toBeNull());
    const call = httpMock.expectOne(`${baseUrl}/5`);
    expect(call.request.method).toBe('DELETE');
    call.flush(null);
  });
});

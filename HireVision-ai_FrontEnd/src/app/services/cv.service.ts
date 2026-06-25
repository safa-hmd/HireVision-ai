import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CvDTO {
  id?: number;
  filePath: string;
  uploadDate: string;
  userId: number;
  skillNames: string[];
}

@Injectable({
  providedIn: 'root'
})
export class CvService {
  private baseUrl = 'http://localhost:8086/HireVision/cvs';

  constructor(private http: HttpClient) {}

  upload(file: File, userId: number): Observable<CvDTO> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', String(userId));
    return this.http.post<CvDTO>(`${this.baseUrl}/upload`, formData);
  }

  getById(id: number): Observable<CvDTO> {
    return this.http.get<CvDTO>(`${this.baseUrl}/${id}`);
  }

  getByUserId(userId: number): Observable<CvDTO[]> {
    return this.http.get<CvDTO[]>(`${this.baseUrl}/user/${userId}`);
  }

  getLatest(userId: number): Observable<CvDTO> {
    return this.http.get<CvDTO>(`${this.baseUrl}/user/${userId}/latest`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

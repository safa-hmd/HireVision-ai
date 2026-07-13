import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface JobOfferDTO {
  id?: number;
  title: string;
  company?: string;
  description?: string;
  active?: boolean;
  createdAt?: string;
  requiredSkills: string[];
}

@Injectable({ providedIn: 'root' })
export class JobOfferService {
  private baseUrl = 'http://localhost:8086/HireVision/job-offers';

  constructor(private http: HttpClient) {}

  create(dto: JobOfferDTO): Observable<JobOfferDTO> {
    return this.http.post<JobOfferDTO>(this.baseUrl, dto);
  }

  update(id: number, dto: JobOfferDTO): Observable<JobOfferDTO> {
    return this.http.put<JobOfferDTO>(`${this.baseUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getById(id: number): Observable<JobOfferDTO> {
    return this.http.get<JobOfferDTO>(`${this.baseUrl}/${id}`);
  }

  getAll(): Observable<JobOfferDTO[]> {
    return this.http.get<JobOfferDTO[]>(this.baseUrl);
  }

  /** Offres publiées, utilisées côté candidat pour le matching */
  getActive(): Observable<JobOfferDTO[]> {
    return this.http.get<JobOfferDTO[]>(`${this.baseUrl}/active`);
  }

  search(keyword: string): Observable<JobOfferDTO[]> {
    return this.http.get<JobOfferDTO[]>(`${this.baseUrl}/search`, { params: { keyword } });
  }
}

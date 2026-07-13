import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface JobMatchRequestDTO {
  cvId: number;
  cvSkills: string[];
  jobSkills: string[];
  jobOfferId?: number | null;
}

export interface MatchingResultDTO {
  id?: number;
  score: number;
  cvId: number;
  jobOfferId?: number;
  jobOfferTitle?: string;
  label?: string;
  message?: string;
  compatible?: boolean;
  matched?: string[];
  missingSkills?: string[];
  missedSkills?: { id?: number; skillName: string; matchingResultId: number }[];
}

@Injectable({ providedIn: 'root' })
export class MatchingService {
  private baseUrl = 'http://localhost:8086/HireVision/matching-results';

  constructor(private http: HttpClient) {}

  matchAndSave(request: JobMatchRequestDTO): Observable<MatchingResultDTO> {
    return this.http.post<MatchingResultDTO>(`${this.baseUrl}/match`, request);
  }

  getByCvId(cvId: number): Observable<MatchingResultDTO[]> {
    return this.http.get<MatchingResultDTO[]>(`${this.baseUrl}/cv/${cvId}`);
  }

  getByUserId(userId: number): Observable<MatchingResultDTO[]> {
    return this.http.get<MatchingResultDTO[]>(`${this.baseUrl}/user/${userId}`);
  }

  getBestByCvId(cvId: number): Observable<MatchingResultDTO> {
    return this.http.get<MatchingResultDTO>(`${this.baseUrl}/cv/${cvId}/best`);
  }
}
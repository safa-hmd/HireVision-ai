import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface MissedSkillDTO {
  id?: number;
  skillName: string;
  matchingResultId: number;
}

export interface MatchingResultDTO {
  id?: number;
  score: number;
  cvId: number;
  missedSkills?: MissedSkillDTO[];
}

@Injectable({
  providedIn: 'root'
})
export class MatchingService {
  private baseUrl = 'http://localhost:8086/HireVision/matching-results';

  constructor(private http: HttpClient) {}

  create(dto: MatchingResultDTO): Observable<MatchingResultDTO> {
    return this.http.post<MatchingResultDTO>(`${this.baseUrl}/add`, dto);
  }

  getById(id: number): Observable<MatchingResultDTO> {
    return this.http.get<MatchingResultDTO>(`${this.baseUrl}/${id}`);
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

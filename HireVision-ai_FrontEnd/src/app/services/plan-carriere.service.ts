import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface MissedSkillDTO {
  id: number;
  skillName: string;
  priority: 'HAUTE' | 'MOYENNE' | 'BASSE';
  estimatedWeeks: number;
  matchingResultId: number;
}

export interface LearningPlanDTO {
  id: number;
  title: string;
  content: string;
  resourceUrl: string;
  weekNumber: number | null;
  source: 'JOB_MATCHING' | 'INTERVIEW';
  missedSkillId: number | null;
  interviewId: number | null;
}

@Injectable({ providedIn: 'root' })
export class PlanCarriereService {
  private baseUrl = 'http://localhost:8086/HireVision';

  constructor(private http: HttpClient) {}

  getMissedSkills(userId: number): Observable<MissedSkillDTO[]> {
    return this.http.get<MissedSkillDTO[]>(`${this.baseUrl}/missed-skills/user/${userId}`);
  }

  getLearningPlans(userId: number): Observable<LearningPlanDTO[]> {
    return this.http.get<LearningPlanDTO[]>(`${this.baseUrl}/learning-plans/user/${userId}`);
  }
}
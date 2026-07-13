import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CategoryCount, DayCount } from './admin-stats.service';

export interface ScorePoint {
  label: string;
  avgScore: number;
}

export interface AnalyticsOverview {
  totalCvsAnalyzed: number;
  totalMatchings: number;
  avgMatchingScore: number;
  cvUploadsLast7Days: DayCount[];
  matchingScoreDistribution: CategoryCount[];
  topMissingSkills: CategoryCount[];
  interviewsByDifficulty: CategoryCount[];
  scoreTrendLast6Months: ScorePoint[];
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private baseUrl = 'http://localhost:8086/HireVision/admin/analytics';

  constructor(private http: HttpClient) {}

  getOverview(): Observable<AnalyticsOverview> {
    return this.http.get<AnalyticsOverview>(`${this.baseUrl}/overview`);
  }
}

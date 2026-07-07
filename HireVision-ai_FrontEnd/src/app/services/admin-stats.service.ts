import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface DayCount {
  label: string;
  value: number;
}

export interface CategoryCount {
  label: string;
  value: number;
}

export interface RecentUser {
  idUser: number;
  fullName: string;
  email: string;
  role: string;
  profilePicture?: string;
}

export interface RecentInterview {
  id: number;
  candidateName: string;
  difficulty: string;
  globalScore: number | null;
  durationMinutes: number;
  startDate: string;
}

export interface DashboardStats {
  totalUsers: number;
  totalUsersChangePercent: number;
  totalInterviewsThisMonth: number;
  interviewsChangePercent: number;
  avgGlobalScore: number;
  avgGlobalScoreChangePercent: number;
  newUsersLast7Days: DayCount[];
  interviewsByDifficulty: CategoryCount[];
  recentUsers: RecentUser[];
  recentInterviews: RecentInterview[];
}

@Injectable({
  providedIn: 'root'
})
export class AdminStatsService {
  private baseUrl = 'http://localhost:8086/HireVision/admin/stats';

  constructor(private http: HttpClient) {}

  getOverview(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/overview`);
  }
}

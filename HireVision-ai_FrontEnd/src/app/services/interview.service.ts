import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RecentInterview } from './admin-stats.service';

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private baseUrl = 'http://localhost:8086/HireVision/interviews';

  constructor(private http: HttpClient) {}

  /** Toutes les interviews, pour la vue admin */
  getAll(): Observable<RecentInterview[]> {
    return this.http.get<RecentInterview[]>(this.baseUrl);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

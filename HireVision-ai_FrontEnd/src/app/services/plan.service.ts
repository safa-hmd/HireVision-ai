import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PlanDTO {
  key: 'PRO' | 'PREMIUM';
  name: string;
  price: number;
  tagline: string;
  highlighted: boolean;
  features: string[];
}

@Injectable({ providedIn: 'root' })
export class PlanService {
  private baseUrl = 'http://localhost:8086/HireVision/plans';

  constructor(private http: HttpClient) {}

  getAllPlans(): Observable<PlanDTO[]> {
    return this.http.get<PlanDTO[]>(this.baseUrl);
  }

  updatePlan(key: 'PRO' | 'PREMIUM', dto: PlanDTO): Observable<PlanDTO> {
    return this.http.put<PlanDTO>(`${this.baseUrl}/${key}`, dto);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CategoryCount, DayCount } from './admin-stats.service';

export interface PaymentTransaction {
  id: number;
  userId: number;
  userFullName: string;
  plan: string;
  amount: number;
  status: 'PAID' | 'FAILED' | 'PENDING';
  paymentDate: string;
}

export interface SubscriptionStats {
  mrr: number;
  mrrChangePercent: number;
  arr: number;
  payingSubscribersCount: number;
  totalUsers: number;
  arpu: number;
  planDistribution: CategoryCount[];
  revenueLast6Months: DayCount[];
  recentTransactions: PaymentTransaction[];
}

export interface SubscriptionInfo {
  id: number;
  userId: number;
  userFullName: string;
  plan: string;
  status: 'ACTIVE' | 'CANCELED' | 'EXPIRED';
  startDate: string;
  renewalDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class SubscriptionService {
  private baseUrl = 'http://localhost:8086/HireVision/subscriptions';

  constructor(private http: HttpClient) {}

  getStatsOverview(): Observable<SubscriptionStats> {
    return this.http.get<SubscriptionStats>(`${this.baseUrl}/admin/overview`);
  }

  subscribe(userId: number, plan: 'PRO' | 'PREMIUM'): Observable<SubscriptionInfo> {
    return this.http.post<SubscriptionInfo>(`${this.baseUrl}/subscribe`, { userId, plan });
  }

  cancel(userId: number): Observable<SubscriptionInfo> {
    return this.http.post<SubscriptionInfo>(`${this.baseUrl}/cancel/${userId}`, {});
  }

  getByUserId(userId: number): Observable<SubscriptionInfo[]> {
    return this.http.get<SubscriptionInfo[]>(`${this.baseUrl}/user/${userId}`);
  }

  getPaymentsByUserId(userId: number): Observable<PaymentTransaction[]> {
    return this.http.get<PaymentTransaction[]>(`${this.baseUrl}/user/${userId}/payments`);
  }
}

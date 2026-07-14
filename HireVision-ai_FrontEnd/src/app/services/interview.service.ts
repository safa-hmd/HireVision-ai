import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RecentInterview } from './admin-stats.service';

// ── Sous-structures alignées sur le backend réel ──
export interface QuestionDTO {
  id?: number;
  text?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD';
}

export interface FeedbackDTO {
  technicalScore?: number;
  communicationScore?: number;
  confidenceScore?: number;
  eyeContactScore?: number;
}

export interface VoiceAnalysisDTO {
  [key: string]: any;
}

export interface BehaviorAnalysisDTO {
  [key: string]: any;
}

// ── DTO principal, aligné sur InterviewDTO.java côté backend ──
export interface InterviewDTO {
  id?: number;
  startDate: string;
  durationMinutes: number;
  userId: number;
  cvId: number;
  questions: QuestionDTO[];
  feedback: FeedbackDTO | null;
  voiceAnalysis: VoiceAnalysisDTO | null;
  behaviorAnalysis: BehaviorAnalysisDTO | null;
}

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private baseUrl = 'http://localhost:8086/HireVision/interviews';

  constructor(private http: HttpClient) {}

  /** Toutes les interviews, pour la vue admin */
  getAll(): Observable<RecentInterview[]> {
    return this.http.get<RecentInterview[]>(this.baseUrl);
  }

  getById(id: number): Observable<InterviewDTO> {
    return this.http.get<InterviewDTO>(`${this.baseUrl}/${id}`);
  }

  getByUserId(userId: number): Observable<InterviewDTO[]> {
    return this.http.get<InterviewDTO[]>(`${this.baseUrl}/user/${userId}`);
  }

  /** Historique trié par date décroissante — utilisé dans l'onglet profil */
  getHistoryByUserId(userId: number): Observable<InterviewDTO[]> {
    return this.http.get<InterviewDTO[]>(`${this.baseUrl}/user/${userId}/sorted`);
  }

  getByDateRange(userId: number, start: string, end: string): Observable<InterviewDTO[]> {
    return this.http.get<InterviewDTO[]>(
      `${this.baseUrl}/user/${userId}/range`,
      { params: { start, end } }
    );
  }

  countByUserId(userId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/user/${userId}/count`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
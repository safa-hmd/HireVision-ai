import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AnswerDTO {
  id?: number;
  answerText: string;
  score: number;
  aiComment?: string;
}

export interface QuestionDTO {
  id: number;
  content: string;
  difficulty: string;
  interviewId: number;
  answer?: AnswerDTO;
}

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private baseUrl = 'http://localhost:8086/HireVision/questions';

  constructor(private http: HttpClient) {}

  /** Toutes les questions posées, pour la vue admin */
  getAll(): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(this.baseUrl);
  }

  getByDifficulty(difficulty: string): Observable<QuestionDTO[]> {
    return this.http.get<QuestionDTO[]>(`${this.baseUrl}/difficulty/${difficulty}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

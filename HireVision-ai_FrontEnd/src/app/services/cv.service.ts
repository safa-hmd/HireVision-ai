import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CvDTO {
  id?: number;
  filePath: string;
  uploadDate: string;
  userId: number;
  skillNames: string[];
}

export interface CvAnalysis {
  skills: string[];
  education: { degree: string; institution: string; period: string; }[];
  experience: { title: string; company: string; period: string; description: string; }[];
  projects: { title: string; period: string; description: string; }[];
  certifications: string[];
  languages: { language: string; level: string; }[];
  summary: string;
  // Nouveaux champs ML
  profile?: string;
  confidence?: number;
  global_score?: number;
  skill_scores?: { [key: string]: number };
  proposed_summary?: string;
  optimization_suggestions?: string[];
  strengths?: string[];
  weaknesses?: string[];
  recommendations?: string[];
}

export interface CvUploadResponse {
  cv: CvDTO;
  analysis: CvAnalysis;
}

@Injectable({ providedIn: 'root' })
export class CvService {
  private baseUrl = 'http://localhost:8086/HireVision/cvs';

  constructor(private http: HttpClient) {}

  // ← Nouveau endpoint avec analyse IA
  uploadAndAnalyze(file: File, userId: number): Observable<CvUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', String(userId));
    return this.http.post<CvUploadResponse>(`${this.baseUrl}/upload-and-analyze`, formData);
  }

  // Anciens endpoints conservés
  upload(file: File, userId: number): Observable<CvDTO> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', String(userId));
    return this.http.post<CvDTO>(`${this.baseUrl}/upload`, formData);
  }

  getById(id: number): Observable<CvDTO> {
    return this.http.get<CvDTO>(`${this.baseUrl}/${id}`);
  }

  getByUserId(userId: number): Observable<CvDTO[]> {
    return this.http.get<CvDTO[]>(`${this.baseUrl}/user/${userId}`);
  }

  getLatest(userId: number): Observable<CvDTO> {
    return this.http.get<CvDTO>(`${this.baseUrl}/user/${userId}/latest`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getLatestAnalysis(userId: number): Observable<CvUploadResponse> {
  return this.http.get<CvUploadResponse>(`${this.baseUrl}/user/${userId}/latest-analysis`);
}
}
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface UserDTO {
  idUser?: number;
  fullName: string;
  email: string;
  age: number;
  role?: string;
  phone?: string;
  title?: string;
  linkedin?: string;
  github?: string;
  profilePicture?: string;
}

export interface GitHubAnalysis {
  username: string;
  profile_url: string;
  avatar_url: string;
  total_repos: number;
  followers: number;
  stars: number;
  top_languages: string[];
  score: number;
  has_dockerfile: boolean;
  has_workflows: boolean;
  has_tests: boolean;
  has_readme: boolean;
  recommendations: string[];
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private baseUrl = 'http://localhost:8086/HireVision/users';

  constructor(private http: HttpClient) {}

  getById(id: number): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.baseUrl}/${id}`);
  }

  getAll(): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(this.baseUrl);
  }

  update(id: number, dto: UserDTO): Observable<UserDTO> {
    return this.http.put<UserDTO>(`${this.baseUrl}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadPicture(id: number, file: File): Observable<UserDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UserDTO>(`${this.baseUrl}/${id}/upload-picture`, formData);
  }

  getPictureUrl(filename: string): string {
    return `http://localhost:8086/HireVision/users/pictures/${filename}`;
  }

  create(dto: UserDTO): Observable<UserDTO> {
    return this.http.post<UserDTO>(this.baseUrl, dto);
  }

  /** Analyse le profil GitHub lié au compte utilisateur */
  analyzeGithub(id: number): Observable<GitHubAnalysis> {
    return this.http.get<GitHubAnalysis>(`${this.baseUrl}/${id}/analyze-github`);
  }
}


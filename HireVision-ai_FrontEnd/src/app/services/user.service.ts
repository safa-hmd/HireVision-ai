import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface UserDTO {
  idUser?: number;
  fullName: string;
  email: string;
  age: number;
  role?: string;
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
}

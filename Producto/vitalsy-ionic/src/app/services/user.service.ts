import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserProfile {
  id?: number;
  nombre: string;
  email: string;
  pesoActual: number;
  altura: number;
  tipoInsulina: string;
  ratioIc: number;
  factorIs: number;
  alertasGlucosa: boolean;
  recordatorioComidas: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/usuarios`;

  getUserProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/perfil`);
  }

  updateUserProfile(profileData: Partial<UserProfile>): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/perfil`, profileData);
  }
}

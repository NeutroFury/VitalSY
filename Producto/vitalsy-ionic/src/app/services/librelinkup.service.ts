import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LibreLinkUpStatus {
  configurado: boolean;
  email?: string;
  activo?: boolean;
  ultimoSync?: string;
}

export interface SyncResponse {
  mensaje: string;
  nuevosRegistros: number;
}

@Injectable({
  providedIn: 'root'
})
export class LibreLinkUpService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/librelinkup`;

  getStatus(): Observable<LibreLinkUpStatus> {
    return this.http.get<LibreLinkUpStatus>(`${this.apiUrl}/status`);
  }

  setup(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/setup`, credentials);
  }

  forceSync(): Observable<SyncResponse> {
    return this.http.post<SyncResponse>(`${this.apiUrl}/sync`, {});
  }

  disconnect(): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/disconnect`);
  }
}

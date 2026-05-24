import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
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

  private syncSuccessSubject = new BehaviorSubject<SyncResponse | null>(null);
  public syncSuccess$ = this.syncSuccessSubject.asObservable();

  getStatus(): Observable<LibreLinkUpStatus> {
    return this.http.get<LibreLinkUpStatus>(`${this.apiUrl}/status`);
  }

  setup(credentials: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/setup`, credentials).pipe(
      tap((res) => {
        this.syncSuccessSubject.next({ mensaje: 'Vínculo establecido', nuevosRegistros: 0 });
      })
    );
  }

  forceSync(): Observable<SyncResponse> {
    return this.http.post<SyncResponse>(`${this.apiUrl}/sync`, {}).pipe(
      tap((res) => {
        this.syncSuccessSubject.next(res);
      })
    );
  }

  disconnect(): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/disconnect`).pipe(
      tap((res) => {
        this.syncSuccessSubject.next({ mensaje: 'Desconectado', nuevosRegistros: 0 });
      })
    );
  }
}

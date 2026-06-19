import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RecordatorioResponse {
  id: number;
  tipo: 'MEDICION_GLUCOSA' | 'APLICACION_INSULINA' | 'COMIDA';
  hora: string; // "HH:mm:ss"
  diasRepeticion: string; // "1,2,3" etc
  activo: boolean;
  fechaCreacion?: string;
}

export interface RecordatorioRequest {
  tipo: 'MEDICION_GLUCOSA' | 'APLICACION_INSULINA' | 'COMIDA';
  hora: string;
  diasRepeticion: string;
  activo: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RecordatorioService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/recordatorios`;

  getRecordatorios(): Observable<RecordatorioResponse[]> {
    return this.http.get<RecordatorioResponse[]>(this.apiUrl);
  }

  createRecordatorio(data: RecordatorioRequest): Observable<RecordatorioResponse> {
    return this.http.post<RecordatorioResponse>(this.apiUrl, data);
  }

  updateRecordatorio(id: number, data: RecordatorioRequest): Observable<RecordatorioResponse> {
    return this.http.put<RecordatorioResponse>(`${this.apiUrl}/${id}`, data);
  }

  deleteRecordatorio(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

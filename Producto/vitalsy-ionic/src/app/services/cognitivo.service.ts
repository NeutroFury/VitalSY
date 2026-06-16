import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CognitivoResponse {
  mensaje: string;
  reglasExtraidas: number;
  error?: string;
}

export interface CalculoDosisRequest {
  usuarioId: number;
  nombreComida: string;
  glicemiaActual: number;
  carbohidratosGr: number;
}

export interface CalculoDosisResponse {
  dosisRecomendada: number;
  metodoCalculo: string;
  mensajeInfo?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CognitivoService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/cognitivo`;
  private calcUrl = `${environment.apiUrl}/calculadora`;

  subirPautaMedica(files: File[]): Observable<CognitivoResponse> {
    const formData = new FormData();
    for (const file of files) {
      formData.append('files', file);
    }
    return this.http.post<CognitivoResponse>(`${this.apiUrl}/pauta`, formData);
  }

  getComidas(usuarioId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.calcUrl}/comidas/${usuarioId}`);
  }

  calcularDosis(request: CalculoDosisRequest): Observable<CalculoDosisResponse> {
    return this.http.post<CalculoDosisResponse>(`${this.calcUrl}/calcular`, request);
  }
}

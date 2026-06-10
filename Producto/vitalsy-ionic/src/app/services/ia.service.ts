import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface IaAnalysis {
  tendencia: string;
  nivel_de_riesgo: string;
  consejo_breve: string;
}

export interface ChatResponse {
  respuesta: string;
}

@Injectable({
  providedIn: 'root'
})
export class IaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ia`;

  getLatestAnalysis(): Observable<IaAnalysis> {
    console.log('IA_SERVICE: Iniciando petición de análisis...');
    return this.http.post<IaAnalysis | null>(`${this.apiUrl}/analizar-ultima`, {}).pipe(
      map(res => {
        if (!res) {
          return {
            tendencia: 'ESTABLE',
            nivel_de_riesgo: 'BAJO',
            consejo_breve: 'No hay suficientes datos históricos para un análisis clínico'
          };
        }
        return res;
      }),
      catchError(err => {
        console.error('IA_SERVICE: Error en la petición:', err);
        if (err.status === 204) {
          return of({
            tendencia: 'ESTABLE',
            nivel_de_riesgo: 'BAJO',
            consejo_breve: 'No hay suficientes datos históricos para un análisis clínico'
          });
        }
        return throwError(() => new Error('IA_SERVER_UNAVAILABLE'));
      })
    );
  }

  enviarMensajeChat(mensaje: string): Observable<ChatResponse> {
    console.log('IA_SERVICE: Enviando mensaje de chat...');
    return this.http.post<ChatResponse>(`${this.apiUrl}/chat`, { mensaje }).pipe(
      catchError(err => {
        console.error('IA_SERVICE: Error en el chat:', err);
        return throwError(() => new Error('IA_SERVER_UNAVAILABLE'));
      })
    );
  }
}

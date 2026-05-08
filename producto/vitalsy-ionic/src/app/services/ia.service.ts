import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';

export interface IaAnalysis {
  tendencia: string;
  nivel_de_riesgo: string;
  consejo_breve: string;
}

@Injectable({
  providedIn: 'root'
})
export class IaService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ia`;

  getLatestAnalysis(): Observable<IaAnalysis> {
    console.log('IA_SERVICE: Iniciando petición de análisis...');
    return this.http.post<IaAnalysis>(`${this.apiUrl}/analizar-ultima`, {}).pipe(
      catchError(err => {
        console.error('IA_SERVICE: Error en la petición:', err);
        return throwError(() => new Error('IA_SERVER_UNAVAILABLE'));
      })
    );
  }
}

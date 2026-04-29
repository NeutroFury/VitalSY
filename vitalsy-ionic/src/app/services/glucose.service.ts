import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class GlucoseService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/glucosa`;

  saveReading(valor: number, tendencia: string) {
    return this.http.post(`${this.apiUrl}/registrar`, { valorMgdl: valor, tendencia });
  }
}

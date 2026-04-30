import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { NotificationService } from './notification.service';

export interface GlucoseReadingDto {
  id: number;
  valorMgdl: number;
  tendencia: string | null;
  tipoRegistro: string;
  fechaHora: string;
  analisisIa: string | null;
  carbohidratos: number | null;
  comentarios: string | null;
}

@Injectable({ providedIn: 'root' })
export class GlucoseService {
  private http = inject(HttpClient);
  private notificationService = inject(NotificationService);
  private apiUrl = `${environment.apiUrl}/glucosa`;

  public refreshDashboard$ = new Subject<void>();

  registrarGlucosa(lectura: any): Observable<GlucoseReadingDto> {
    return this.http.post<GlucoseReadingDto>(this.apiUrl, lectura).pipe(
      tap((res) => {
        this.checkCriticalValue(res);
        this.refreshDashboard$.next();
      })
    );
  }

  private checkCriticalValue(reading: GlucoseReadingDto) {
    if (reading.valorMgdl > 250) {
      this.notificationService.scheduleUrgentNotification(
        '¡Alerta: Hiperglucemia Crítica!',
        `Tu nivel de glucosa es de ${reading.valorMgdl} mg/dL. Toma acción inmediata.`,
        'critical'
      );
    } else if (reading.valorMgdl < 60) {
      this.notificationService.scheduleUrgentNotification(
        '¡Alerta: Hipoglucemia Crítica!',
        `Tu nivel de glucosa es muy bajo (${reading.valorMgdl} mg/dL). Consume azúcar rápido.`,
        'critical'
      );
    }
  }

  saveReading(valor: number, tendencia: string) {
    return this.registrarGlucosa({ valorMgdl: valor, tendencia });
  }

  getRecentReadings(): Observable<GlucoseReadingDto[]> {
    return this.http.get<GlucoseReadingDto[]>(`${this.apiUrl}/ultimas`);
  }

  getAllReadings(): Observable<GlucoseReadingDto[]> {
    return this.http.get<GlucoseReadingDto[]>(`${this.apiUrl}/historial`);
  }
}

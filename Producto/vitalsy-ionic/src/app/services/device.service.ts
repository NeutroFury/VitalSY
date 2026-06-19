import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface FcmTokenPayload {
  fcmToken: string;
  plataforma: 'android' | 'ios' | 'web';
}

/**
 * Servicio para registrar y desactivar tokens FCM en el backend.
 * El token nunca se expone en respuestas (204 No Content).
 */
@Injectable({
  providedIn: 'root'
})
export class DeviceService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/dispositivos`;

  /**
   * Registra o actualiza el token FCM del dispositivo actual.
   * Upsert: si el token ya existe, lo reactiva.
   */
  registrarFcmToken(payload: FcmTokenPayload): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/fcm-token`, payload);
  }

  /**
   * Desactiva el token FCM (llamar al hacer logout).
   */
  desactivarFcmToken(fcmToken: string, plataforma: 'android' | 'ios' | 'web'): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/fcm-token`, {
      body: { fcmToken, plataforma }
    });
  }
}

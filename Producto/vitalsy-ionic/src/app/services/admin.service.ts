import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PacienteAdmin {
  id: number;
  nombre: string;
  email: string;
  pesoActual: number;
  altura: number;
  insulinaLenta: string;
  insulinaRapida: string;
  ratioIc: number;
  factorIs: number;
  rangoGlucosaMin: number;
  rangoGlucosaMax: number;
  zonaHoraria: string;
  rol: string;
  activo: boolean;
}

export interface AdminStats {
  totalUsuarios: number;
  totalPacientes: number;
  totalMedicos: number;
  totalAdmins: number;
  totalActivos: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/admin`;

  getPacientes(): Observable<PacienteAdmin[]> {
    return this.http.get<PacienteAdmin[]>(`${this.apiUrl}/usuarios`);
  }

  getPacienteById(id: number): Observable<PacienteAdmin> {
    return this.http.get<PacienteAdmin>(`${this.apiUrl}/usuarios/${id}`);
  }

  getTodos(): Observable<PacienteAdmin[]> {
    return this.http.get<PacienteAdmin[]>(`${this.apiUrl}/usuarios/todos`);
  }

  getUltimosRegistrosGlucosa(usuarioId: number): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/registros/usuario/${usuarioId}/ultimos`);
  }

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}/stats`);
  }

  cambiarRol(id: number, rol: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/usuarios/${id}/rol`, { rol });
  }

  toggleStatus(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/usuarios/${id}/toggle-status`, {});
  }

  triggerPasswordReset(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/usuarios/${id}/trigger-password-reset`, {});
  }
}

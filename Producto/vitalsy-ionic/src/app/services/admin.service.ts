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

  getTodos(): Observable<PacienteAdmin[]> {
    return this.http.get<PacienteAdmin[]>(`${this.apiUrl}/usuarios/todos`);
  }

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}/stats`);
  }

  cambiarRol(id: number, rol: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/usuarios/${id}/rol`, { rol });
  }
}

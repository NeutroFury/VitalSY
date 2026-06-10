import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  nombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/auth`;

  private authState = new BehaviorSubject<boolean>(this.hasToken());

  constructor() {}

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  get isLoggedIn$(): Observable<boolean> {
    return this.authState.asObservable();
  }

  login(credentials: any): Observable<AuthResponse> {
    return this.http.post<any>(`${this.apiUrl}/login`, credentials).pipe(
      tap(res => {
        const displayName = res.nombre || res.email || 'Usuario';
        this.saveAuthData(res.token, String(res.userId), displayName);

        // Extraer y guardar el rol desde el JWT
        const rol = this.decodeRolFromToken(res.token);
        localStorage.setItem('rol', rol);

        // Actualizar zona horaria del usuario al hacer login
        const zonaHoraria = Intl.DateTimeFormat().resolvedOptions().timeZone;
        localStorage.setItem('zonaHoraria', zonaHoraria);
        this.http.put(`${environment.apiUrl}/usuarios/perfil`, { zonaHoraria }).subscribe({
          error: (err) => console.error('Error actualizando timezone en login', err)
        });

        this.authState.next(true);
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  /**
   * Decodifica el payload del JWT (sin verificar firma) para extraer el claim 'rol'.
   * El JWT tiene el formato: header.payload.signature (Base64URL)
   */
  decodeRolFromToken(token: string): string {
    try {
      const payload = token.split('.')[1];
      // Base64URL → Base64 → JSON
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      return decoded.rol ?? 'PACIENTE';
    } catch {
      return 'PACIENTE';
    }
  }

  private saveAuthData(token: string, userId: string, username: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('userId', userId);
    localStorage.setItem('username', username);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUserId(): string | null {
    return localStorage.getItem('userId');
  }

  getUsername(): string {
    const name = localStorage.getItem('username');
    if (!name || name === 'undefined') {
      return 'Usuario';
    }
    return name;
  }

  getRol(): string {
    return localStorage.getItem('rol') ?? 'PACIENTE';
  }

  isAdmin(): boolean {
    return this.getRol() === 'ADMIN';
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('rol');
    this.authState.next(false);
  }
}

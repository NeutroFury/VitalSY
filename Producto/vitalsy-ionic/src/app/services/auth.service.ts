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
        this.authState.next(true);
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
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

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    this.authState.next(false);
  }
}

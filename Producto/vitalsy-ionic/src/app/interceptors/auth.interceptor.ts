import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  const isAuthRequest = req.url.includes('/api/v1/auth/');

  console.log('INTERCEPTOR: Petición saliente a:', req.url);

  if (token && !isAuthRequest) {
    console.log('INTERCEPTOR: Token encontrado, inyectando header...');
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(authReq);
  }

  if (isAuthRequest) {
    console.log('INTERCEPTOR: Omitiendo token para auth.');
  }

  console.warn('INTERCEPTOR: No se encontró token para esta petición.');
  return next(req);
};

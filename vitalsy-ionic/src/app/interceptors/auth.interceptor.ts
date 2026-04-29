import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  console.log('INTERCEPTOR: Petición saliente a:', req.url);

  if (token) {
    console.log('INTERCEPTOR: Token encontrado, inyectando header...');
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(authReq);
  }

  console.warn('INTERCEPTOR: No se encontró token para esta petición.');
  return next(req);
};

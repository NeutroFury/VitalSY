import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map, take } from 'rxjs';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isLoggedIn$.pipe(
    take(1),
    map(isLoggedIn => {
      if (!isLoggedIn) {
        router.navigate(['/login']);
        return false;
      }
      if (!authService.isAdmin()) {
        // Usuario autenticado pero sin permisos → redirigir al dashboard
        router.navigate(['/dashboard']);
        return false;
      }
      return true;
    })
  );
};

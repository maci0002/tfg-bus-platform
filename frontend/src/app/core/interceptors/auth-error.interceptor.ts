import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * Detecta respuestas 401 (token ausente, expirado o usuario inexistente):
 *  - Limpia la sesión local (token + datos de usuario).
 *  - Redirige a /login si la URL fallida es una operación protegida.
 *
 * No actúa sobre 403 — esos indican "estás logueado pero no tienes permiso"
 * y los manejan los componentes individualmente.
 */
export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  return next(req).pipe(
    catchError(err => {
      if (err.status === 401) {
        tokenStorage.clear();

        // Solo redirigir si la petición era a un endpoint protegido
        // (no si era el propio /auth/login fallando por contraseña incorrecta)
        const isAuthEndpoint = req.url.includes('/auth/login') || req.url.includes('/auth/register');
        if (!isAuthEndpoint) {
          router.navigate(['/login'], {
            queryParams: { sessionExpired: 'true' },
          });
        }
      }
      return throwError(() => err);
    }),
  );
};

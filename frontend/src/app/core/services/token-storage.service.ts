import { Injectable } from '@angular/core';
import { AuthResponse } from '../models/auth.model';

const TOKEN_KEY = 'bp_token';
const USER_KEY  = 'bp_user';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {

  /**
   * Se usa `sessionStorage` (no `localStorage`): la sesión vive únicamente
   * mientras la pestaña del navegador permanece abierta. Al cerrarla, el token
   * desaparece y la aplicación vuelve a exigir inicio de sesión. Sobrevive a un
   * refresco de página (no expulsa al usuario por un F5), pero no queda guardada
   * de forma persistente en el disco.
   */
  private readonly storage: Storage = sessionStorage;

  saveSession(auth: AuthResponse): void {
    this.storage.setItem(TOKEN_KEY, auth.token);
    const { token, ...user } = auth;
    this.storage.setItem(USER_KEY, JSON.stringify(user));
  }

  getToken(): string | null {
    const token = this.storage.getItem(TOKEN_KEY);
    if (!token) return null;
    // Si el JWT ha caducado, se descarta la sesión y se trata como no autenticado.
    if (this.isExpired(token)) {
      this.clear();
      return null;
    }
    return token;
  }

  getUser(): Omit<AuthResponse, 'token'> | null {
    // Solo hay usuario si el token sigue siendo válido.
    if (!this.getToken()) return null;
    const raw = this.storage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  clear(): void {
    this.storage.removeItem(TOKEN_KEY);
    this.storage.removeItem(USER_KEY);
  }

  /** Decodifica el claim `exp` del JWT y comprueba si ya ha caducado. */
  private isExpired(token: string): boolean {
    try {
      const part = token.split('.')[1] ?? '';
      const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
      const pad = base64.length % 4 ? '='.repeat(4 - (base64.length % 4)) : '';
      const payload = JSON.parse(atob(base64 + pad));
      if (!payload?.exp) return false;        // sin exp → no se puede saber, se acepta
      return payload.exp * 1000 <= Date.now();
    } catch {
      return true;                            // token mal formado → inválido
    }
  }
}

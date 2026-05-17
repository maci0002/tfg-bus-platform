import { Injectable } from '@angular/core';
import { AuthResponse } from '../models/auth.model';

const TOKEN_KEY = 'bp_token';
const USER_KEY  = 'bp_user';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {

  saveSession(auth: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, auth.token);
    const { token, ...user } = auth;
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUser(): Omit<AuthResponse, 'token'> | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}

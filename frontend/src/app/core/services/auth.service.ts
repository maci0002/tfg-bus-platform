import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly baseUrl = `${environment.apiUrl}/auth`;

  // inject() a nivel de campo para que tokenStorage esté disponible
  // antes de que se inicialice _currentUser
  private tokenStorage = inject(TokenStorageService);
  private http         = inject(HttpClient);
  private router       = inject(Router);

  private _currentUser = signal<Omit<AuthResponse, 'token'> | null>(
    this.tokenStorage.getUser()
  );

  readonly currentUser = this._currentUser.asReadonly();
  readonly isLoggedIn  = computed(() => !!this._currentUser());
  readonly userName    = computed(() => {
    const u = this._currentUser();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });

  constructor() {}

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, payload).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap(res => this.handleAuthResponse(res))
    );
  }

  logout(): void {
    this.tokenStorage.clear();
    this._currentUser.set(null);
    this.router.navigate(['/']);
  }

  private handleAuthResponse(res: AuthResponse): void {
    this.tokenStorage.saveSession(res);
    const { token, ...user } = res;
    this._currentUser.set(user);
  }
}

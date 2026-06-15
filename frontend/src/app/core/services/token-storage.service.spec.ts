import { TokenStorageService } from './token-storage.service';
import { AuthResponse } from '../models/auth.model';

/**
 * Pruebas del almacenamiento de sesión en `sessionStorage`: persistencia del
 * token y del usuario, y descarte automático del token cuando el JWT ha caducado.
 */
describe('TokenStorageService', () => {
  let service: TokenStorageService;

  /** Construye un JWT de prueba (sin firma real) con el `exp` indicado. */
  function fakeJwt(expEpochSeconds: number): string {
    const b64url = (obj: unknown) =>
      btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    const header = b64url({ alg: 'HS256', typ: 'JWT' });
    const payload = b64url({ sub: 'alice@example.com', exp: expEpochSeconds });
    return `${header}.${payload}.signature`;
  }

  function authResponse(token: string): AuthResponse {
    return { token, email: 'alice@example.com', firstName: 'Alice', lastName: 'Pérez', role: 'USER' };
  }

  beforeEach(() => {
    sessionStorage.clear();
    service = new TokenStorageService();
  });

  it('guarda y recupera el token y el usuario de una sesión válida', () => {
    const token = fakeJwt(Math.floor(Date.now() / 1000) + 3600); // +1 hora
    service.saveSession(authResponse(token));

    expect(service.getToken()).toBe(token);
    expect(service.isLoggedIn()).toBe(true);
    expect(service.getUser()).toEqual({
      email: 'alice@example.com',
      firstName: 'Alice',
      lastName: 'Pérez',
      role: 'USER',
    });
  });

  it('descarta el token cuando el JWT ha caducado', () => {
    const token = fakeJwt(Math.floor(Date.now() / 1000) - 60); // hace 1 minuto
    service.saveSession(authResponse(token));

    expect(service.getToken()).toBeNull();
    expect(service.isLoggedIn()).toBe(false);
    // Al caducar, la sesión se limpia completamente.
    expect(sessionStorage.getItem('bp_token')).toBeNull();
  });

  it('trata un token mal formado como no autenticado', () => {
    service.saveSession(authResponse('esto-no-es-un-jwt'));
    expect(service.getToken()).toBeNull();
  });

  it('clear() elimina la sesión almacenada', () => {
    const token = fakeJwt(Math.floor(Date.now() / 1000) + 3600);
    service.saveSession(authResponse(token));

    service.clear();

    expect(service.getToken()).toBeNull();
    expect(service.getUser()).toBeNull();
  });
});

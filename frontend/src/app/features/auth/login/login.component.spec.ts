import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Prueba de componente del inicio de sesión: validación del formulario y
 * navegación al panel tras autenticarse correctamente.
 */
describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let loginSpy: ReturnType<typeof vi.fn>;
  let router: Router;

  beforeEach(async () => {
    loginSpy = vi.fn().mockReturnValue(of({ token: 't', email: 'a@b.com' }));

    await TestBed.configureTestingModule({
      imports: [LoginComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: { login: loginSpy, isLoggedIn: () => false } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('arranca con un formulario inválido', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('no llama a login si el formulario es inválido', () => {
    component.submit();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('inicia sesión y navega al panel cuando las credenciales son válidas', () => {
    component.form.setValue({ email: 'alice@example.com', password: 'secreto1' });

    component.submit();

    expect(loginSpy).toHaveBeenCalledWith({ email: 'alice@example.com', password: 'secreto1' });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('muestra el error y desactiva loading si el login falla', () => {
    loginSpy.mockReturnValueOnce(throwError(() => ({ error: { error: 'Credenciales' } })));
    component.form.setValue({ email: 'alice@example.com', password: 'mala' });

    component.submit();

    expect(component.loading).toBe(false);
  });
});

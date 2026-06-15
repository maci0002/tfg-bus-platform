import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Prueba de componente del registro: validación del formulario, coincidencia de
 * contraseñas y envío del alta sin el campo de confirmación.
 */
describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let registerSpy: ReturnType<typeof vi.fn>;
  let router: Router;

  const fillValid = () => component.form.setValue({
    firstName: 'Lucía',
    lastName: 'Moya',
    email: 'lucia@example.com',
    password: 'claveSegura1',
    confirmPassword: 'claveSegura1',
  });

  beforeEach(async () => {
    registerSpy = vi.fn().mockReturnValue(of({ token: 't', email: 'lucia@example.com' }));

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, TranslateModule.forRoot()],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AuthService, useValue: { register: registerSpy, isLoggedIn: () => false } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('arranca con un formulario inválido', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('detecta contraseñas que no coinciden', () => {
    fillValid();
    component.confirmPassword.setValue('otraCosa');
    component.form.updateValueAndValidity();
    expect(component.confirmPassword.hasError('passwordMismatch')).toBe(true);
  });

  it('exige una contraseña de al menos 8 caracteres', () => {
    component.password.setValue('corta');
    expect(component.password.hasError('minlength')).toBe(true);
  });

  it('registra al usuario sin enviar el campo de confirmación', () => {
    fillValid();

    component.submit();

    expect(registerSpy).toHaveBeenCalledWith({
      firstName: 'Lucía',
      lastName: 'Moya',
      email: 'lucia@example.com',
      password: 'claveSegura1',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });
});

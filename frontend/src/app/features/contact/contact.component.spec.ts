import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { ContactComponent } from './contact.component';
import { ContactService } from '../../core/services/contact.service';

/**
 * Prueba de componente del formulario de contacto: validación reactiva del
 * formulario y envío a través del servicio (caso correcto y caso de error).
 */
describe('ContactComponent', () => {
  let fixture: ComponentFixture<ContactComponent>;
  let component: ContactComponent;
  let sendSpy: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    sendSpy = vi.fn().mockReturnValue(of({ message: 'ok' }));

    await TestBed.configureTestingModule({
      imports: [ContactComponent, TranslateModule.forRoot()],
      providers: [
        provideNoopAnimations(),
        { provide: ContactService, useValue: { send: sendSpy } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ContactComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('arranca con un formulario inválido y vacío', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('marca el email mal formado como inválido', () => {
    component.email.setValue('no-es-un-email');
    expect(component.email.valid).toBe(false);
  });

  it('exige un mensaje de al menos 10 caracteres', () => {
    component.message.setValue('corto');
    expect(component.message.hasError('minlength')).toBe(true);
  });

  it('no envía nada si el formulario es inválido', () => {
    component.submit();
    expect(sendSpy).not.toHaveBeenCalled();
  });

  it('envía el mensaje y marca el envío como completado cuando es válido', () => {
    component.form.setValue({
      name: 'Ana López',
      email: 'ana@example.com',
      subject: 'Consulta',
      message: 'Quería saber los horarios de la línea de Martos.',
    });

    component.submit();

    expect(sendSpy).toHaveBeenCalledTimes(1);
    expect(component.sent()).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('mantiene loading en falso si el envío falla', () => {
    sendSpy.mockReturnValueOnce(throwError(() => new Error('fallo de red')));
    component.form.setValue({
      name: 'Ana',
      email: 'ana@example.com',
      subject: 'Consulta',
      message: 'Este es un mensaje de prueba suficientemente largo.',
    });

    component.submit();

    expect(component.sent()).toBe(false);
    expect(component.loading()).toBe(false);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { FaqComponent } from './faq.component';

/**
 * Prueba de componente de la página de preguntas frecuentes: comprueba que se
 * renderiza un panel desplegable por cada pregunta configurada.
 */
describe('FaqComponent', () => {
  let fixture: ComponentFixture<FaqComponent>;
  let component: FaqComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FaqComponent, TranslateModule.forRoot()],
      providers: [provideRouter([]), provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(FaqComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('se crea correctamente', () => {
    expect(component).toBeTruthy();
  });

  it('define seis preguntas frecuentes', () => {
    expect(component.items).toEqual([1, 2, 3, 4, 5, 6]);
  });

  it('renderiza un panel de expansión por cada pregunta', () => {
    const panels = fixture.nativeElement.querySelectorAll('mat-expansion-panel');
    expect(panels.length).toBe(6);
  });
});

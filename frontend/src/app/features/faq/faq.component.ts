import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatExpansionModule,
    MatIconModule,
    MatButtonModule,
    TranslateModule,
  ],
  templateUrl: './faq.component.html',
  styleUrl: './faq.component.scss',
})
export class FaqComponent {
  /** Índices de las preguntas; el texto se resuelve por i18n (faq.qN / faq.aN). */
  readonly items = [1, 2, 3, 4, 5, 6];
}

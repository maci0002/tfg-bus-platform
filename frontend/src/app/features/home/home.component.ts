import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    TranslateModule,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  features = [
    { icon: 'map',                 key: 'home.features.map' },
    { icon: 'schedule',            key: 'home.features.schedules' },
    { icon: 'confirmation_number', key: 'home.features.booking' },
    { icon: 'notifications',       key: 'home.features.alerts' },
    { icon: 'qr_code',             key: 'home.features.ticket' },
    { icon: 'support_agent',       key: 'home.features.support' },
  ];
}

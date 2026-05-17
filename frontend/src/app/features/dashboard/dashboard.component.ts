import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  constructor(public auth: AuthService) {}

  quickActions = [
    { icon: 'search',                route: '/routes',    key: 'dashboard.actions.search' },
    { icon: 'map',                   route: '/map',       key: 'dashboard.actions.map' },
    { icon: 'confirmation_number',   route: '/my-bookings', key: 'dashboard.actions.bookings' },
    { icon: 'contact_support',       route: '/contact',   key: 'dashboard.actions.contact' },
  ];
}

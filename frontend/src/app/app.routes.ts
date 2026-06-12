import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent),
  },
  {
    path: 'lines',
    loadComponent: () => import('./features/lines/lines.component').then(m => m.LinesComponent),
  },
  {
    path: 'lines/:id',
    loadComponent: () => import('./features/lines/line-detail/line-detail.component').then(m => m.LineDetailComponent),
  },
  {
    path: 'map',
    loadComponent: () => import('./features/map/map.component').then(m => m.MapComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
  },

  // ── Flujo de reserva ──────────────────────────────────────────────────────
  {
    path: 'booking/seats',
    loadComponent: () => import('./features/booking/seat-selection/seat-selection.component').then(m => m.SeatSelectionComponent),
  },
  {
    path: 'booking/confirm',
    loadComponent: () => import('./features/booking/booking-confirm/booking-confirm.component').then(m => m.BookingConfirmComponent),
  },
  {
    path: 'my-bookings',
    loadComponent: () => import('./features/booking/my-bookings/my-bookings.component').then(m => m.MyBookingsComponent),
    canActivate: [authGuard],
  },
  {
    path: 'bookings/:id',
    loadComponent: () => import('./features/booking/booking-detail/booking-detail.component').then(m => m.BookingDetailComponent),
    canActivate: [authGuard],
  },

  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
  },
  {
    path: '**',
    redirectTo: '',
  },
];

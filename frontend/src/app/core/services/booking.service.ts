import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateReservationRequest,
  Reservation,
  SeatMap,
} from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class BookingService {

  private readonly baseUrl = `${environment.apiUrl}/bookings`;
  private http = inject(HttpClient);

  getSeatMap(lineId: number, origin: string, destination: string,
             date: string, time: string): Observable<SeatMap> {
    const params = new HttpParams()
      .set('lineId', lineId)
      .set('origin', origin)
      .set('destination', destination)
      .set('date', date)
      .set('time', time);
    return this.http.get<SeatMap>(`${this.baseUrl}/seats`, { params });
  }

  create(request: CreateReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>(this.baseUrl, request);
  }

  getMy(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.baseUrl}/my`);
  }

  getById(id: number): Observable<Reservation> {
    return this.http.get<Reservation>(`${this.baseUrl}/${id}`);
  }

  cancel(id: number): Observable<Reservation> {
    return this.http.delete<Reservation>(`${this.baseUrl}/${id}`);
  }
}

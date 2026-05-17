import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LineDetail,
  LineSummary,
  Stop,
  TripSearchParams,
  TripSearchResult,
} from '../models/transport.model';

@Injectable({ providedIn: 'root' })
export class TransportService {

  private readonly baseUrl = `${environment.apiUrl}/transport`;
  private http = inject(HttpClient);

  getLines(): Observable<LineSummary[]> {
    return this.http.get<LineSummary[]>(`${this.baseUrl}/lines`);
  }

  getLine(id: number): Observable<LineDetail> {
    return this.http.get<LineDetail>(`${this.baseUrl}/lines/${id}`);
  }

  getStops(): Observable<Stop[]> {
    return this.http.get<Stop[]>(`${this.baseUrl}/stops`);
  }

  searchTrips(params: TripSearchParams): Observable<TripSearchResult[]> {
    let httpParams = new HttpParams()
      .set('origin', params.origin)
      .set('destination', params.destination);

    if (params.date) httpParams = httpParams.set('date', params.date);
    if (params.time) httpParams = httpParams.set('time', params.time);

    return this.http.get<TripSearchResult[]>(`${this.baseUrl}/search`, { params: httpParams });
  }
}

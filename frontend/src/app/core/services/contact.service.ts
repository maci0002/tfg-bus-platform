import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ContactRequest {
  name: string;
  email: string;
  subject: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ContactService {

  private readonly baseUrl = `${environment.apiUrl}/contact`;
  private http = inject(HttpClient);

  send(request: ContactRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(this.baseUrl, request);
  }
}

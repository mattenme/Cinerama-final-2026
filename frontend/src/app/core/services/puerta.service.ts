import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Validacion } from '../interfaces/cinerama.interfaces';
import { API } from './api';

@Injectable({ providedIn: 'root' })
export class PuertaService {
  private http = inject(HttpClient);
  private url = `${API}/puerta`;

  validar(codigo: string): Observable<Validacion> {
    return this.http.post<Validacion>(`${this.url}/validar`, { codigo });
  }

  recientes(): Observable<Validacion[]> {
    return this.http.get<Validacion[]>(`${this.url}/validaciones`);
  }
}

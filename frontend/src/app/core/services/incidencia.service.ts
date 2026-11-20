import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Incidencia, IncidenciaRequest } from '../interfaces/cinerama.interfaces';
import { API } from './api';

@Injectable({ providedIn: 'root' })
export class IncidenciaService {
  private http = inject(HttpClient);
  private url = `${API}/incidencias`;

  listarTodas(): Observable<Incidencia[]> {
    return this.http.get<Incidencia[]>(this.url);
  }

  listarMias(): Observable<Incidencia[]> {
    return this.http.get<Incidencia[]>(`${this.url}/mias`);
  }

  listarPorCliente(idCliente: number): Observable<Incidencia[]> {
    return this.http.get<Incidencia[]>(`${this.url}/cliente/${idCliente}`);
  }

  crear(incidencia: IncidenciaRequest): Observable<Incidencia> {
    return this.http.post<Incidencia>(this.url, incidencia);
  }

  cambiarEstado(id: number, estado: string): Observable<Incidencia> {
    return this.http.patch<Incidencia>(`${this.url}/${id}/estado/${encodeURIComponent(estado)}`, {});
  }
}

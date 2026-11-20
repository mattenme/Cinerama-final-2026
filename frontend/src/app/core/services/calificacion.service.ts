import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Calificacion,
  CalificacionPromedio,
  CalificacionRequest,
  PuedeCalificar
} from '../interfaces/cinerama.interfaces';
import { API } from './api';

@Injectable({ providedIn: 'root' })
export class CalificacionService {
  private http = inject(HttpClient);
  private url = `${API}/calificaciones`;

  promedio(idPelicula: number): Observable<CalificacionPromedio> {
    return this.http.get<CalificacionPromedio>(`${this.url}/pelicula/${idPelicula}/promedio`);
  }

  listarPorPelicula(idPelicula: number): Observable<Calificacion[]> {
    return this.http.get<Calificacion[]>(`${this.url}/pelicula/${idPelicula}`);
  }

  puedeCalificar(idPelicula: number): Observable<PuedeCalificar> {
    return this.http.get<PuedeCalificar>(`${this.url}/pelicula/${idPelicula}/puede-calificar`);
  }

  mia(idPelicula: number): Observable<Calificacion> {
    return this.http.get<Calificacion>(`${this.url}/pelicula/${idPelicula}/mia`);
  }

  registrar(calificacion: CalificacionRequest): Observable<Calificacion> {
    return this.http.post<Calificacion>(this.url, calificacion);
  }
}

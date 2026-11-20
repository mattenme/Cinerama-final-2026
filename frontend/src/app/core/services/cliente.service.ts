import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Cliente } from '../interfaces/cinerama.interfaces';
import { API } from './api';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private http = inject(HttpClient);
  private url = `${API}/clientes`;

  listar(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.url);
  }

  obtener(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.url}/${id}`);
  }

  toggleActivo(id: number): Observable<Cliente> {
    return this.http.patch<Cliente>(`${this.url}/${id}/activo`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

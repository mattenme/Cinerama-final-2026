import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CorreoEnviado } from '../interfaces/cinerama.interfaces';
import { API } from './api';

export interface PruebaCorreo {
  enviado: boolean;
  destino: string;
  servidor?: string | null;
  remitente?: string | null;
  mensaje: string;
  fecha?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CorreoAdminService {
  private http = inject(HttpClient);
  private url = `${API}/admin/correo`;

  bandeja(): Observable<CorreoEnviado[]> {
    return this.http.get<CorreoEnviado[]>(`${this.url}/bandeja`);
  }

  limpiar(): Observable<{ eliminados: number }> {
    return this.http.delete<{ eliminados: number }>(`${this.url}/bandeja`);
  }

  probar(destino: string): Observable<PruebaCorreo> {
    return this.http.post<PruebaCorreo>(`${this.url}/prueba`, { destino });
  }
}

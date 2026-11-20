import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Compra, CompraRequest } from '../interfaces/cinerama.interfaces';
import { API } from './api';

@Injectable({ providedIn: 'root' })
export class CompraService {
  private http = inject(HttpClient);
  private url = `${API}/compras`;

  procesar(request: CompraRequest): Observable<Compra[]> {
    return this.http.post<Compra[]>(`${this.url}/procesar`, request);
  }

  listarMias(): Observable<Compra[]> {
    return this.http.get<Compra[]>(`${this.url}/mias`);
  }

  listarPorCliente(idCliente: number): Observable<Compra[]> {
    return this.http.get<Compra[]>(`${this.url}/cliente/${idCliente}`);
  }

  listarConfirmadasPorFuncion(idFuncion: number): Observable<Compra[]> {
    return this.http.get<Compra[]>(`${this.url}/funcion/${idFuncion}/confirmadas`);
  }

  obtener(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.url}/${id}`);
  }

  cancelar(id: number): Observable<Compra> {
    return this.http.post<Compra>(`${this.url}/${id}/cancelar`, {});
  }

  asientosOcupados(idFuncion: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.url}/funcion/${idFuncion}/asientos-ocupados`);
  }

  obtenerQr(idCompra: number): Observable<Blob> {
    return this.http.get(`${this.url}/${idCompra}/qr.png`, { responseType: 'blob' });
  }
}

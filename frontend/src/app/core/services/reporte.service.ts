import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API } from './api';

export interface ResumenFuncion {
  idFuncion: number;
  butacas: number;
  ingresosEntradas: number;
  ingresosDulceria: number;
  total: number;
}

export interface VentaPelicula {
  idPelicula: number;
  titulo: string;
  compras: number;
  butacas: number;
  entradas: number;
  dulceria: number;
  total: number;
}

export interface OcupacionFuncion {
  idFuncion: number;
  titulo: string;
  sala: string;
  horaInicio?: string | null;
  capacidad: number;
  compras: number;
  vendidas: number;
  ocupacionPct: number;
}

export interface TopCliente {
  posicion: number;
  dni: string;
  nombre: string;
  compras: number;
  butacas: number;
  entradas: number;
  dulceria: number;
  total: number;
  ticketPromedio: number;
  diferenciaPromedio: number;
  vsPromedioPct: number;
  ultimaCompra?: string | null;
}

export interface ComparacionPeriodo {
  metrica: string;
  actual: number;
  anterior: number;
  variacion: number;
  variacionPct: number | null;
}

export interface ClienteVsPromedio {
  idCliente: number;
  dni: string;
  nombre: string;
  compras: number;
  totalCliente: number;
  promedio: number;
  diferencia: number;
  porcentaje: number;
  posicion: number | null;
  clientes: number;
}

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private http = inject(HttpClient);
  private url = `${API}/reportes`;

  resumenFuncion(idFuncion: number): Observable<ResumenFuncion> {
    return this.http.get<ResumenFuncion>(`${this.url}/funcion/${idFuncion}/resumen`);
  }

  ventasPorPelicula(): Observable<VentaPelicula[]> {
    return this.http.get<VentaPelicula[]>(`${this.url}/ventas-peliculas`);
  }

  ocupacion(): Observable<OcupacionFuncion[]> {
    return this.http.get<OcupacionFuncion[]>(`${this.url}/ocupacion`);
  }

  topClientes(limite = 5): Observable<TopCliente[]> {
    return this.http.get<TopCliente[]>(`${this.url}/top-clientes`, { params: { limite } });
  }

  comparacion(dias = 30): Observable<ComparacionPeriodo[]> {
    return this.http.get<ComparacionPeriodo[]>(`${this.url}/comparacion`, { params: { dias } });
  }

  clienteVsPromedio(idCliente: number): Observable<ClienteVsPromedio> {
    return this.http.get<ClienteVsPromedio>(`${this.url}/cliente/${idCliente}/comparacion`);
  }
}

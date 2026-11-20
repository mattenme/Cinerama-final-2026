import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { formatoFechaHora, formatoSoles } from '../../../core/services/api';
import {
  ComparacionPeriodo,
  OcupacionFuncion,
  ReporteService,
  ResumenFuncion,
  TopCliente,
  VentaPelicula
} from '../../../core/services/reporte.service';

@Component({
  selector: 'app-admin-reportes',
  standalone: true,
  imports: [RouterLink, AdminNavComponent],
  templateUrl: './admin-reportes.component.html',
  styleUrl: './admin-reportes.component.css'
})
export class AdminReportesComponent implements OnInit {
  private service = inject(ReporteService);

  readonly ventas = signal<VentaPelicula[]>([]);
  readonly ocupacion = signal<OcupacionFuncion[]>([]);
  readonly topClientes = signal<TopCliente[]>([]);
  readonly comparacion = signal<ComparacionPeriodo[]>([]);
  readonly resumen = signal<ResumenFuncion | null>(null);
  readonly resumenTitulo = signal('');
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly maxVenta = computed(() =>
    Math.max(1, ...this.ventas().map((v) => Number(v.total ?? 0)))
  );

  readonly maxOcupacion = computed(() =>
    Math.max(1, ...this.ocupacion().map((o) => this.porcentaje(o)))
  );

  readonly limite = signal(5);
  readonly dias = signal(30);

  private pendientes = 0;

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set(null);
    this.pendientes = 4;

    this.service.ventasPorPelicula().subscribe({
      next: (ventas) => {
        this.ventas.set(ventas);
        this.listo();
      },
      error: () => {
        this.error.set('No se pudo cargar el reporte de ventas por película.');
        this.listo();
      }
    });

    this.service.ocupacion().subscribe({
      next: (ocupacion) => {
        this.ocupacion.set(ocupacion);
        this.listo();
      },
      error: () => {
        this.error.set('No se pudo cargar la ocupación por función.');
        this.listo();
      }
    });

    this.service.topClientes(this.limite()).subscribe({
      next: (clientes) => {
        this.topClientes.set(clientes);
        this.listo();
      },
      error: () => {
        this.error.set('No se pudo cargar el ranking de clientes.');
        this.listo();
      }
    });

    this.service.comparacion(this.dias()).subscribe({
      next: (filas) => {
        this.comparacion.set(filas);
        this.listo();
      },
      error: () => {
        this.error.set('No se pudo cargar la comparación del periodo.');
        this.listo();
      }
    });
  }

  cambiarLimite(valor: number) {
    if (this.limite() === valor) return;
    this.limite.set(valor);
    this.cargarTopClientes();
  }

  cambiarDias(valor: number) {
    if (this.dias() === valor) return;
    this.dias.set(valor);
    this.cargarComparacion();
  }

  private cargarTopClientes() {
    this.service.topClientes(this.limite()).subscribe({
      next: (clientes) => this.topClientes.set(clientes),
      error: () => this.error.set('No se pudo cargar el ranking de clientes.')
    });
  }

  private cargarComparacion() {
    this.service.comparacion(this.dias()).subscribe({
      next: (filas) => this.comparacion.set(filas),
      error: () => this.error.set('No se pudo cargar la comparación del periodo.')
    });
  }

  private listo() {
    this.pendientes = Math.max(0, this.pendientes - 1);
    if (this.pendientes === 0) {
      this.cargando.set(false);
    }
  }

  verResumen(funcion: OcupacionFuncion) {
    this.resumenTitulo.set(`${funcion.titulo} · ${funcion.sala} · ${this.fecha(funcion.horaInicio)}`);
    this.service.resumenFuncion(funcion.idFuncion).subscribe({
      next: (resumen) => this.resumen.set(resumen),
      error: () => this.error.set('No se pudo generar el resumen de la función.')
    });
  }

  porcentaje(funcion: OcupacionFuncion): number {
    if (funcion.ocupacionPct != null) {
      return Math.min(100, Math.round(Number(funcion.ocupacionPct)));
    }
    if (!funcion.capacidad) return 0;
    return Math.min(100, Math.round((funcion.vendidas * 100) / funcion.capacidad));
  }

  nombreMetrica(metrica: string): string {
    const nombres: Record<string, string> = {
      COMPRAS: 'Compras',
      BUTACAS: 'Butacas vendidas',
      ENTRADAS: 'Ingresos por entradas',
      DULCERIA: 'Ingresos por dulcería',
      TOTAL: 'Ingreso total'
    };
    return nombres[metrica] ?? metrica;
  }

  esMonetaria(metrica: string): boolean {
    return metrica === 'ENTRADAS' || metrica === 'DULCERIA' || metrica === 'TOTAL';
  }

  valor(metrica: string, numero?: number | null): string {
    return this.esMonetaria(metrica) ? this.soles(numero) : String(Number(numero ?? 0));
  }

  variacion(fila: ComparacionPeriodo): string {
    if (fila.variacionPct == null) return 'sin periodo anterior';
    const signo = fila.variacionPct > 0 ? '+' : '';
    return `${signo}${fila.variacionPct}%`;
  }

  claseVariacion(fila: ComparacionPeriodo): string {
    if (fila.variacionPct == null || fila.variacionPct === 0) return 'neutro';
    return fila.variacionPct > 0 ? 'sube' : 'baja';
  }

  anchoVenta(v: VentaPelicula): number {
    return Math.max(2, Math.round((Number(v.total ?? 0) / this.maxVenta()) * 100));
  }

  anchoOcupacion(o: OcupacionFuncion): number {
    return Math.max(2, Math.round((this.porcentaje(o) / this.maxOcupacion()) * 100));
  }

  descargarVentas() {
    this.descargarCsv('cinerama-ventas-por-pelicula.csv', [
      ['Pelicula', 'Compras', 'Butacas', 'Entradas', 'Dulceria', 'Total'],
      ...this.ventas().map((v) => [
        v.titulo, v.compras, v.butacas, v.entradas, v.dulceria, v.total
      ])
    ]);
  }

  descargarOcupacion() {
    this.descargarCsv('cinerama-ocupacion-por-funcion.csv', [
      ['Funcion', 'Sala', 'Hora', 'Capacidad', 'Compras', 'Vendidas', 'Ocupacion %'],
      ...this.ocupacion().map((o) => [
        o.titulo, o.sala, this.fecha(o.horaInicio), o.capacidad, o.compras, o.vendidas, this.porcentaje(o)
      ])
    ]);
  }

  private descargarCsv(nombre: string, filas: (string | number | null | undefined)[][]) {
    const contenido = filas
      .map((fila) => fila.map((celda) => `"${String(celda ?? '').replace(/"/g, '""')}"`).join(','))
      .join('\r\n');

    const blob = new Blob(['\ufeff' + contenido], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(url);
  }

  soles(valor?: number | null): string {
    return formatoSoles(Number(valor ?? 0));
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }
}

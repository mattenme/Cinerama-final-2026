import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { Incidencia } from '../../../core/interfaces/cinerama.interfaces';
import { formatoFechaHora } from '../../../core/services/api';
import { AlertService } from '../../../core/services/alert.service';
import { IncidenciaService } from '../../../core/services/incidencia.service';

@Component({
  selector: 'app-admin-incidencias',
  standalone: true,
  imports: [RouterLink, AdminNavComponent],
  templateUrl: './admin-incidencias.component.html',
  styleUrl: './admin-incidencias.component.css'
})
export class AdminIncidenciasComponent implements OnInit {
  private service = inject(IncidenciaService);
  private alertas = inject(AlertService);

  readonly incidencias = signal<Incidencia[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly filtro = signal('TODAS');
  readonly busqueda = signal('');

  readonly filtros = [
    { valor: 'TODAS', texto: 'Todas' },
    { valor: 'PENDIENTE', texto: 'Pendientes' },
    { valor: 'EN_REVISION', texto: 'En revisión' },
    { valor: 'RESUELTA', texto: 'Resueltas' }
  ];

  readonly visibles = computed(() => {
    const filtro = this.filtro();
    const texto = this.busqueda().toLowerCase().trim();

    return this.incidencias().filter((i) => {
      const coincideEstado = filtro === 'TODAS' || (i.estado ?? '') === filtro;
      if (!coincideEstado) return false;
      if (!texto) return true;
      return [i.tipo, i.descripcion, i.nombreCliente, i.nombreSala, i.tituloPelicula]
        .some((valor) => (valor ?? '').toLowerCase().includes(texto));
    });
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.service.listarTodas().subscribe({
      next: (incidencias) => {
        this.incidencias.set(incidencias);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las incidencias. Verifica el backend.');
        this.cargando.set(false);
      }
    });
  }

  cambiarEstado(incidencia: Incidencia, estado: string) {
    if (!incidencia.idIncidencia) return;

    this.service.cambiarEstado(incidencia.idIncidencia, estado).subscribe({
      next: () => {
        this.alertas.exito('Incidencia actualizada', `Estado: ${this.textoEstado(estado)}`);
        this.cargar();
      },
      error: (err) =>
        this.alertas.error('No se pudo actualizar', err?.error?.message ?? 'Inténtalo de nuevo.')
    });
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  textoEstado(estado?: string | null): string {
    switch ((estado ?? '').toUpperCase()) {
      case 'PENDIENTE':
        return 'Pendiente';
      case 'EN_REVISION':
        return 'En revisión';
      case 'RESUELTA':
        return 'Resuelta';
      default:
        return estado || '—';
    }
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }
}

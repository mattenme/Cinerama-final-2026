import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Incidencia } from '../../core/interfaces/cinerama.interfaces';
import { formatoFechaHora } from '../../core/services/api';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { IncidenciaService } from '../../core/services/incidencia.service';

@Component({
  selector: 'app-soporte',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './soporte.component.html',
  styleUrl: './soporte.component.css'
})
export class SoporteComponent implements OnInit {
  private service = inject(IncidenciaService);
  private alertas = inject(AlertService);
  readonly auth = inject(AuthService);

  readonly tipos = ['Problema con mi compra', 'Sala o butaca', 'Dulcería', 'Otro'];
  readonly incidencias = signal<Incidencia[]>([]);
  readonly cargando = signal(true);
  readonly enviando = signal(false);

  tipo = this.tipos[0];
  descripcion = '';

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    if (!this.auth.estaLogueado()) {
      this.cargando.set(false);
      return;
    }

    this.cargando.set(true);
    this.service.listarMias().subscribe({
      next: (incidencias) => {
        this.incidencias.set(incidencias);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }

  enviar() {
    if (!this.auth.estaLogueado()) return;

    if (!this.descripcion.trim()) {
      this.alertas.error('Falta la descripción', 'Cuéntanos qué problema tuviste.');
      return;
    }

    this.enviando.set(true);
    this.service
      .crear({
        tipo: this.tipo,
        descripcion: this.descripcion.trim()
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.descripcion = '';
          this.alertas.exito('Incidencia enviada', 'El equipo la revisará pronto.');
          this.cargar();
        },
        error: (err) => {
          this.enviando.set(false);
          this.alertas.error('No se pudo enviar', err?.error?.message ?? 'Inténtalo de nuevo.');
        }
      });
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

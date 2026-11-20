import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';
import { forkJoin } from 'rxjs';

import { Funcion, Pelicula, Sala } from '../../../core/interfaces/cinerama.interfaces';
import { AIsoDatetimeLocal, formatoFechaHora } from '../../../core/services/api';
import { AlertService } from '../../../core/services/alert.service';
import { FuncionService, PeliculaService, SalaService } from '../../../core/services/catalogo.service';

@Component({
  selector: 'app-admin-funciones',
  standalone: true,
  imports: [FormsModule, RouterLink, AdminNavComponent],
  templateUrl: './admin-funciones.component.html',
  styleUrl: './admin-funciones.component.css'
})
export class AdminFuncionesComponent implements OnInit {
  private service = inject(FuncionService);
  private peliculaService = inject(PeliculaService);
  private salaService = inject(SalaService);
  private alertas = inject(AlertService);

  readonly funciones = signal<Funcion[]>([]);
  readonly peliculas = signal<Pelicula[]>([]);
  readonly salas = signal<Sala[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);
  readonly mostrarForm = signal(false);
  readonly editando = signal<Funcion | null>(null);
  readonly busqueda = signal('');

  readonly visibles = computed(() => {
    const texto = this.busqueda().toLowerCase().trim();
    if (!texto) return this.funciones();
    return this.funciones().filter((f) =>
      [f.tituloPelicula, f.nombreSala, f.estado].some((valor) => (valor ?? '').toLowerCase().includes(texto))
    );
  });

  modelo = this.vacia();
  horaLocal = '';
  readonly estados = ['Programada', 'En curso', 'Finalizada', 'Cancelada'];

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    forkJoin({
      funciones: this.service.listarTodas(),
      peliculas: this.peliculaService.listarTodas(),
      salas: this.salaService.listarTodas()
    }).subscribe({
      next: ({ funciones, peliculas, salas }) => {
        this.funciones.set(
          [...funciones].sort(
            (a, b) => new Date(a.horaInicio).getTime() - new Date(b.horaInicio).getTime()
          )
        );
        this.peliculas.set(peliculas);
        this.salas.set(salas);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la programación. Verifica el backend.');
        this.cargando.set(false);
      }
    });
  }

  nueva() {
    this.modelo = this.vacia();
    this.horaLocal = '';
    this.editando.set(null);
    this.mostrarForm.set(true);
  }

  editar(f: Funcion) {
    this.modelo = { ...f, estado: this.normalizarEstado(f.estado) };
    this.horaLocal = AIsoDatetimeLocal(f.horaInicio);
    this.editando.set(f);
    this.mostrarForm.set(true);
  }

  cerrar() {
    this.mostrarForm.set(false);
  }

  guardar() {
    if (!this.modelo.idPelicula || !this.modelo.idSala || !this.horaLocal) {
      this.alertas.error('Datos incompletos', 'Elige película, sala y fecha/hora.');
      return;
    }
    if (this.modelo.precio == null || Number(this.modelo.precio) < 0) {
      this.alertas.error('Precio inválido', 'Indica el precio de la butaca (0 a más).');
      return;
    }

    const cuerpo: Funcion = {
      idPelicula: this.modelo.idPelicula,
      idSala: this.modelo.idSala,
      horaInicio: this.horaLocal,
      estado: this.modelo.estado,
      activo: this.modelo.activo,
      precio: this.modelo.precio
    };
    this.guardando.set(true);
    const editando = this.editando();

    const peticion = editando
      ? this.service.actualizar(editando.idFuncion!, cuerpo)
      : this.service.crear(cuerpo);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.mostrarForm.set(false);
        this.alertas.exito(editando ? 'Función actualizada' : 'Función programada');
        this.cargar();
      },
      error: (err) => {
        this.guardando.set(false);
        this.alertas.error('No se pudo guardar', err?.error?.message ?? 'Revisa los datos.');
      }
    });
  }

  toggleActivo(f: Funcion) {
    this.service.toggleActivo(f.idFuncion!).subscribe({
      next: () => this.cargar(),
      error: () => this.alertas.error('No se pudo cambiar el estado')
    });
  }

  async eliminar(f: Funcion) {
    const ok = await this.alertas.confirmar(
      '¿Eliminar función?',
      `${f.tituloPelicula} · ${this.fecha(f.horaInicio)}`,
      'Sí, eliminar'
    );
    if (!ok) return;

    this.service.eliminar(f.idFuncion!).subscribe({
      next: () => {
        this.alertas.exito('Función eliminada');
        this.cargar();
      },
      error: () => this.alertas.error('No se pudo eliminar', 'Puede que tenga compras asociadas.')
    });
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }

  pasada(iso?: string | null): boolean {
    return iso ? new Date(iso).getTime() < Date.now() : false;
  }

  private vacia(): Funcion {
    return {
      idPelicula: 0,
      idSala: 0,
      horaInicio: '',
      estado: 'Programada',
      activo: 1,
      precio: 20
    };
  }

  private normalizarEstado(estado?: string | null): string {
    const valor = (estado ?? '').trim().toLowerCase();
    return this.estados.find((e) => e.toLowerCase() === valor) ?? this.estados[0];
  }
}

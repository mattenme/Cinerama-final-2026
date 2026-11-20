import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { Pelicula } from '../../../core/interfaces/cinerama.interfaces';
import { formatoSoles, urlImagen } from '../../../core/services/api';
import { AlertService } from '../../../core/services/alert.service';
import { PeliculaService } from '../../../core/services/catalogo.service';

@Component({
  selector: 'app-admin-peliculas',
  standalone: true,
  imports: [FormsModule, RouterLink, AdminNavComponent],
  templateUrl: './admin-peliculas.component.html',
  styleUrl: './admin-peliculas.component.css'
})
export class AdminPeliculasComponent implements OnInit {
  private service = inject(PeliculaService);
  private alertas = inject(AlertService);

  readonly peliculas = signal<Pelicula[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);
  readonly mostrarForm = signal(false);
  readonly editando = signal<Pelicula | null>(null);
  readonly busqueda = signal('');

  readonly visibles = computed(() => {
    const texto = this.busqueda().toLowerCase().trim();
    if (!texto) return this.peliculas();
    return this.peliculas().filter((p) =>
      [p.titulo, p.genero, p.sinopsis].some((valor) => (valor ?? '').toLowerCase().includes(texto))
    );
  });

  readonly GENEROS = [
    'Acción',
    'Animación',
    'Aventura',
    'Ciencia ficción',
    'Comedia',
    'Documental',
    'Drama',
    'Familiar',
    'Fantasía',
    'Musical',
    'Romance',
    'Suspenso',
    'Terror'
  ];

  modelo: Pelicula = this.vacia();
  archivo: File | null = null;
  nombreArchivo = '';
  readonly vistaPrevia = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.service.listarTodas().subscribe({
      next: (peliculas) => {
        this.peliculas.set(peliculas);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista. Verifica que el backend esté encendido.');
        this.cargando.set(false);
      }
    });
  }

  nueva() {
    this.modelo = this.vacia();
    this.editando.set(null);
    this.limpiarArchivo();
    this.mostrarForm.set(true);
  }

  editar(p: Pelicula) {
    this.modelo = { ...p };
    this.editando.set(p);
    this.limpiarArchivo();
    this.mostrarForm.set(true);
  }

  cerrar() {
    this.limpiarArchivo();
    this.mostrarForm.set(false);
  }

  elegirArchivo(evento: Event) {
    const input = evento.target as HTMLInputElement;
    this.limpiarArchivo();
    this.archivo = input.files?.[0] ?? null;
    this.nombreArchivo = this.archivo?.name ?? '';
    this.vistaPrevia.set(this.archivo ? URL.createObjectURL(this.archivo) : null);
  }

  private limpiarArchivo() {
    const previa = this.vistaPrevia();
    if (previa) URL.revokeObjectURL(previa);
    this.vistaPrevia.set(null);
    this.archivo = null;
    this.nombreArchivo = '';
  }

  guardar() {
    if (!this.modelo.titulo?.trim() || !this.modelo.duracionMinutos) {
      this.alertas.error('Datos incompletos', 'El título y la duración son obligatorios.');
      return;
    }

    this.guardando.set(true);
    const editando = this.editando();

    const cuerpo: Pelicula = {
      titulo: this.modelo.titulo,
      duracionMinutos: this.modelo.duracionMinutos,
      genero: this.modelo.genero ?? null,
      sinopsis: this.modelo.sinopsis ?? null,
      destacado: this.modelo.destacado,
      activo: this.modelo.activo
    };

    const peticion = editando
      ? this.service.actualizar(editando.idPelicula!, cuerpo)
      : this.service.crear(cuerpo);

    peticion.subscribe({
      next: (guardada) => {
        if (this.archivo && guardada.idPelicula) {
          this.service.subirImagen(guardada.idPelicula, this.archivo).subscribe({
            next: () => {
              this.guardando.set(false);
              this.mostrarForm.set(false);
              this.alertas.exito(editando ? 'Película actualizada' : 'Película creada');
              this.cargar();
            },
            error: () => {
              this.guardando.set(false);
              this.mostrarForm.set(false);
              this.alertas.error('Se guardó la película, pero la imagen no se pudo subir');
              this.cargar();
            }
          });
          return;
        }
        this.guardando.set(false);
        this.mostrarForm.set(false);
        this.alertas.exito(editando ? 'Película actualizada' : 'Película creada');
        this.cargar();
      },
      error: (err) => {
        this.guardando.set(false);
        this.alertas.error('No se pudo guardar', err?.error?.message ?? 'Revisa los datos.');
      }
    });
  }

  toggleActivo(p: Pelicula) {
    this.service.toggleActivo(p.idPelicula!).subscribe({
      next: () => this.cargar(),
      error: () => this.alertas.error('No se pudo cambiar el estado')
    });
  }

  toggleDestacado(p: Pelicula) {
    this.service.toggleDestacado(p.idPelicula!).subscribe({
      next: () => this.cargar(),
      error: () => this.alertas.error('No se pudo cambiar el destacado')
    });
  }

  async eliminar(p: Pelicula) {
    const ok = await this.alertas.confirmar('¿Eliminar película?', `Se eliminará "${p.titulo}".`, 'Sí, eliminar');
    if (!ok) return;

    this.service.eliminar(p.idPelicula!).subscribe({
      next: () => {
        this.alertas.exito('Película eliminada');
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        const detalle = String(err.error?.message ?? '');
        const mensaje = err.status === 0
          ? 'No se pudo conectar con el backend.'
          : detalle.includes('ORA-02292')
            ? 'No se puede eliminar porque hay registros relacionados con esta película. Puedes desactivarla para quitarla de la cartelera.'
            : detalle || 'El servidor rechazó la eliminación.';
        this.alertas.error('No se pudo eliminar', mensaje);
      }
    });
  }

  imagen(p: Pelicula): string | null {
    return urlImagen(p.imagenUrl);
  }

  ocultar(evento: Event) {
    (evento.target as HTMLImageElement).style.display = 'none';
  }

  soles(valor: number): string {
    return formatoSoles(valor);
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  private vacia(): Pelicula {
    return {
      titulo: '',
      duracionMinutos: 90,
      genero: '',
      sinopsis: '',
      imagenUrl: null,
      destacado: 0,
      activo: 1
    };
  }
}

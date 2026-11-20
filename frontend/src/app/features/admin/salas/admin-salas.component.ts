import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { Sala } from '../../../core/interfaces/cinerama.interfaces';
import { AlertService } from '../../../core/services/alert.service';
import { AsientoService, SalaService } from '../../../core/services/catalogo.service';

@Component({
  selector: 'app-admin-salas',
  standalone: true,
  imports: [FormsModule, RouterLink, AdminNavComponent],
  templateUrl: './admin-salas.component.html',
  styleUrl: './admin-salas.component.css'
})
export class AdminSalasComponent implements OnInit {
  private service = inject(SalaService);
  private asientoService = inject(AsientoService);
  private alertas = inject(AlertService);

  readonly salas = signal<Sala[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);
  readonly mostrarForm = signal(false);
  readonly editando = signal<Sala | null>(null);
  readonly busqueda = signal('');

  readonly visibles = computed(() => {
    const texto = this.busqueda().toLowerCase().trim();
    if (!texto) return this.salas();
    return this.salas().filter((s) =>
      [s.nombre, s.tipo].some((valor) => (valor ?? '').toLowerCase().includes(texto))
    );
  });

  readonly salaButacas = signal<Sala | null>(null);
  readonly generando = signal(false);
  butacasFilas = 6;
  butacasPorFila = 10;

  modelo: Sala = this.vacia();

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.service.listarTodas().subscribe({
      next: (salas) => {
        this.salas.set(salas);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las salas. Verifica el backend.');
        this.cargando.set(false);
      }
    });
  }

  nueva() {
    this.modelo = this.vacia();
    this.editando.set(null);
    this.mostrarForm.set(true);
  }

  editar(sala: Sala) {
    this.modelo = { ...sala };
    this.editando.set(sala);
    this.mostrarForm.set(true);
  }

  cerrar() {
    this.mostrarForm.set(false);
  }

  guardar() {
    if (!this.modelo.nombre?.trim()) {
      this.alertas.error('Datos incompletos', 'El nombre de la sala es obligatorio.');
      return;
    }

    this.guardando.set(true);
    const editando = this.editando();
    const cuerpo: Sala = {
      nombre: this.modelo.nombre.trim(),
      tipo: this.modelo.tipo ?? null,
      capacidadTotal: this.modelo.capacidadTotal,
      activo: this.modelo.activo
    };

    const peticion = editando
      ? this.service.actualizar(editando.idSala!, cuerpo)
      : this.service.crear(cuerpo);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.mostrarForm.set(false);
        this.alertas.exito(editando ? 'Sala actualizada' : 'Sala creada');
        this.cargar();
      },
      error: (err) => {
        this.guardando.set(false);
        this.alertas.error('No se pudo guardar', err?.error?.message ?? 'Revisa los datos.');
      }
    });
  }

  toggleActivo(sala: Sala) {
    this.service.toggleActivo(sala.idSala!).subscribe({
      next: () => this.cargar(),
      error: () => this.alertas.error('No se pudo cambiar el estado')
    });
  }

  async eliminar(sala: Sala) {
    const ok = await this.alertas.confirmar(
      '¿Eliminar sala?',
      `Se eliminará "${sala.nombre}". Si tiene butacas, primero desactívala.`,
      'Sí, eliminar'
    );
    if (!ok) return;

    this.service.eliminar(sala.idSala!).subscribe({
      next: () => {
        this.alertas.exito('Sala eliminada');
        this.cargar();
      },
      error: (err) =>
        this.alertas.error('No se pudo eliminar', err?.error?.message ?? 'Inténtalo de nuevo.')
    });
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  abrirButacas(sala: Sala) {
    this.butacasFilas = 6;
    this.butacasPorFila = 10;
    this.salaButacas.set(sala);
  }

  cerrarButacas() {
    this.salaButacas.set(null);
  }

  generarButacas() {
    const sala = this.salaButacas();
    if (!sala?.idSala) return;

    if (this.butacasFilas < 1 || this.butacasPorFila < 1) {
      this.alertas.error('Datos incompletos', 'Indica filas y butacas por fila.');
      return;
    }

    this.generando.set(true);
    this.asientoService
      .generarButacas(sala.idSala, this.butacasFilas, this.butacasPorFila)
      .subscribe({
        next: (butacas) => {
          this.generando.set(false);
          this.salaButacas.set(null);
          this.alertas.exito('Butacas generadas', `Se crearon ${butacas.length} butacas.`);
          this.cargar();
        },
        error: (err) => {
          this.generando.set(false);
          this.alertas.error('No se pudieron generar', err?.error?.message ?? 'Revisa los datos.');
        }
      });
  }

  private vacia(): Sala {
    return { nombre: '', tipo: '2D', capacidadTotal: 0, activo: 1 };
  }
}

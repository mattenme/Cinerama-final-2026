import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { Promocion } from '../../../core/interfaces/cinerama.interfaces';
import { AlertService } from '../../../core/services/alert.service';
import { PromocionService } from '../../../core/services/catalogo.service';

@Component({
  selector: 'app-admin-promociones',
  standalone: true,
  imports: [FormsModule, RouterLink, AdminNavComponent],
  templateUrl: './admin-promociones.component.html',
  styleUrl: './admin-promociones.component.css'
})
export class AdminPromocionesComponent implements OnInit {
  private service = inject(PromocionService);
  private alertas = inject(AlertService);

  readonly promociones = signal<Promocion[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);
  readonly mostrarForm = signal(false);
  readonly editando = signal<Promocion | null>(null);
  readonly busqueda = signal('');

  readonly visibles = computed(() => {
    const texto = this.busqueda().toLowerCase().trim();
    if (!texto) return this.promociones();
    return this.promociones().filter((p) =>
      [p.codigo, p.descripcion].some((valor) => (valor ?? '').toLowerCase().includes(texto))
    );
  });

  modelo = this.vacia();

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.service.listarTodas().subscribe({
      next: (promociones) => {
        this.promociones.set(promociones);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las promociones. Verifica el backend.');
        this.cargando.set(false);
      }
    });
  }

  nueva() {
    this.modelo = this.vacia();
    this.editando.set(null);
    this.mostrarForm.set(true);
  }

  editar(p: Promocion) {
    this.modelo = { ...p };
    this.editando.set(p);
    this.mostrarForm.set(true);
  }

  cerrar() {
    this.mostrarForm.set(false);
  }

  guardar() {
    if (!this.modelo.codigo?.trim() || !this.modelo.descuento) {
      this.alertas.error('Datos incompletos', 'El código y el descuento son obligatorios.');
      return;
    }

    const cuerpo: Promocion = {
      codigo: this.modelo.codigo.trim().toUpperCase(),
      descripcion: this.modelo.descripcion ?? null,
      descuento: this.modelo.descuento,
      activo: this.modelo.activo
    };
    this.guardando.set(true);
    const editando = this.editando();

    const peticion = editando
      ? this.service.actualizar(editando.idPromocion!, cuerpo)
      : this.service.crear(cuerpo);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.mostrarForm.set(false);
        this.alertas.exito(editando ? 'Promoción actualizada' : 'Promoción creada');
        this.cargar();
      },
      error: (err) => {
        this.guardando.set(false);
        this.alertas.error('No se pudo guardar', err?.error?.message ?? '¿El código ya existe?');
      }
    });
  }

  toggleActivo(p: Promocion) {
    this.service.toggleActivo(p.idPromocion!).subscribe({
      next: () => this.cargar(),
      error: () => this.alertas.error('No se pudo cambiar el estado')
    });
  }

  async eliminar(p: Promocion) {
    const ok = await this.alertas.confirmar('¿Eliminar promoción?', `Se eliminará "${p.codigo}".`, 'Sí, eliminar');
    if (!ok) return;

    this.service.eliminar(p.idPromocion!).subscribe({
      next: () => {
        this.alertas.exito('Promoción eliminada');
        this.cargar();
      },
      error: () => this.alertas.error('No se pudo eliminar')
    });
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  private vacia(): Promocion {
    return { codigo: '', descripcion: '', descuento: 10, activo: 1 };
  }
}

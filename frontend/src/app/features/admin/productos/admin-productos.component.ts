import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { Producto } from '../../../core/interfaces/cinerama.interfaces';
import { formatoSoles, urlImagen } from '../../../core/services/api';
import { AlertService } from '../../../core/services/alert.service';
import { ProductoService } from '../../../core/services/catalogo.service';

@Component({
  selector: 'app-admin-productos',
  standalone: true,
  imports: [FormsModule, RouterLink, AdminNavComponent],
  templateUrl: './admin-productos.component.html',
  styleUrl: './admin-productos.component.css'
})
export class AdminProductosComponent implements OnInit {
  private service = inject(ProductoService);
  private alertas = inject(AlertService);

  readonly productos = signal<Producto[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly guardando = signal(false);
  readonly mostrarForm = signal(false);
  readonly editando = signal<Producto | null>(null);
  readonly busqueda = signal('');

  readonly visibles = computed(() => {
    const texto = this.busqueda().toLowerCase().trim();
    if (!texto) return this.productos();
    return this.productos().filter((p) =>
      [p.nombre, p.categoria, p.descripcion].some((valor) => (valor ?? '').toLowerCase().includes(texto))
    );
  });

  modelo = this.vacio();
  archivo: File | null = null;
  nombreArchivo = '';
  readonly vistaPrevia = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.service.listarTodos().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la dulcería. Verifica el backend.');
        this.cargando.set(false);
      }
    });
  }

  nuevo() {
    this.modelo = this.vacio();
    this.editando.set(null);
    this.limpiarArchivo();
    this.mostrarForm.set(true);
  }

  editar(p: Producto) {
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
    if (!this.modelo.nombre?.trim() || !this.modelo.precio || this.modelo.precio <= 0) {
      this.alertas.error('Datos incompletos', 'El nombre y un precio mayor a 0 son obligatorios.');
      return;
    }

    this.guardando.set(true);
    const editando = this.editando();

    const cuerpo: Producto = {
      nombre: this.modelo.nombre,
      descripcion: this.modelo.descripcion ?? null,
      precio: this.modelo.precio,
      categoria: this.modelo.categoria ?? null,
      activo: this.modelo.activo,
      stock: this.modelo.stock ?? null
    };

    const peticion = editando
      ? this.service.actualizar(editando.idProducto!, cuerpo)
      : this.service.crear(cuerpo);

    peticion.subscribe({
      next: (guardado) => {
        if (this.archivo && guardado.idProducto) {
          this.service.subirImagen(guardado.idProducto, this.archivo).subscribe({
            next: () => this.finalizar(editando, true),
            error: () => this.finalizar(editando, false)
          });
          return;
        }
        this.finalizar(editando, true);
      },
      error: (err) => {
        this.guardando.set(false);
        this.alertas.error('No se pudo guardar', err?.error?.message ?? 'Revisa los datos.');
      }
    });
  }

  toggleActivo(p: Producto) {
    this.service.toggleActivo(p.idProducto!).subscribe({
      next: () => this.cargar(),
      error: () => this.alertas.error('No se pudo cambiar el estado')
    });
  }

  async eliminar(p: Producto) {
    const ok = await this.alertas.confirmar('¿Eliminar producto?', `Se eliminará "${p.nombre}".`, 'Sí, eliminar');
    if (!ok) return;

    this.service.eliminar(p.idProducto!).subscribe({
      next: () => {
        this.alertas.exito('Producto eliminado');
        this.cargar();
      },
      error: () => this.alertas.error('No se pudo eliminar')
    });
  }

  imagen(p: Producto): string | null {
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

  private finalizar(editando: Producto | null, imagenOk: boolean) {
    this.guardando.set(false);
    this.mostrarForm.set(false);
    if (!imagenOk) {
      this.alertas.error('Se guardó el producto, pero la imagen no se pudo subir');
    } else {
      this.alertas.exito(editando ? 'Producto actualizado' : 'Producto creado');
    }
    this.cargar();
  }

  stockTexto(p: Producto): string {
    if (p.stock == null) return 'Sin límite';
    return p.stock <= 0 ? 'Agotado' : `${p.stock} u.`;
  }

  claseStock(p: Producto): string {
    if (p.stock == null) return 'tenue';
    return p.stock <= 0 ? 'agotado' : p.stock <= 5 ? 'bajo' : 'ok';
  }

  private vacio(): Producto {
    return { nombre: '', descripcion: '', precio: 0, imagenUrl: null, categoria: 'Snacks', activo: 1, stock: null };
  }
}

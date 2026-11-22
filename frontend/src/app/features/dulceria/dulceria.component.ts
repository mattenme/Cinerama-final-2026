import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Producto } from '../../core/interfaces/cinerama.interfaces';
import { formatoSoles, urlImagen } from '../../core/services/api';
import { AuthService } from '../../core/services/auth.service';
import { CarritoService } from '../../core/services/carrito.service';
import { ProductoService } from '../../core/services/catalogo.service';

@Component({
  selector: 'app-dulceria',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dulceria.component.html',
  styleUrl: './dulceria.component.css'
})
export class DulceriaComponent implements OnInit {
  private service = inject(ProductoService);
  readonly carrito = inject(CarritoService);
  readonly auth = inject(AuthService);

  /** El administrador no compra productos de dulcería. */
  readonly puedeComprar = computed(() => !this.auth.esAdmin());

  readonly productos = signal<Producto[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly categoria = signal('TODOS');
  readonly busqueda = signal('');

  readonly categorias = computed(() =>
    [...new Set(this.productos().map((p) => (p.categoria ?? '').trim()).filter((c) => !!c))].sort()
  );

  readonly filtrados = computed(() => {
    const categoria = this.categoria();
    const texto = this.busqueda().toLowerCase().trim();

    return this.productos().filter((p) => {
      const coincideCategoria = categoria === 'TODOS' || (p.categoria ?? '').trim() === categoria;
      const coincideTexto = !texto || [p.nombre, p.descripcion, p.categoria]
        .some((valor) => (valor ?? '').toLowerCase().includes(texto));
      return coincideCategoria && coincideTexto;
    });
  });

  ngOnInit(): void {
    this.service.listarActivos().subscribe({
      next: (productos) => {
        this.productos.set(productos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la dulcería. Verifica que el backend esté encendido.');
        this.cargando.set(false);
      }
    });
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
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

  agotado(p: Producto): boolean {
    return p.stock != null && p.stock <= 0;
  }
}

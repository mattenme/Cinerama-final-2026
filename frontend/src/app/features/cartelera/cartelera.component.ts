import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { Pelicula } from '../../core/interfaces/cinerama.interfaces';
import { PeliculaService } from '../../core/services/catalogo.service';
import { PeliculaCardComponent } from '../../shared/pelicula-card/pelicula-card.component';

@Component({
  selector: 'app-cartelera',
  standalone: true,
  imports: [PeliculaCardComponent],
  templateUrl: './cartelera.component.html',
  styleUrl: './cartelera.component.css'
})
export class CarteleraComponent implements OnInit {
  private service = inject(PeliculaService);

  readonly peliculas = signal<Pelicula[]>([]);
  readonly genero = signal('TODOS');
  readonly busqueda = signal('');
  readonly orden = signal<'RECIENTES' | 'AZ'>('RECIENTES');
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);

  readonly generos = computed(() =>
    [...new Set(this.peliculas().map((p) => (p.genero ?? '').trim()).filter((g) => !!g))].sort()
  );

  readonly filtradas = computed(() => {
    const genero = this.genero();
    const texto = this.busqueda().toLowerCase().trim();

    const visibles = this.peliculas().filter((p) => {
      const coincideGenero = genero === 'TODOS' || (p.genero ?? '').trim() === genero;
      const coincideTexto = !texto || [p.titulo, p.genero, p.sinopsis]
        .some((valor) => (valor ?? '').toLowerCase().includes(texto));
      return coincideGenero && coincideTexto;
    });

    return this.orden() === 'AZ'
      ? [...visibles].sort((a, b) => (a.titulo ?? '').localeCompare(b.titulo ?? '', 'es'))
      : visibles;
  });

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
  }

  ngOnInit(): void {
    this.service.listarActivas().subscribe({
      next: (peliculas) => {
        this.peliculas.set(peliculas);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la cartelera. Verifica que el backend esté encendido.');
        this.cargando.set(false);
      }
    });
  }
}

import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Pelicula, Promocion } from '../../core/interfaces/cinerama.interfaces';
import { formatoSoles, urlImagen } from '../../core/services/api';
import { PeliculaService, PromocionService } from '../../core/services/catalogo.service';
import { PeliculaCardComponent } from '../../shared/pelicula-card/pelicula-card.component';

const SEGUNDOS_POR_DIAPOSITIVA = 6;
const MAXIMO_DIAPOSITIVAS = 5;

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, PeliculaCardComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {
  private peliculaService = inject(PeliculaService);
  private promocionService = inject(PromocionService);

  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly peliculas = signal<Pelicula[]>([]);
  readonly promociones = signal<Promocion[]>([]);

  readonly indice = signal(0);
  readonly destacadas = computed(() => this.peliculas().slice(0, MAXIMO_DIAPOSITIVAS));

  private temporizador: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.cargar();
    this.reanudar();
  }

  ngOnDestroy(): void {
    this.pausar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set(null);

    forkJoin({
      peliculas: this.peliculaService.listarActivas(),
      promociones: this.promocionService.listarActivas()
    }).subscribe({
      next: ({ peliculas, promociones }) => {
        const ordenadas = [...peliculas].sort((a, b) => (b.destacado ?? 0) - (a.destacado ?? 0));
        this.peliculas.set(ordenadas);
        this.promociones.set(promociones);
        this.indice.set(0);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la cartelera. Verifica que el backend esté encendido en http://localhost:8082/cinerama');
        this.cargando.set(false);
      }
    });
  }

  anterior() {
    this.mover(-1);
  }

  siguiente() {
    this.mover(1);
  }

  irA(posicion: number) {
    this.indice.set(posicion);
    this.reanudar();
  }

  moverPoster(evento: MouseEvent) {
    if (window.matchMedia('(hover: none), (prefers-reduced-motion: reduce)').matches) return;

    const slide = evento.currentTarget as HTMLElement;
    const rectangulo = slide.getBoundingClientRect();
    const cursorX = (evento.clientX - rectangulo.left) / rectangulo.width - 0.5;
    const cursorY = (evento.clientY - rectangulo.top) / rectangulo.height - 0.5;
    const poster = slide.querySelector<HTMLElement>('.hero-poster .poster');

    if (!poster) return;

    poster.style.setProperty('--rotacion-y', `${cursorX * 14}deg`);
    poster.style.setProperty('--rotacion-x', `${cursorY * -14}deg`);
    poster.style.setProperty('--traslado-x', `${cursorX * 10}px`);
    poster.style.setProperty('--traslado-y', `${cursorY * 8}px`);
  }

  restablecerPoster(evento: MouseEvent) {
    const slide = evento.currentTarget as HTMLElement;
    const poster = slide.querySelector<HTMLElement>('.hero-poster .poster');
    if (!poster) return;

    poster.style.setProperty('--rotacion-y', '0deg');
    poster.style.setProperty('--rotacion-x', '0deg');
    poster.style.setProperty('--traslado-x', '0px');
    poster.style.setProperty('--traslado-y', '0px');
  }

  pausar() {
    if (this.temporizador) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }

  reanudar() {
    this.pausar();
    this.temporizador = setInterval(() => this.mover(1), SEGUNDOS_POR_DIAPOSITIVA * 1000);
  }

  imagen(url?: string | null): string | null {
    return urlImagen(url);
  }

  soles(valor: number): string {
    return formatoSoles(valor);
  }

  ocultar(evento: Event) {
    (evento.target as HTMLImageElement).style.display = 'none';
  }

  private mover(paso: number) {
    const total = this.destacadas().length;
    if (total <= 1) return;

    this.indice.set((this.indice() + paso + total) % total);
  }
}

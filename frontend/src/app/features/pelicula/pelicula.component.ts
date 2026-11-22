import { Component, HostListener, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Asiento, Calificacion, Funcion, Pelicula, Producto } from '../../core/interfaces/cinerama.interfaces';
import { formatoSoles, soloFecha, soloHora, urlImagen } from '../../core/services/api';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { CalificacionService } from '../../core/services/calificacion.service';
import { CarritoService } from '../../core/services/carrito.service';
import { AsientoService, FuncionService, PeliculaService, ProductoService } from '../../core/services/catalogo.service';
import { CompraService } from '../../core/services/compra.service';
import { CheckoutComponent } from '../../shared/checkout/checkout.component';

interface DiaFunciones {
  fecha: string;
  funciones: Funcion[];
}

const MAX_BUTACAS = 8;

@Component({
  selector: 'app-pelicula',
  standalone: true,
  imports: [FormsModule, RouterLink, CheckoutComponent],
  templateUrl: './pelicula.component.html',
  styleUrl: './pelicula.component.css'
})
export class PeliculaComponent implements OnInit {
  private ruta = inject(ActivatedRoute);
  private router = inject(Router);
  private peliculaService = inject(PeliculaService);
  private funcionService = inject(FuncionService);
  private asientoService = inject(AsientoService);
  private productoService = inject(ProductoService);
  private compraService = inject(CompraService);
  private alertas = inject(AlertService);
  private calificacionService = inject(CalificacionService);
  readonly auth = inject(AuthService);
  readonly carrito = inject(CarritoService);

  readonly promedio = signal(0);
  readonly totalCalificaciones = signal(0);
  readonly comentarios = signal<Calificacion[]>([]);
  readonly miCalificacion = signal<Calificacion | null>(null);
  readonly puedeCalificar = signal(false);
  readonly puntuacion = signal(0);
  readonly enviandoCalificacion = signal(false);
  comentario = '';

  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly pelicula = signal<Pelicula | null>(null);
  readonly dias = signal<DiaFunciones[]>([]);
  readonly diaSeleccionado = signal(0);
  readonly funcionSeleccionada = signal<Funcion | null>(null);
  readonly productosDulceria = signal<Producto[]>([]);
  readonly cargandoDulceria = signal(true);
  readonly errorDulceria = signal(false);
  readonly categoriaDulceria = signal('TODOS');

  readonly categoriasDulceria = computed(() =>
    [...new Set(this.productosDulceria().map((p) => (p.categoria ?? '').trim()).filter(Boolean))].sort()
  );

  readonly productosDulceriaFiltrados = computed(() => {
    const categoria = this.categoriaDulceria();
    return this.productosDulceria().filter((p) =>
      categoria === 'TODOS' || (p.categoria ?? '').trim() === categoria
    );
  });

  readonly diaActual = computed(() => this.dias()[this.diaSeleccionado()] ?? null);

  readonly cargandoSalas = signal(false);
  readonly asientos = signal<Asiento[]>([]);
  readonly ocupados = signal<Set<number>>(new Set());
  readonly filas = signal<{ fila: string; butacas: Asiento[] }[]>([]);

  readonly totalFunciones = computed(() =>
    this.dias().reduce((total, dia) => total + dia.funciones.length, 0)
  );

  /** El administrador gestiona el sistema: no compra entradas ni dulcería. */
  readonly puedeComprar = computed(() => !this.auth.esAdmin());

  readonly precio = computed(() =>
    Number(
      this.funcionSeleccionada()?.precio ??
        this.dias()[0]?.funciones[0]?.precio ??
        0
    )
  );

  constructor() {
    effect(
      () => {
        if (!this.carrito.expirado()) return;
        this.carrito.limpiarExpirado();
        this.funcionSeleccionada.set(null);
        this.router.navigate(['/home']);
        this.alertas.info('Tiempo agotado', 'Liberamos tus butacas. Elige de nuevo la función y tus asientos.');
      },
      { allowSignalWrites: true }
    );
  }

  @HostListener('window:beforeunload', ['$event'])
  avisarSalida(evento: BeforeUnloadEvent) {
    if (this.carrito.asientos().length === 0) return;
    evento.preventDefault();
    evento.returnValue = '';
  }

  ngOnInit(): void {
    if (!this.puedeComprar()) {
      this.carrito.limpiar();
    }

    const id = Number(this.ruta.snapshot.paramMap.get('id'));

    this.productoService.listarActivos().subscribe({
      next: (productos) => {
        this.productosDulceria.set(productos);
        this.cargandoDulceria.set(false);
      },
      error: () => {
        this.errorDulceria.set(true);
        this.cargandoDulceria.set(false);
      }
    });

    forkJoin({
      pelicula: this.peliculaService.obtener(id),
      funciones: this.funcionService.listarPorPelicula(id)
    }).subscribe({
      next: ({ pelicula, funciones }) => {
        this.pelicula.set(pelicula);
        this.dias.set(this.agruparPorDia(funciones));
        this.cargando.set(false);
        this.restaurarSeleccionPrevia(pelicula, funciones);
        if (pelicula.idPelicula) {
          this.cargarCalificaciones(pelicula.idPelicula);
        }
      },
      error: () => {
        this.error.set('No se pudo cargar la película. Verifica que el backend esté encendido.');
        this.cargando.set(false);
      }
    });
  }

  imagen(): string | null {
    return urlImagen(this.pelicula()?.imagenUrl);
  }

  ocultar(evento: Event) {
    (evento.target as HTMLImageElement).style.display = 'none';
  }

  hora(iso: string): string {
    return soloHora(iso);
  }

  soles(valor: number): string {
    return formatoSoles(valor);
  }

  imagenProducto(producto: Producto): string | null {
    return urlImagen(producto.imagenUrl);
  }

  productoAgotado(producto: Producto): boolean {
    return producto.stock != null && producto.stock <= 0;
  }

  productoSePuedeAgregar(producto: Producto): boolean {
    return this.puedeComprar() && !this.productoAgotado(producto) && this.carrito.puedeAgregar(producto);
  }

  elegirDia(indice: number) {
    this.diaSeleccionado.set(indice);
  }

  elegirFuncion(funcion: Funcion) {
    const pelicula = this.pelicula();
    if (!pelicula) return;

    this.funcionSeleccionada.set(funcion);
    this.carrito.seleccionarFuncion(funcion, pelicula);
    this.cargarButacas(funcion);
  }

  butacaSeleccionada(asiento: Asiento): boolean {
    return this.carrito.esButacaSeleccionada(asiento.idAsiento);
  }

  butacaOcupada(asiento: Asiento): boolean {
    return this.ocupados().has(asiento.idAsiento);
  }

  butacaBloqueada(asiento: Asiento): boolean {
    if (this.butacaOcupada(asiento)) return false;
    const estado = (asiento.estado ?? '').toUpperCase();
    return estado.includes('MANTENIMIENTO') || estado.includes('BLOQUE');
  }

  noDisponible(asiento: Asiento): boolean {
    return this.butacaOcupada(asiento) || this.butacaBloqueada(asiento);
  }

  async elegirButaca(asiento: Asiento) {
    if (!this.puedeComprar()) {
      await this.alertas.info(
        'Cuenta de administrador',
        'El administrador no puede comprar entradas. Usa una cuenta de cliente.'
      );
      return;
    }
    if (this.noDisponible(asiento)) return;

    const seleccionadas = this.carrito.asientos().length;
    if (!this.butacaSeleccionada(asiento) && seleccionadas >= MAX_BUTACAS) {
      await this.alertas.info('Máximo de butacas', `Puedes comprar hasta ${MAX_BUTACAS} butacas por función.`);
      return;
    }
    this.carrito.alternarButaca(asiento);
  }

  textoPromedio(): string {
    return this.promedio().toFixed(1);
  }

  calificar() {
    const sesion = this.auth.usuario();
    const pelicula = this.pelicula();

    if (!sesion) {
      this.router.navigate(['/cuenta/login'], {
        queryParams: { returnUrl: `/pelicula/${pelicula?.idPelicula ?? ''}` }
      });
      return;
    }
    if (!pelicula?.idPelicula || this.puntuacion() === 0) {
      this.alertas.info('Elige una puntuación', 'Toca las estrellas para calificar.');
      return;
    }

    this.enviandoCalificacion.set(true);
    this.calificacionService
      .registrar({
        idPelicula: pelicula.idPelicula,
        puntuacion: this.puntuacion(),
        comentario: this.comentario.trim() || null
      })
      .subscribe({
        next: (guardada) => {
          this.enviandoCalificacion.set(false);
          this.miCalificacion.set(guardada);
          this.alertas.exito('Gracias por calificar');
          this.cargarCalificaciones(pelicula.idPelicula!);
        },
        error: (err) => {
          this.enviandoCalificacion.set(false);
          this.alertas.error('No se pudo guardar', err?.error?.message ?? 'Inténtalo de nuevo.');
        }
      });
  }

  private cargarCalificaciones(idPelicula: number) {
    this.calificacionService.promedio(idPelicula).subscribe({
      next: (r) => {
        this.promedio.set(Number(r.promedio ?? 0));
        this.totalCalificaciones.set(Number(r.total ?? 0));
      },
      error: () => {}
    });

    this.calificacionService.listarPorPelicula(idPelicula).subscribe({
      next: (lista) => {
        const conComentario = lista
          .filter((c) => (c.comentario ?? '').trim() !== '')
          .sort(
            (a, b) =>
              new Date(b.fechaCalificacion ?? 0).getTime() -
              new Date(a.fechaCalificacion ?? 0).getTime()
          );
        this.comentarios.set(conComentario.slice(0, 5));
      },
      error: () => {}
    });

    if (!this.auth.estaLogueado()) {
      this.miCalificacion.set(null);
      this.puedeCalificar.set(false);
      return;
    }

    this.calificacionService.puedeCalificar(idPelicula).subscribe({
      next: (r) => this.puedeCalificar.set(r.puede),
      error: () => this.puedeCalificar.set(false)
    });

    this.calificacionService.mia(idPelicula).subscribe({
      next: (c) => {
        this.miCalificacion.set(c);
        this.puntuacion.set(c.puntuacion);
        this.comentario = c.comentario ?? '';
      },
      error: () => {
        this.miCalificacion.set(null);
      }
    });
  }

  private agruparPorDia(funciones: Funcion[]): DiaFunciones[] {
    const ahora = new Date();
    const vigentes = funciones
      .filter((f) => f.activo !== 0)
      .filter((f) => {
        const fecha = new Date(f.horaInicio);
        return !isNaN(fecha.getTime()) && fecha >= new Date(ahora.getTime() - 60 * 60 * 1000);
      })
      .sort((a, b) => new Date(a.horaInicio).getTime() - new Date(b.horaInicio).getTime());

    const mapa = new Map<string, Funcion[]>();
    for (const f of vigentes) {
      const clave = soloFecha(f.horaInicio);
      mapa.set(clave, [...(mapa.get(clave) ?? []), f]);
    }
    return [...mapa.entries()].map(([fecha, lista]) => ({ fecha, funciones: lista }));
  }

  private restaurarSeleccionPrevia(pelicula: Pelicula, funciones: Funcion[]) {
    const enCarrito = this.carrito.funcion();
    if (!enCarrito || this.carrito.pelicula()?.idPelicula !== pelicula.idPelicula) return;

    const vigente = funciones.find((f) => f.idFuncion === enCarrito.idFuncion);
    if (vigente) {
      this.funcionSeleccionada.set(vigente);
      const indice = this.dias().findIndex((d) =>
        d.funciones.some((f) => f.idFuncion === vigente.idFuncion)
      );
      if (indice >= 0) this.diaSeleccionado.set(indice);
      this.cargarButacas(vigente);
    }
  }

  private cargarButacas(funcion: Funcion) {
    if (!funcion.idFuncion || !funcion.idSala) return;

    this.cargandoSalas.set(true);
    this.asientos.set([]);
    this.filas.set([]);

    forkJoin({
      asientos: this.asientoService.listarPorSala(funcion.idSala),
      ocupados: this.compraService.asientosOcupados(funcion.idFuncion)
    }).subscribe({
      next: ({ asientos, ocupados }) => {
        this.asientos.set(asientos);
        this.ocupados.set(new Set(ocupados));
        this.filas.set(this.agruparPorFila(asientos));
        this.cargandoSalas.set(false);
      },
      error: () => {
        this.cargandoSalas.set(false);
        this.alertas.error('No se pudieron cargar las butacas', 'Revisa la conexión con el backend.');
      }
    });
  }

  private agruparPorFila(asientos: Asiento[]) {
    const mapa = new Map<string, Asiento[]>();
    for (const a of [...asientos].sort((x, y) => Number(x.numero) - Number(y.numero))) {
      mapa.set(a.fila, [...(mapa.get(a.fila) ?? []), a]);
    }
    return [...mapa.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([fila, butacas]) => ({ fila, butacas }));
  }
}

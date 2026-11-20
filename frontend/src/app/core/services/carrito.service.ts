import { Injectable, computed, effect, signal } from '@angular/core';

import { Asiento, Funcion, ItemCompraDulceria, Pelicula, Producto } from '../interfaces/cinerama.interfaces';

export interface ItemDulceria {
  producto: Producto;
  cantidad: number;
}

interface CarritoEstado {
  funcion: Funcion | null;
  pelicula: Pelicula | null;
  asientos: Asiento[];
  dulceria: ItemDulceria[];
}

const CLAVE = 'cinerama.dulceria';
const CLAVE_ANTERIOR = 'cinerama.carrito.v2';
const VACIO: CarritoEstado = { funcion: null, pelicula: null, asientos: [], dulceria: [] };
const SEGUNDOS_RESERVA = 10 * 60;

@Injectable({ providedIn: 'root' })
export class CarritoService {
  private estado = signal<CarritoEstado>({ ...VACIO, dulceria: this.leerDulceria() });

  readonly funcion = computed(() => this.estado().funcion);
  readonly pelicula = computed(() => this.estado().pelicula);
  readonly asientos = computed(() => this.estado().asientos);
  readonly dulceria = computed(() => this.estado().dulceria);

  readonly precioPorButaca = computed(() => Number(this.estado().funcion?.precio ?? 0));

  readonly totalButacas = computed(() => this.estado().asientos.length * this.precioPorButaca());

  readonly totalDulceria = computed(() =>
    this.estado().dulceria.reduce((suma, item) => suma + item.producto.precio * item.cantidad, 0)
  );

  readonly total = computed(() => this.totalButacas() + this.totalDulceria());

  readonly totalItems = computed(
    () =>
      this.estado().asientos.length +
      this.estado().dulceria.reduce((suma, item) => suma + item.cantidad, 0)
  );

  readonly tieneButacas = computed(() => this.estado().asientos.length > 0);

  readonly restante = signal(SEGUNDOS_RESERVA);
  readonly expirado = signal(false);

  readonly reservaTexto = computed(() => {
    const total = Math.max(0, this.restante());
    const minutos = String(Math.floor(total / 60)).padStart(2, '0');
    const segundos = String(total % 60).padStart(2, '0');
    return `${minutos}:${segundos}`;
  });

  private temporizador: ReturnType<typeof setInterval> | null = null;

  constructor() {
    try {
      localStorage.removeItem(CLAVE_ANTERIOR);
    } catch {
      /* sin almacenamiento disponible */
    }

    effect(() => {
      try {
        localStorage.setItem(CLAVE, JSON.stringify(this.estado().dulceria));
      } catch {
        /* almacenamiento lleno o bloqueado: se ignora */
      }
    });
  }

  seleccionarFuncion(funcion: Funcion, pelicula: Pelicula) {
    const actual = this.estado().funcion;
    const cambiaFuncion =
      !actual || actual.idFuncion !== funcion.idFuncion || this.estado().pelicula?.idPelicula !== pelicula.idPelicula;

    this.estado.update((e) => ({
      ...e,
      funcion,
      pelicula,
      asientos: cambiaFuncion ? [] : e.asientos
    }));
    this.ajustarTemporizador();
  }

  esButacaSeleccionada(idAsiento: number): boolean {
    return this.estado().asientos.some((a) => a.idAsiento === idAsiento);
  }

  alternarButaca(asiento: Asiento) {
    const existe = this.esButacaSeleccionada(asiento.idAsiento);
    this.estado.update((e) => ({
      ...e,
      asientos: existe
        ? e.asientos.filter((a) => a.idAsiento !== asiento.idAsiento)
        : [...e.asientos, asiento]
    }));
    this.ajustarTemporizador();
  }

  quitarButaca(idAsiento: number) {
    this.estado.update((e) => ({
      ...e,
      asientos: e.asientos.filter((a) => a.idAsiento !== idAsiento)
    }));
    this.ajustarTemporizador();
  }

  private ajustarTemporizador() {
    if (this.estado().asientos.length === 0) {
      this.detenerTemporizador();
      this.restante.set(SEGUNDOS_RESERVA);
      return;
    }
    if (this.temporizador) return;

    this.temporizador = setInterval(() => {
      const siguiente = this.restante() - 1;
      this.restante.set(siguiente);
      if (siguiente <= 0) this.liberarPorTiempo();
    }, 1000);
  }

  private liberarPorTiempo() {
    this.detenerTemporizador();
    this.restante.set(SEGUNDOS_RESERVA);
    this.estado.update((e) => ({ ...e, asientos: [] }));
    this.expirado.set(true);
  }

  private detenerTemporizador() {
    if (this.temporizador) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }

  limpiarExpirado() {
    this.expirado.set(false);
  }

  cantidadProducto(idProducto: number): number {
    return this.estado().dulceria.find((i) => i.producto.idProducto === idProducto)?.cantidad ?? 0;
  }

  puedeAgregar(producto: Producto): boolean {
    const stock = producto.stock;
    if (stock == null) return true;
    return this.cantidadProducto(producto.idProducto!) < stock;
  }

  agregarProducto(producto: Producto) {
    if (!this.puedeAgregar(producto)) return;

    this.estado.update((e) => {
      const existente = e.dulceria.find((i) => i.producto.idProducto === producto.idProducto);
      const dulceria = existente
        ? e.dulceria.map((i) =>
            i.producto.idProducto === producto.idProducto ? { ...i, cantidad: i.cantidad + 1 } : i
          )
        : [...e.dulceria, { producto, cantidad: 1 }];
      return { ...e, dulceria };
    });
  }

  quitarProducto(idProducto: number) {
    this.estado.update((e) => {
      const actual = e.dulceria.find((i) => i.producto.idProducto === idProducto);
      if (!actual) return e;
      const dulceria =
        actual.cantidad <= 1
          ? e.dulceria.filter((i) => i.producto.idProducto !== idProducto)
          : e.dulceria.map((i) =>
              i.producto.idProducto === idProducto ? { ...i, cantidad: i.cantidad - 1 } : i
            );
      return { ...e, dulceria };
    });
  }

  itemsDulceria(): ItemCompraDulceria[] {
    return this.estado().dulceria
      .filter((i) => typeof i.producto.idProducto === 'number')
      .map((i) => ({ idProducto: i.producto.idProducto!, cantidad: i.cantidad }));
  }

  limpiar() {
    this.detenerTemporizador();
    this.restante.set(SEGUNDOS_RESERVA);
    this.estado.set({ ...VACIO });
  }

  private leerDulceria(): ItemDulceria[] {
    try {
      const txt = localStorage.getItem(CLAVE);
      if (!txt) return [];
      const datos = JSON.parse(txt);
      return Array.isArray(datos) ? (datos as ItemDulceria[]) : [];
    } catch {
      return [];
    }
  }
}

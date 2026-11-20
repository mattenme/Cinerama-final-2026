// Las interfaces reflejan exactamente los DTO (records) que devuelve el backend.

export interface Pelicula {
  idPelicula?: number;
  titulo: string;
  duracionMinutos: number;
  genero?: string | null;
  sinopsis?: string | null;
  imagenUrl?: string | null;
  destacado?: number;
  activo?: number;
}

export interface Sala {
  idSala?: number;
  nombre: string;
  tipo?: string | null;
  capacidadTotal: number;
  activo?: number;
}

export interface Funcion {
  idFuncion?: number;
  idPelicula: number;
  tituloPelicula?: string | null;
  idSala: number;
  nombreSala?: string | null;
  horaInicio: string; // ISO: 2026-10-27T18:00:00
  estado?: string | null;
  activo?: number;
  /** Precio por butaca definido en la función (lo calcula el backend). */
  precio?: number | null;
}

export interface Asiento {
  idAsiento: number;
  idSala: number;
  nombreSala?: string | null;
  fila: string;
  numero: number;
  estado?: string | null;
}

export interface Producto {
  idProducto?: number;
  nombre: string;
  descripcion?: string | null;
  precio: number;
  imagenUrl?: string | null;
  categoria?: string | null;
  activo?: number;
  /** Unidades disponibles; null o ausente = sin control de stock. */
  stock?: number | null;
}

/** Un correo de la bandeja simulada del panel de administración. */
export interface CorreoEnviado {
  fecha: string;
  destino: string;
  asunto: string;
  enviado: boolean;
  detalle?: string | null;
  codigoQr?: string | null;
  html?: string | null;
}

export interface Promocion {
  idPromocion?: number;
  codigo: string;
  descripcion?: string | null;
  descuento: number;
  activo?: number;
}

export interface Compra {
  idCompra: number;
  idCliente: number;
  nombreCliente?: string | null;
  idFuncion: number;
  tituloPelicula?: string | null;
  nombreSala?: string | null;
  horaInicio?: string | null;
  idPelicula?: number | null;
  finFuncion?: string | null;
  idAsiento?: number | null;
  filaAsiento?: string | null;
  numeroAsiento?: number | null;
  precio: number;
  totalProductos?: number | null;
  metodoPago?: string | null;
  estado?: string | null;
  codigoQr?: string | null;
  fechaCompra?: string | null;
  productos?: string | null;
  usado?: number | null;
  fechaUso?: string | null;
  fechaCancelacion?: string | null;

}

export interface Validacion {
  idValidacion?: number;
  idCompra?: number | null;
  codigoQr: string;
  resultado: string;
  mensaje?: string | null;
  fecha?: string | null;
  idEmpleado?: number | null;
  tituloPelicula?: string | null;
  nombreSala?: string | null;
  horaInicio?: string | null;
  asientos?: string | null;
  nombreCliente?: string | null;
  usado?: number | null;
  fechaUso?: string | null;
}

export interface ItemCompraDulceria {
  idProducto: number;
  cantidad: number;
}

export interface CompraRequest {
  funcionId: number;
  asientosIds: number[];
  metodoPago: string;
  referenciaPago?: string | null;
  productos?: ItemCompraDulceria[] | null;
  codigoPromocion?: string | null;
}

export interface Cliente {
  idCliente?: number;
  dni: string;
  nombre: string;
  email: string;
  telefono?: string | null;
  rol?: string | null;
  activo?: number;
  verificado?: number;
}

export interface AuthResponse {
  success: boolean;
  message?: string | null;
  rol?: string | null;
  clienteId?: number | null;
  nombre?: string | null;
  codigoDesarrollo?: string | null;
  token?: string | null;
  expiraEnSegundos?: number | null;
}

export interface SesionUsuario {
  clienteId: number;
  nombre: string;
  rol: string;
}

export interface Calificacion {
  idCliente: number;
  idPelicula: number;
  puntuacion: number;
  comentario?: string | null;
  fechaCalificacion?: string | null;
  nombreCliente?: string | null;
}

export interface CalificacionRequest {
  idPelicula: number;
  puntuacion: number;
  comentario?: string | null;
}

export interface PuedeCalificar {
  puede: boolean;
  mensaje: string;
}

export interface CalificacionPromedio {
  idPelicula: number;
  promedio: number;
  total: number;
}

export interface Incidencia {
  idIncidencia?: number;
  tipo: string;
  descripcion?: string | null;
  fechaReporte?: string | null;
  estado?: string | null;
  idSala?: number | null;
  idFuncion?: number | null;
  idCliente?: number | null;
  nombreCliente?: string | null;
  nombreSala?: string | null;
  tituloPelicula?: string | null;
}

export interface IncidenciaRequest {
  tipo: string;
  descripcion?: string | null;
  idSala?: number | null;
  idFuncion?: number | null;
}

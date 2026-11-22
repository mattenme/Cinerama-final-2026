import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, tap, throwError } from 'rxjs';

import { AuthResponse, SesionUsuario } from '../interfaces/cinerama.interfaces';
import { AlertService } from './alert.service';
import { API } from './api';
import { CarritoService } from './carrito.service';

export const credencialesInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const token = auth.token();
  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(
    catchError((error) => {
      const esRutaDeSesion = /\/auth\/(login|check|logout)$/.test(req.url);
      if (error?.status === 401 && !esRutaDeSesion) {
        auth.logoutLocal();
        router.navigate(['/cuenta/login'], { queryParams: { returnUrl: router.url, expirado: 1 } });
      }
      return throwError(() => error);
    })
  );
};

const CLAVE_SESION = 'cinerama.sesion';
const CLAVE_TOKEN = 'cinerama.token';
const CLAVE_EXPIRA = 'cinerama.tokenExpira';

/** Vida del token en el backend (security.jwt.expiration-time = 8 horas). */
const SEGUNDOS_TOKEN_POR_DEFECTO = 28800;
/** Con 120 s o menos se pide un token nuevo (sesión deslizante). */
const UMBRAL_RENOVACION = 120;
/** Sin actividad durante este tiempo: se cierra la sesión y se vuelve al inicio. */
const INACTIVIDAD_MS = 30 * 60 * 1000;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private carrito = inject(CarritoService);
  private alertas = inject(AlertService);

  readonly usuario = signal<SesionUsuario | null>(this.leerGuardado());
  readonly token = signal<string | null>(this.leerToken());
  /** Segundos que le quedan al token; el header los muestra como cuenta atrás. */
  readonly segundosRestantes = signal(0);
  private temporizador: ReturnType<typeof setInterval> | null = null;
  /** Evita pedir dos renovaciones a la vez (con reintento si una se queda colgada). */
  private renovando = false;
  private ultimoIntento = 0;
  /** Instante exacto de vencimiento del token (epoch ms); la cuenta se calcula desde aquí. */
  private expiraEn = 0;

  readonly estaLogueado = computed(() => this.usuario() !== null && this.token() !== null);
  readonly esAdmin = computed(() => (this.usuario()?.rol ?? '').toUpperCase() === 'ADMIN');
  /** Solo EMPLEADO valida entradas en la puerta; el administrador no opera puerta. */
  readonly esEmpleado = computed(() => (this.usuario()?.rol ?? '').toUpperCase() === 'EMPLEADO');
  /** Último momento en que la persona interactuó con la app. */
  private ultimaActividad = Date.now();

  constructor() {
    const segundos = this.segundosGuardados();
    if (this.token() && segundos > 0) {
      this.iniciarContador(segundos);
    } else if (this.token()) {
      this.limpiar();
    }

    // Chrome frena los temporizadores en pestañas ocultas: al volver a la pestaña
    // se recalcula el tiempo real que queda (y se cierra si ya venció).
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) this.actualizarRestantes();
    });
    window.addEventListener('focus', () => this.actualizarRestantes());

    // Inactividad: cualquier gesto cuenta como que la persona sigue ahí.
    const marcarActividad = () => {
      this.ultimaActividad = Date.now();
    };
    for (const evento of ['pointerdown', 'keydown', 'scroll', 'touchstart']) {
      window.addEventListener(evento, marcarActividad, { passive: true });
    }
  }

  /** Pide un token nuevo cuando queda poco tiempo y el usuario sigue activo. */
  renovarSiHaceFalta(): void {
    if (!this.estaLogueado()) return;
    if (this.segundosRestantes() > UMBRAL_RENOVACION) return;

    const ahora = Date.now();
    if (this.renovando && ahora - this.ultimoIntento < 15000) return;

    this.renovando = true;
    this.ultimoIntento = ahora;

    this.http
      .post<AuthResponse>(`${API}/auth/renovar`, {})
      .pipe(finalize(() => (this.renovando = false)))
      .subscribe({
        next: (r) => {
          if (r.success && r.token) {
            this.guardarToken(r.token, r.expiraEnSegundos ?? SEGUNDOS_TOKEN_POR_DEFECTO);
          }
        },
        // Si no se puede renovar (token vencido) el contador termina el trabajo.
        error: () => undefined
      });
  }

  /** Valida el token contra el backend y retoma la cuenta atrás con lo que le queda. */
  restaurarSesion(): Observable<SesionUsuario | null> {
    if (!this.token()) {
      this.limpiar();
      return of(null);
    }

    return this.http.get<AuthResponse>(`${API}/auth/check`).pipe(
      map((r) => {
        if (r.success && r.clienteId) {
          const sesion: SesionUsuario = {
            clienteId: r.clienteId,
            nombre: r.nombre ?? 'Cliente',
            rol: r.rol ?? 'CLIENTE'
          };
          this.guardar(sesion);
          if (r.token) {
            this.guardarToken(r.token, r.expiraEnSegundos ?? SEGUNDOS_TOKEN_POR_DEFECTO);
          }
          return sesion;
        }
        this.limpiar();
        return null;
      }),
      catchError(() => {
        this.limpiar();
        return of(null);
      })
    );
  }

  login(dni: string, contrasena: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API}/auth/login`, { dni, contrasena })
      .pipe(
        tap((r) => {
          if (!r.success) return;

          if (r.token) {
            this.guardarToken(r.token, r.expiraEnSegundos ?? SEGUNDOS_TOKEN_POR_DEFECTO);
          }
          if (r.clienteId) {
            const anterior = this.usuario()?.clienteId;
            if (anterior && anterior !== r.clienteId) this.carrito.limpiar();
            this.guardar({
              clienteId: r.clienteId,
              nombre: r.nombre ?? 'Cliente',
              rol: r.rol ?? 'CLIENTE'
            });
          }
        })
      );
  }

  registrar(datos: {
    dni: string;
    nombre: string;
    email: string;
    telefono?: string;
    contrasena: string;
  }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/register`, datos);
  }

  verificar(email: string, codigo: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/verificar`, { email, codigo });
  }

  reenviarCodigo(email: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/reenviar-codigo`, { email });
  }

  logout(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/logout`, {}).pipe(
      tap(() => {
        this.carrito.limpiar();
        this.limpiar();
        this.router.navigate(['/home']);
      })
    );
  }

  /** Cierra la sesión solo en el navegador: se descarta el token y el carrito. */
  logoutLocal(): void {
    this.carrito.limpiar();
    this.limpiar();
  }

  /** Mensaje que devuelve el AuthResponseDTO del backend. */
  mensaje(r: AuthResponse | null | undefined): string {
    return r?.message ?? '';
  }

  private guardarToken(token: string, segundos: number) {
    this.token.set(token);
    localStorage.setItem(CLAVE_TOKEN, token);
    this.iniciarContador(segundos);
  }

  /** Cuenta atrás del token: al llegar a 0 se cierra la sesión y se pide login. */
  private iniciarContador(segundos: number) {
    this.detenerContador();
    const total = Math.max(0, Math.floor(segundos));
    this.expiraEn = Date.now() + total * 1000;
    this.segundosRestantes.set(total);
    this.guardarExpiracion();

    this.temporizador = setInterval(() => this.actualizarRestantes(), 1000);
  }

  /**
   * Recalcula el tiempo restante desde la hora de vencimiento (no resta de uno en
   * uno), así el contador sigue siendo exacto cuando el navegador frena el timer.
   */
  private actualizarRestantes() {
    if (!this.token()) return;

    const restantes = Math.max(0, Math.ceil((this.expiraEn - Date.now()) / 1000));
    this.segundosRestantes.set(restantes);
    this.guardarExpiracion();

    if (restantes <= 0) {
      this.expirar();
      return;
    }

    // Con poco tiempo por delante se renueva solo: así nadie pierde la compra
    // por tardar en elegir butacas.
    if (restantes <= UMBRAL_RENOVACION) {
      this.renovarSiHaceFalta();
    }

    if (Date.now() - this.ultimaActividad > INACTIVIDAD_MS) {
      this.cerrarPorInactividad();
    }
  }

  /**
   * Cierre por inactividad: no es un error, así que no se manda al login. Se
   * limpia la sesión y se deja a la persona en el inicio.
   */
  private cerrarPorInactividad(): void {
    const enInicio = this.router.url.startsWith('/home');
    this.carrito.limpiar();
    this.limpiar();
    if (!enInicio) {
      this.router.navigate(['/home']);
    }
    this.alertas.avisoRapido('Cerramos tu sesión por inactividad');
  }

  private detenerContador() {
    if (this.temporizador !== null) {
      clearInterval(this.temporizador);
      this.temporizador = null;
    }
  }

  /** Se guarda el instante de vencimiento para que un F5 no reinicie la cuenta. */
  private guardarExpiracion() {
    localStorage.setItem(CLAVE_EXPIRA, String(this.expiraEn));
  }

  private segundosGuardados(): number {
    const expira = Number(localStorage.getItem(CLAVE_EXPIRA));
    if (!expira || isNaN(expira)) return 0;
    return Math.max(0, Math.floor((expira - Date.now()) / 1000));
  }

  private expirar() {
    const actual = this.router.url;
    this.carrito.limpiar();
    this.limpiar();
    const returnUrl = actual.startsWith('/cuenta/login') ? null : actual;
    this.router.navigate(['/cuenta/login'], {
      queryParams: { expirado: 1, ...(returnUrl ? { returnUrl } : {}) }
    });
  }

  private guardar(sesion: SesionUsuario) {
    this.usuario.set(sesion);
    localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
  }

  private limpiar() {
    this.detenerContador();
    this.expiraEn = 0;
    this.usuario.set(null);
    this.token.set(null);
    this.segundosRestantes.set(0);
    localStorage.removeItem(CLAVE_SESION);
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_EXPIRA);
  }

  private leerGuardado(): SesionUsuario | null {
    try {
      const txt = localStorage.getItem(CLAVE_SESION);
      return txt ? (JSON.parse(txt) as SesionUsuario) : null;
    } catch {
      return null;
    }
  }

  private leerToken(): string | null {
    return localStorage.getItem(CLAVE_TOKEN);
  }
}

const destinoLogin = (router: Router, url: string) =>
  router.createUrlTree(['/cuenta/login'], { queryParams: { returnUrl: url } });

const destinoAdmin = (router: Router, url: string) =>
  router.createUrlTree(['/admin/login'], { queryParams: { returnUrl: url } });

// Si el usuario entra directo a una ruta protegida (o recarga la página), el estado
// local puede estar vacío aunque el token siga en localStorage: se consulta al
// backend antes de decidir, para no expulsar a un usuario que sí tiene token.
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.estaLogueado()) return true;

  return auth
    .restaurarSesion()
    .pipe(map((usuario) => (usuario ? true : destinoLogin(router, estado.url))));
};

export const adminGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const decidir = () => (auth.esAdmin() ? true : destinoAdmin(router, estado.url));

  if (auth.estaLogueado()) return decidir();
  return auth.restaurarSesion().pipe(map(() => decidir()));
};

/** La puerta comparte el login normal y solo la opera el personal EMPLEADO. */
export const puertaGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const decidir = () =>
    auth.esEmpleado() ? true : auth.esAdmin() ? router.createUrlTree(['/home']) : destinoLogin(router, estado.url);

  if (auth.estaLogueado()) return decidir();
  return auth.restaurarSesion().pipe(map(() => decidir()));
};

/** El soporte es para clientes: el administrador es quien lo atiende. */
export const soporteGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const decidir = () => {
    if (!auth.estaLogueado()) return destinoLogin(router, estado.url);
    return auth.esAdmin() ? router.createUrlTree(['/home']) : true;
  };

  if (auth.estaLogueado()) return decidir();
  return auth.restaurarSesion().pipe(map(() => decidir()));
};

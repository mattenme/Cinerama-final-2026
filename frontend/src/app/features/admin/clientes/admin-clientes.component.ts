import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { Cliente } from '../../../core/interfaces/cinerama.interfaces';
import { AlertService } from '../../../core/services/alert.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClienteService } from '../../../core/services/cliente.service';

@Component({
  selector: 'app-admin-clientes',
  standalone: true,
  imports: [RouterLink, AdminNavComponent],
  templateUrl: './admin-clientes.component.html',
  styleUrl: './admin-clientes.component.css'
})
export class AdminClientesComponent implements OnInit {
  private service = inject(ClienteService);
  private alertas = inject(AlertService);
  private auth = inject(AuthService);

  readonly clientes = signal<Cliente[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly ocupado = signal<number | null>(null);

  readonly busqueda = signal('');
  readonly rol = signal('TODOS');
  readonly estado = signal('TODOS');
  readonly pagina = signal(1);
  readonly tamano = 10;

  readonly roles = [
    { valor: 'TODOS', etiqueta: 'Todos' },
    { valor: 'CLIENTE', etiqueta: 'Clientes' },
    { valor: 'ADMIN', etiqueta: 'Admins' },
    { valor: 'EMPLEADO', etiqueta: 'Empleados' }
  ];

  readonly estados = [
    { valor: 'TODOS', etiqueta: 'Cualquier estado' },
    { valor: 'ACTIVOS', etiqueta: 'Activos' },
    { valor: 'INACTIVOS', etiqueta: 'Inactivos' }
  ];

  readonly activos = computed(() => this.clientes().filter((c) => c.activo === 1).length);
  readonly verificados = computed(() => this.clientes().filter((c) => c.verificado === 1).length);

  readonly filtrados = computed(() => {
    const texto = this.busqueda().toLowerCase().trim();
    const rol = this.rol();
    const estado = this.estado();

    return this.clientes().filter((c) => {
      const coincideTexto =
        !texto ||
        [c.nombre, c.dni, c.email, c.telefono].some((valor) =>
          (valor ?? '').toLowerCase().includes(texto)
        );
      const coincideRol = rol === 'TODOS' || (c.rol ?? '').toUpperCase() === rol;
      const coincideEstado =
        estado === 'TODOS' || (estado === 'ACTIVOS' ? c.activo === 1 : c.activo !== 1);

      return coincideTexto && coincideRol && coincideEstado;
    });
  });

  readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.filtrados().length / this.tamano))
  );

  readonly visibles = computed(() => {
    const inicio = (this.pagina() - 1) * this.tamano;
    return this.filtrados().slice(inicio, inicio + this.tamano);
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.error.set(null);

    this.service.listar().subscribe({
      next: (clientes) => {
        this.clientes.set(clientes);
        this.cargando.set(false);
        this.ajustarPagina();
      },
      error: () => {
        this.error.set('No se pudieron cargar los clientes. Verifica el backend.');
        this.cargando.set(false);
      }
    });
  }

  buscar(evento: Event) {
    this.busqueda.set((evento.target as HTMLInputElement).value);
    this.pagina.set(1);
  }

  filtrarRol(valor: string) {
    this.rol.set(valor);
    this.pagina.set(1);
  }

  filtrarEstado(valor: string) {
    this.estado.set(valor);
    this.pagina.set(1);
  }

  irPagina(numero: number) {
    if (numero < 1 || numero > this.totalPaginas()) return;
    this.pagina.set(numero);
  }

  toggleActivo(cliente: Cliente) {
    if (!cliente.idCliente) return;

    this.ocupado.set(cliente.idCliente);
    this.service.toggleActivo(cliente.idCliente).subscribe({
      next: () => {
        this.ocupado.set(null);
        this.alertas.exito(
          cliente.activo === 1 ? 'Cliente desactivado' : 'Cliente reactivado',
          cliente.nombre
        );
        this.cargar();
      },
      error: (err) => {
        this.ocupado.set(null);
        this.alertas.error('No se pudo cambiar el estado', err?.error?.message ?? 'Inténtalo de nuevo.');
      }
    });
  }

  async eliminar(cliente: Cliente) {
    if (!cliente.idCliente) return;

    const ok = await this.alertas.confirmar(
      '¿Eliminar cliente?',
      `Se eliminará a "${cliente.nombre}" (${cliente.dni}). Si tiene compras registradas, el sistema no lo permitirá: desactívalo en su lugar.`,
      'Sí, eliminar'
    );
    if (!ok) return;

    this.ocupado.set(cliente.idCliente);
    this.service.eliminar(cliente.idCliente).subscribe({
      next: () => {
        this.ocupado.set(null);
        this.alertas.exito('Cliente eliminado');
        this.cargar();
      },
      error: (err) => {
        this.ocupado.set(null);
        this.alertas.error(
          'No se pudo eliminar',
          err?.error?.message ?? 'El cliente tiene compras asociadas. Desactívalo en su lugar.'
        );
      }
    });
  }

  /** La cuenta con la que se inició sesión no puede desactivarse ni eliminarse a sí misma. */
  esMiCuenta(cliente: Cliente): boolean {
    return !!cliente.idCliente && cliente.idCliente === this.auth.usuario()?.clienteId;
  }

  rolTexto(cliente: Cliente): string {
    const rol = (cliente.rol ?? '').toUpperCase();
    if (rol === 'ADMIN') return 'Administrador';
    if (rol === 'EMPLEADO') return 'Empleado';
    return 'Cliente';
  }

  inicial(cliente: Cliente): string {
    return (cliente.nombre ?? '?').trim().charAt(0).toUpperCase();
  }

  private ajustarPagina() {
    if (this.pagina() > this.totalPaginas()) {
      this.pagina.set(this.totalPaginas());
    }
  }
}

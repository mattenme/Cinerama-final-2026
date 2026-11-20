import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Compra } from '../../core/interfaces/cinerama.interfaces';
import { formatoFechaHora, formatoSoles } from '../../core/services/api';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { CompraService } from '../../core/services/compra.service';
import { CalificarModalComponent } from '../../shared/calificar-modal/calificar-modal.component';

interface GrupoCompra {
  codigo: string;
  compras: Compra[];
  total: number;
  asientos: string;
  idPelicula: number | null;
  finFuncion: string | null;
}

@Component({
  selector: 'app-mis-compras',
  standalone: true,
  imports: [RouterLink, CalificarModalComponent],
  templateUrl: './mis-compras.component.html',
  styleUrl: './mis-compras.component.css'
})
export class MisComprasComponent implements OnInit, OnDestroy {
  private compraService = inject(CompraService);
  private auth = inject(AuthService);
  private alertas = inject(AlertService);

  readonly grupos = signal<GrupoCompra[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly cancelando = signal<number | null>(null);
  readonly calificando = signal<GrupoCompra | null>(null);
  readonly qrs = signal<Record<string, string>>({});

  ngOnInit(): void {
    if (!this.auth.estaLogueado()) {
      this.cargando.set(false);
      return;
    }

    this.compraService.listarMias().subscribe({
      next: (compras) => {
        const grupos = this.agrupar(compras);
        this.grupos.set(grupos);
        this.cargarQrs(grupos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar tu historial. Verifica que el backend esté encendido.');
        this.cargando.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.liberarQrs();
  }

  private cargarQrs(grupos: GrupoCompra[]) {
    this.liberarQrs();

    for (const g of grupos) {
      const idCompra = g.compras[0]?.idCompra;
      if (!idCompra) continue;

      this.compraService.obtenerQr(idCompra).subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          this.qrs.update((mapa) => ({ ...mapa, [g.codigo]: url }));
        },
        error: () => undefined
      });
    }
  }

  private liberarQrs() {
    for (const url of Object.values(this.qrs())) {
      URL.revokeObjectURL(url);
    }
    this.qrs.set({});
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }

  soles(valor: number): string {
    return formatoSoles(valor);
  }

  estadoTexto(estado?: string | null): string {
    const nombres: Record<string, string> = {
      COMPLETADA: 'Completada',
      PENDIENTE: 'Pendiente',
      CANCELADA: 'Cancelada',
      CONFIRMADA: 'Confirmada'
    };
    return nombres[(estado ?? '').toUpperCase()] ?? (estado ?? '—');
  }

  puedeCancelar(g: GrupoCompra): boolean {
    const c = g.compras[0];
    return (c.estado ?? '').toUpperCase() !== 'CANCELADA' && c.usado !== 1;
  }

  abrirCalificar(g: GrupoCompra) {
    this.calificando.set(g);
  }

  cerrarCalificar() {
    this.calificando.set(null);
  }

  puedeCalificar(g: GrupoCompra): boolean {
    const c = g.compras[0];
    if ((c.estado ?? '').toUpperCase() === 'CANCELADA') return false;
    if (!g.idPelicula || !g.finFuncion) return false;
    const fin = new Date(g.finFuncion).getTime();
    return !isNaN(fin) && fin <= Date.now();
  }

  async cancelar(g: GrupoCompra) {
    const id = g.compras[0].idCompra;
    const ok = await this.alertas.confirmar(
      '¿Cancelar esta compra?',
      `Se anularán las butacas ${g.asientos || 'de esta compra'} y volverán a estar disponibles.`,
      'Sí, cancelar'
    );
    if (!ok) return;

    this.cancelando.set(id);
    this.compraService.cancelar(id).subscribe({
      next: () => {
        this.cancelando.set(null);
        this.alertas.exito('Compra cancelada', 'Las butacas volvieron a estar disponibles.');
        this.ngOnInit();
      },
      error: (err) => {
        this.cancelando.set(null);
        this.alertas.error('No se pudo cancelar', err?.error?.message ?? 'Inténtalo de nuevo.');
      }
    });
  }

  private agrupar(compras: Compra[]): GrupoCompra[] {
    const mapa = new Map<string, Compra[]>();
    for (const c of compras) {
      const clave = c.codigoQr || `compra-${c.idCompra}`;
      mapa.set(clave, [...(mapa.get(clave) ?? []), c]);
    }

    return [...mapa.entries()]
      .map(([codigo, lista]) => ({
        codigo,
        compras: lista,
        total: lista.reduce(
          (suma, c) => suma + Number(c.precio ?? 0) + Number(c.totalProductos ?? 0),
          0
        ),
        asientos: lista
          .map((c) => `${c.filaAsiento ?? ''}${c.numeroAsiento ?? ''}`)
          .filter((t) => t.trim() !== '')
          .join(', '),
        idPelicula: lista[0]?.idPelicula ?? null,
        finFuncion: lista[0]?.finFuncion ?? null
      }))
      .sort(
        (a, b) =>
          new Date(b.compras[0]?.fechaCompra ?? 0).getTime() -
          new Date(a.compras[0]?.fechaCompra ?? 0).getTime()
      );
  }
}

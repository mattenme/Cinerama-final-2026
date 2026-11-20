import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map, switchMap } from 'rxjs';

import { Compra } from '../../core/interfaces/cinerama.interfaces';
import { formatoFechaHora, formatoSoles } from '../../core/services/api';
import { CompraService } from '../../core/services/compra.service';
import { CalificarModalComponent } from '../../shared/calificar-modal/calificar-modal.component';

@Component({
  selector: 'app-confirmacion',
  standalone: true,
  imports: [RouterLink, CalificarModalComponent],
  templateUrl: './confirmacion.component.html',
  styleUrl: './confirmacion.component.css'
})
export class ConfirmacionComponent implements OnInit, OnDestroy {
  private ruta = inject(ActivatedRoute);
  private compraService = inject(CompraService);

  readonly compras = signal<Compra[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly mostrarCalificar = signal(false);
  readonly qrSrc = signal<string | null>(null);

  readonly primera = computed(() => this.compras()[0] ?? null);
  readonly total = computed(() =>
    this.compras().reduce(
      (suma, c) => suma + Number(c.precio ?? 0) + Number(c.totalProductos ?? 0),
      0
    )
  );
  readonly asientos = computed(() =>
    this.compras().map((c) => `${c.filaAsiento ?? ''}${c.numeroAsiento ?? ''}`).filter((t) => t.trim() !== '')
  );

  readonly puedeCalificar = computed(() => {
    const c = this.primera();
    if (!c || (c.estado ?? '').toUpperCase() === 'CANCELADA') return false;
    if (!c.idPelicula || !c.finFuncion) return false;
    const fin = new Date(c.finFuncion).getTime();
    return !isNaN(fin) && fin <= Date.now();
  });

  ngOnInit(): void {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));

    this.compraService
      .obtener(id)
      .pipe(
        switchMap((base) =>
          this.compraService.listarMias().pipe(map((todas) => ({ base, todas })))
        )
      )
      .subscribe({
        next: ({ base, todas }) => {
          const grupo = todas.filter((c) => c.codigoQr === base.codigoQr && c.idFuncion === base.idFuncion);
          const lista = grupo.length > 0 ? grupo : [base];
          this.compras.set(lista);
          this.cargarQr(lista[0]?.idCompra);
          this.cargando.set(false);
        },
        error: () => {
          this.error.set('No se pudo cargar la entrada. Verifica que el backend esté encendido.');
          this.cargando.set(false);
        }
      });
  }

  ngOnDestroy(): void {
    this.liberarQr();
  }

  private cargarQr(idCompra?: number) {
    this.liberarQr();
    if (!idCompra) return;

    this.compraService.obtenerQr(idCompra).subscribe({
      next: (blob) => this.qrSrc.set(URL.createObjectURL(blob)),
      error: () => this.qrSrc.set(null)
    });
  }

  private liberarQr() {
    const actual = this.qrSrc();
    if (actual) URL.revokeObjectURL(actual);
    this.qrSrc.set(null);
  }

  soles(valor: number): string {
    return formatoSoles(valor);
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }
}

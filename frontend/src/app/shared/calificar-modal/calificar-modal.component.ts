import { Component, EventEmitter, HostListener, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Calificacion } from '../../core/interfaces/cinerama.interfaces';
import { AlertService } from '../../core/services/alert.service';
import { CalificacionService } from '../../core/services/calificacion.service';

@Component({
  selector: 'app-calificar-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './calificar-modal.component.html',
  styleUrl: './calificar-modal.component.css'
})
export class CalificarModalComponent implements OnInit {
  @Input() idPelicula = 0;
  @Input() titulo: string | null = null;

  @Output() cerrada = new EventEmitter<void>();
  @Output() guardada = new EventEmitter<Calificacion>();

  private service = inject(CalificacionService);
  private alertas = inject(AlertService);

  readonly puntuacion = signal(0);
  readonly cargando = signal(true);
  readonly enviando = signal(false);
  comentario = '';

  ngOnInit(): void {
    if (!this.idPelicula) {
      this.cargando.set(false);
      return;
    }

    this.service.mia(this.idPelicula).subscribe({
      next: (c) => {
        this.puntuacion.set(c.puntuacion);
        this.comentario = c.comentario ?? '';
        this.cargando.set(false);
      },
     
      error: () => this.cargando.set(false)
    });
  }

  @HostListener('document:keydown.escape')
  cerrar(): void {
    this.cerrada.emit();
  }

  guardar(): void {
    if (this.enviando() || this.puntuacion() === 0) return;

    this.enviando.set(true);
    this.service
      .registrar({
        idPelicula: this.idPelicula,
        puntuacion: this.puntuacion(),
        comentario: this.comentario.trim() || null
      })
      .subscribe({
        next: (c) => {
          this.enviando.set(false);
          this.alertas.exito('Gracias por calificar');
          this.guardada.emit(c);
          this.cerrada.emit();
        },
        error: (err) => {
          this.enviando.set(false);
          this.alertas.error('No se pudo guardar', err?.error?.message ?? 'Inténtalo de nuevo.');
        }
      });
  }
}

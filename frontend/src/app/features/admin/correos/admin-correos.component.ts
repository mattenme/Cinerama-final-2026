import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminNavComponent } from '../../../shared/admin-nav/admin-nav.component';

import { CorreoEnviado } from '../../../core/interfaces/cinerama.interfaces';
import { formatoFechaHora } from '../../../core/services/api';
import { AlertService } from '../../../core/services/alert.service';
import { CorreoAdminService } from '../../../core/services/correo.service';

@Component({
  selector: 'app-admin-correos',
  standalone: true,
  imports: [FormsModule, RouterLink, AdminNavComponent],
  templateUrl: './admin-correos.component.html',
  styleUrl: './admin-correos.component.css'
})
export class AdminCorreosComponent implements OnInit {
  private service = inject(CorreoAdminService);
  private alertas = inject(AlertService);

  readonly correos = signal<CorreoEnviado[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly abierto = signal<number | null>(null);
  readonly destino = signal('');
  readonly probando = signal(false);
  readonly servidor = signal('');
  readonly remitente = signal('');
  readonly resultado = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar() {
    this.cargando.set(true);
    this.service.bandeja().subscribe({
      next: (correos) => {
        this.correos.set(correos);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la bandeja de correos.');
        this.cargando.set(false);
      }
    });
  }

  alternar(indice: number) {
    this.abierto.set(this.abierto() === indice ? null : indice);
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }

  probar() {
    const destino = this.destino().trim();
    if (!destino.includes('@')) {
      this.alertas.error('Correo inválido', 'Escribe una dirección de destino válida.');
      return;
    }

    this.probando.set(true);
    this.service.probar(destino).subscribe({
      next: (resultado) => {
        this.probando.set(false);
        this.servidor.set(resultado.servidor ?? '');
        this.remitente.set(resultado.remitente ?? '');
        this.resultado.set(resultado.mensaje ?? null);
        if (resultado.enviado) {
          this.alertas.exito('Correo enviado', resultado.mensaje);
        } else {
          this.alertas.info('SMTP sin configurar', resultado.mensaje);
        }
        this.cargar();
      },
      error: () => {
        this.probando.set(false);
        this.alertas.error('No se pudo probar el correo');
      }
    });
  }

  async limpiar() {
    const ok = await this.alertas.confirmar(
      '¿Vaciar la bandeja?',
      'Solo se borran los correos guardados en memoria para la demo.',
      'Sí, vaciar'
    );
    if (!ok) return;

    this.service.limpiar().subscribe({
      next: () => {
        this.abierto.set(null);
        this.alertas.exito('Bandeja vaciada');
        this.cargar();
      },
      error: () => this.alertas.error('No se pudo vaciar la bandeja')
    });
  }
}

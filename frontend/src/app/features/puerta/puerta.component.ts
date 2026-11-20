import { Component, ElementRef, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Validacion } from '../../core/interfaces/cinerama.interfaces';
import { formatoFechaHora } from '../../core/services/api';
import { PuertaService } from '../../core/services/puerta.service';

interface LectorQr {
  detect: (fuente: HTMLVideoElement) => Promise<{ rawValue: string }[]>;
}

@Component({
  selector: 'app-puerta',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './puerta.component.html',
  styleUrl: './puerta.component.css'
})
export class PuertaComponent implements OnInit, OnDestroy {
  @ViewChild('video') video?: ElementRef<HTMLVideoElement>;

  private puerta = inject(PuertaService);

  readonly codigo = signal('');
  readonly resultado = signal<Validacion | null>(null);
  readonly historial = signal<Validacion[]>([]);
  readonly validando = signal(false);
  readonly camaraActiva = signal(false);
  readonly errorCamara = signal<string | null>(null);

  private stream: MediaStream | null = null;
  private detector: LectorQr | null = null;
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private escaneando = false;

  ngOnInit(): void {
    this.cargarHistorial();
  }

  ngOnDestroy(): void {
    this.detenerCamara();
  }

  get soportaCamara(): boolean {
    return typeof window !== 'undefined' && 'BarcodeDetector' in window;
  }

  fecha(iso?: string | null): string {
    return formatoFechaHora(iso);
  }

  async iniciarCamara() {
    this.errorCamara.set(null);
    this.resultado.set(null);

    if (!('BarcodeDetector' in window)) {
      this.errorCamara.set(
        'Este navegador no trae lector de QR. Escribe el código a mano o usa Chrome/Edge actualizado.'
      );
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      this.errorCamara.set('No se puede acceder a la cámara desde esta dirección. Usa la entrada manual.');
      return;
    }

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false
      });

      const video = this.video?.nativeElement;
      if (!video) return;
      video.srcObject = this.stream;
      await video.play();

      const ventana = window as unknown as {
        BarcodeDetector: new (opciones: { formats: string[] }) => LectorQr;
      };
      this.detector = new ventana.BarcodeDetector({ formats: ['qr_code'] });
      this.camaraActiva.set(true);
      this.escanear();
    } catch {
      this.errorCamara.set('No se pudo abrir la cámara. Revisa los permisos del navegador.');
      this.detenerCamara();
    }
  }

  detenerCamara() {
    this.camaraActiva.set(false);
    if (this.temporizador) {
      clearTimeout(this.temporizador);
      this.temporizador = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
      this.stream = null;
    }
    this.escaneando = false;
  }

  validar() {
    const valor = this.codigo().trim();
    if (!valor) {
      this.errorCamara.set('Escribe o escanea un código QR.');
      return;
    }
    if (this.validando()) return;

    this.validando.set(true);
    this.errorCamara.set(null);

    this.puerta.validar(valor).subscribe({
      next: (r) => {
        this.validando.set(false);
        this.resultado.set(r);
        this.cargarHistorial();
      },
      error: (err) => {
        this.validando.set(false);
        this.errorCamara.set(err?.error?.message ?? 'No se pudo validar el código.');
      }
    });
  }

  escanearOtra() {
    this.codigo.set('');
    this.resultado.set(null);
    this.errorCamara.set(null);
    this.iniciarCamara();
  }

  esValido(): boolean {
    return this.resultado()?.resultado === 'VALIDO';
  }

  private cargarHistorial() {
    this.puerta.recientes().subscribe({
      next: (lista) => this.historial.set(lista),
      error: () => this.historial.set([])
    });
  }

  private async escanear() {
    const video = this.video?.nativeElement;
    if (!this.camaraActiva() || !video || !this.detector) return;

    if (!this.escaneando && video.readyState >= 2) {
      this.escaneando = true;
      try {
        const codigos = await this.detector.detect(video);
        const valor = codigos.length > 0 ? String(codigos[0].rawValue ?? '').trim() : '';
        if (valor) {
          this.codigo.set(valor);
          this.detenerCamara();
          this.validar();
          return;
        }
      } catch {
      }
      this.escaneando = false;
    }

    this.temporizador = setTimeout(() => this.escanear(), 500);
  }
}

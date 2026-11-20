import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../core/services/alert.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.css'
})
export class RegistroComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private alertas = inject(AlertService);

  datos = { dni: '', nombre: '', email: '', telefono: '', contrasena: '' };
  readonly paso = signal(1);
  readonly cargando = signal(false);
  readonly verificando = signal(false);
  readonly error = signal<string | null>(null);
  readonly codigoDesarrollo = signal<string | null>(null);
  codigo = '';

  registrar() {
    this.error.set(null);

    if (!this.datos.dni.trim() || !this.datos.nombre.trim() || !this.datos.email.trim() || !this.datos.contrasena) {
      this.error.set('Completa los campos obligatorios.');
      return;
    }
    if (!/^\d{8}$/.test(this.datos.dni.trim())) {
      this.error.set('El DNI debe tener exactamente 8 dígitos.');
      return;
    }
    if (this.datos.contrasena.length < 6) {
      this.error.set('La contraseña debe tener al menos 6 caracteres.');
      return;
    }

    this.cargando.set(true);
    this.auth.registrar(this.datos).subscribe({
      next: (r) => {
        this.cargando.set(false);
        if (!r.success) {
          this.error.set(this.auth.mensaje(r) || 'No se pudo registrar la cuenta.');
          return;
        }
        this.codigoDesarrollo.set(r.codigoDesarrollo ?? null);
        if (r.codigoDesarrollo) this.codigo = r.codigoDesarrollo;
        this.paso.set(2);
        if (!r.codigoDesarrollo && /no se pudo enviar/i.test(r.message ?? '')) {
          this.alertas.error('No se pudo enviar el correo', r.message ?? undefined);
        } else {
          this.alertas.exito('Cuenta creada', 'Verifica el código para activar tu cuenta.');
        }
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo conectar con el servidor.');
      }
    });
  }

  verificar() {
    if (this.codigo.trim().length !== 6) {
      this.error.set('El código debe tener 6 dígitos.');
      return;
    }

    this.error.set(null);
    this.verificando.set(true);

    this.auth.verificar(this.datos.email.trim(), this.codigo.trim()).subscribe({
      next: (r) => {
        this.verificando.set(false);
        if (!r.success) {
          this.error.set(this.auth.mensaje(r) || 'Código incorrecto.');
          return;
        }
        this.alertas.exito('Cuenta verificada', 'Ya puedes iniciar sesión.');
        this.router.navigate(['/cuenta/login']);
      },
      error: () => {
        this.verificando.set(false);
        this.error.set('No se pudo verificar el código.');
      }
    });
  }

  reenviar() {
    this.auth.reenviarCodigo(this.datos.email.trim()).subscribe({
      next: (r) => {
        if (r.codigoDesarrollo) {
          this.codigoDesarrollo.set(r.codigoDesarrollo);
          this.codigo = r.codigoDesarrollo;
        }
        if (/no se pudo/i.test(r.message ?? '')) {
          this.alertas.error('No se pudo reenviar el correo', r.message ?? undefined);
        } else {
          this.alertas.info('Código reenviado', this.auth.mensaje(r));
        }
      },
      error: () => this.alertas.error('No se pudo reenviar el código')
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../core/services/alert.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);
  private alertas = inject(AlertService);

  dni = '';
  contrasena = '';
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly modoAdmin = this.ruta.snapshot.data['admin'] === true;
  readonly sesionExpirada = this.ruta.snapshot.queryParamMap.get('expirado') === '1';

  ingresar() {
    if (!this.dni.trim() || !this.contrasena) {
      this.error.set('Ingresa tu DNI y contraseña.');
      return;
    }

    this.cargando.set(true);
    this.error.set(null);

    this.auth.login(this.dni.trim(), this.contrasena).subscribe({
      next: (r) => {
        this.cargando.set(false);
        if (!r.success) {
          this.error.set(this.auth.mensaje(r) || 'No se pudo iniciar sesión.');
          return;
        }
        const esAdmin = (r.rol ?? '').toUpperCase() === 'ADMIN';

        if (this.modoAdmin && !esAdmin) {
          this.auth.logoutLocal();
          this.error.set('Esta cuenta no tiene permisos de administración.');
          return;
        }

        this.alertas.exito(`Bienvenido, ${r.nombre}`);
        const destino = this.modoAdmin
          ? (this.ruta.snapshot.queryParamMap.get('returnUrl') || '/admin/peliculas')
          : (this.ruta.snapshot.queryParamMap.get('returnUrl') || '/home');
        this.router.navigateByUrl(destino);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo conectar con el servidor. Verifica que el backend esté encendido.');
      }
    });
  }
}

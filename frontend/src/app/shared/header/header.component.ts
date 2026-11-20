import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements AfterViewInit, OnDestroy {
  readonly auth = inject(AuthService);
  private router = inject(Router);
  readonly menuAbierto = signal(false);

  @ViewChild('navegacion') navegacion?: ElementRef<HTMLElement>;
  @ViewChild('marca') marca?: ElementRef<HTMLElement>;
  private cambiosRuta?: Subscription;

  ngAfterViewInit(): void {
    setTimeout(() => this.actualizarLinea());
    this.cambiosRuta = this.router.events.subscribe((evento) => {
      if (evento instanceof NavigationEnd) {
        setTimeout(() => this.actualizarLinea());
      }
    });
  }

  ngOnDestroy(): void {
    this.cambiosRuta?.unsubscribe();
  }

  seleccionarEnlace(evento: Event) {
    this.moverLinea(evento.currentTarget as HTMLElement);
  }

  private actualizarLinea() {
    const nav = this.navegacion?.nativeElement;
    const activo = nav?.querySelector<HTMLElement>('a.activo');
    if (activo) {
      this.moverLinea(activo);
    } else if (this.router.url.startsWith('/home')) {
      const marca = this.marca?.nativeElement;
      if (marca) this.moverLinea(marca);
    } else {
      this.limpiarLinea();
    }
  }

  irInicio() {
    const marca = this.marca?.nativeElement;
    if (marca) this.moverLinea(marca);
    this.cerrarMenu();
  }

  private limpiarLinea() {
    const nav = this.navegacion?.nativeElement;
    nav?.style.setProperty('--linea-x', '0px');
    nav?.style.setProperty('--linea-ancho', '0px');
  }

  private moverLinea(enlace: HTMLElement) {
    const nav = this.navegacion?.nativeElement;
    if (!nav || nav.getClientRects().length === 0) return;

    const rectanguloNav = nav.getBoundingClientRect();
    const rectanguloEnlace = enlace.getBoundingClientRect();
    nav.style.setProperty('--linea-x', `${rectanguloEnlace.left - rectanguloNav.left}px`);
    nav.style.setProperty('--linea-ancho', `${rectanguloEnlace.width}px`);
  }

  alternarMenu() {
    this.menuAbierto.update((v) => !v);
  }

  cerrarMenu() {
    this.menuAbierto.set(false);
  }

  salir() {
    this.auth.logout().subscribe({
      error: () => {
        this.auth.logoutLocal();
        this.router.navigate(['/home']);
      }
    });
  }
}

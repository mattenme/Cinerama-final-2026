import { Routes } from '@angular/router';

import { adminGuard, authGuard, puertaGuard, soporteGuard } from './core/services/auth.service';
import { salirConAsientosGuard } from './core/guard/salir-asientos.guard';
import { MainLayoutComponent } from './shared/layouts/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { CarteleraComponent } from './features/cartelera/cartelera.component';
import { PeliculaComponent } from './features/pelicula/pelicula.component';
import { DulceriaComponent } from './features/dulceria/dulceria.component';
import { ConfirmacionComponent } from './features/confirmacion/confirmacion.component';
import { MisComprasComponent } from './features/mis-compras/mis-compras.component';
import { LoginComponent } from './features/cuenta/login/login.component';
import { RegistroComponent } from './features/cuenta/registro/registro.component';
import { AdminPeliculasComponent } from './features/admin/peliculas/admin-peliculas.component';
import { AdminFuncionesComponent } from './features/admin/funciones/admin-funciones.component';
import { AdminProductosComponent } from './features/admin/productos/admin-productos.component';
import { AdminPromocionesComponent } from './features/admin/promociones/admin-promociones.component';
import { AdminSalasComponent } from './features/admin/salas/admin-salas.component';
import { AdminIncidenciasComponent } from './features/admin/incidencias/admin-incidencias.component';
import { AdminClientesComponent } from './features/admin/clientes/admin-clientes.component';
import { AdminReportesComponent } from './features/admin/reportes/admin-reportes.component';
import { AdminCorreosComponent } from './features/admin/correos/admin-correos.component';
import { PuertaComponent } from './features/puerta/puerta.component';
import { SoporteComponent } from './features/soporte/soporte.component';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'home' },
      { path: 'home', component: HomeComponent },
      { path: 'cartelera', component: CarteleraComponent },
      { path: 'pelicula/:id', component: PeliculaComponent, canDeactivate: [salirConAsientosGuard] },
      { path: 'dulceria', component: DulceriaComponent },

      { path: 'confirmacion/:id', component: ConfirmacionComponent },
      { path: 'mis-compras', component: MisComprasComponent, canActivate: [authGuard] },
      { path: 'soporte', component: SoporteComponent, canActivate: [soporteGuard] },
      { path: 'puerta', component: PuertaComponent, canActivate: [puertaGuard] },
      { path: 'cuenta/login', component: LoginComponent },
      { path: 'cuenta/registro', component: RegistroComponent },
      { path: 'admin', pathMatch: 'full', redirectTo: 'admin/peliculas' },
      { path: 'admin/login', component: LoginComponent, data: { admin: true } },
      { path: 'admin/peliculas', component: AdminPeliculasComponent, canActivate: [adminGuard] },
      { path: 'admin/funciones', component: AdminFuncionesComponent, canActivate: [adminGuard] },
      { path: 'admin/productos', component: AdminProductosComponent, canActivate: [adminGuard] },
      { path: 'admin/promociones', component: AdminPromocionesComponent, canActivate: [adminGuard] },
      { path: 'admin/salas', component: AdminSalasComponent, canActivate: [adminGuard] },
      { path: 'admin/clientes', component: AdminClientesComponent, canActivate: [adminGuard] },
      { path: 'admin/incidencias', component: AdminIncidenciasComponent, canActivate: [adminGuard] },
      { path: 'admin/reportes', component: AdminReportesComponent, canActivate: [adminGuard] },
      { path: 'admin/correos', component: AdminCorreosComponent, canActivate: [adminGuard] }
    ]
  },
  { path: '**', redirectTo: 'home' }
];

import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-admin-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './admin-nav.component.html',
  styleUrl: './admin-nav.component.css'
})
export class AdminNavComponent {
  readonly secciones = [
    { ruta: '/admin/peliculas', etiqueta: 'Películas' },
    { ruta: '/admin/funciones', etiqueta: 'Funciones' },
    { ruta: '/admin/salas', etiqueta: 'Salas' },
    { ruta: '/admin/productos', etiqueta: 'Dulcería' },
    { ruta: '/admin/promociones', etiqueta: 'Promociones' },
    { ruta: '/admin/clientes', etiqueta: 'Clientes' },
    { ruta: '/admin/incidencias', etiqueta: 'Incidencias' },
    { ruta: '/admin/reportes', etiqueta: 'Reportes' },
    { ruta: '/admin/correos', etiqueta: 'Correos' }
  ] as const;
}

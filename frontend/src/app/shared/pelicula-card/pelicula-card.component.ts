import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Pelicula } from '../../core/interfaces/cinerama.interfaces';
import { urlImagen } from '../../core/services/api';

@Component({
  selector: 'app-pelicula-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './pelicula-card.component.html',
  styleUrl: './pelicula-card.component.css'
})
export class PeliculaCardComponent {
  @Input({ required: true }) pelicula!: Pelicula;

  imagen(): string | null {
    return urlImagen(this.pelicula.imagenUrl);
  }

  ocultar(evento: Event) {
    (evento.target as HTMLImageElement).style.display = 'none';
  }
}

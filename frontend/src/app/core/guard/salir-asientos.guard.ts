import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { AlertService } from '../services/alert.service';
import { CarritoService } from '../services/carrito.service';

const DESTINOS_PERMITIDOS = ['/dulceria', '/cuenta/login', '/confirmacion'];

export const salirConAsientosGuard: CanDeactivateFn<unknown> = (_componente, _actual, _siguiente, estado) => {
  const carrito = inject(CarritoService);
  const alertas = inject(AlertService);

  if (carrito.asientos().length === 0) return true;

  const destino = estado.url.split('?')[0];
  if (DESTINOS_PERMITIDOS.some((ruta) => destino.startsWith(ruta))) return true;

  return alertas.confirmar(
    '¿Salir sin las butacas?',
    'Tus butacas no se guardan: si sales ahora se liberan y tendrás que elegirlas otra vez.',
    'Sí, salir'
  );
};

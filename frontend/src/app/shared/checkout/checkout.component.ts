import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Promocion } from '../../core/interfaces/cinerama.interfaces';
import { formatoSoles } from '../../core/services/api';
import { AlertService } from '../../core/services/alert.service';
import { AuthService } from '../../core/services/auth.service';
import { CarritoService } from '../../core/services/carrito.service';
import { PromocionService } from '../../core/services/catalogo.service';
import { CompraService } from '../../core/services/compra.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent {
  private promoService = inject(PromocionService);
  private compraService = inject(CompraService);
  private alertas = inject(AlertService);
  private router = inject(Router);
  readonly auth = inject(AuthService);
  readonly carrito = inject(CarritoService);
  readonly returnUrl = this.router.url;

  readonly codigo = signal('');
  readonly promocion = signal<Promocion | null>(null);
  readonly buscandoPromo = signal(false);

  readonly metodoPago = signal('TARJETA');
  readonly enviando = signal(false);
  readonly mensajeError = signal<string | null>(null);
  readonly errorPago = signal<string | null>(null);

  readonly numeroTarjeta = signal('4111 1111 1111 1111');
  readonly titularTarjeta = signal('CLIENTE DEMO');
  readonly venceTarjeta = signal('12/29');
  readonly cvvTarjeta = signal('123');
  readonly celularBilletera = signal('987 654 321');
  readonly codigoBilletera = signal('123456');

  readonly descuento = computed(() => {
    const promo = this.promocion();
    if (!promo) return 0;
    return (this.carrito.totalButacas() * promo.descuento) / 100;
  });

  readonly totalPagar = computed(
    () => this.carrito.totalButacas() - this.descuento() + this.carrito.totalDulceria()
  );

  readonly metodos = [
    { valor: 'TARJETA', texto: 'Tarjeta de crédito / débito', corto: 'Tarjeta', icono: 'bi-credit-card' },
    { valor: 'EFECTIVO', texto: 'Efectivo en taquilla', corto: 'Efectivo', icono: 'bi-cash-coin' },
    { valor: 'YAPE', texto: 'Yape', corto: 'Yape', icono: 'bi-phone' },
    { valor: 'PLIN', texto: 'Plin', corto: 'Plin', icono: 'bi-phone-vibrate' }
  ];

  readonly referenciaPago = computed(() => {
    const metodo = this.metodoPago();
    if (metodo === 'TARJETA') {
      const numero = this.digitos(this.numeroTarjeta(), 16);
      return numero.length === 16 ? `**** ${numero.slice(-4)}` : '';
    }
    if (metodo === 'YAPE' || metodo === 'PLIN') {
      const celular = this.digitos(this.celularBilletera(), 9);
      return celular.length === 9 ? `${celular.slice(0, 3)}***${celular.slice(6)}` : '';
    }
    return 'Pago en taquilla';
  });

  readonly etiquetaPago = computed(() => {
    const elegido = this.metodos.find((m) => m.valor === this.metodoPago());
    const referencia = this.referenciaPago();
    const nombre = elegido?.corto ?? this.metodoPago();
    return referencia ? `${nombre} ${referencia}` : nombre;
  });

  soles(valor: number): string {
    return formatoSoles(valor);
  }

  aplicarPromocion() {
    const codigo = this.codigo().trim().toUpperCase();
    if (!codigo) {
      this.promocion.set(null);
      return;
    }

    this.buscandoPromo.set(true);
    this.promoService.buscarPorCodigo(codigo).subscribe({
      next: (promo) => {
        this.buscandoPromo.set(false);
        if (promo.activo === 0) {
          this.promocion.set(null);
          this.alertas.info('Promoción inactiva', 'Ese código ya no está vigente.');
          return;
        }
        this.promocion.set(promo);
        this.codigo.set(promo.codigo);
        this.alertas.exito('Promoción aplicada', `${promo.descuento}% de descuento en tus butacas.`);
      },
      error: () => {
        this.buscandoPromo.set(false);
        this.promocion.set(null);
        this.alertas.error('Código inválido', 'Verifica el código de la promoción.');
      }
    });
  }

  quitarPromocion() {
    this.promocion.set(null);
    this.codigo.set('');
  }

  elegirMetodo(valor: string) {
    this.metodoPago.set(valor);
    this.errorPago.set(null);
  }

  teclearTarjeta(valor: string) {
    this.numeroTarjeta.set(this.digitos(valor, 16).replace(/(\d{4})(?=\d)/g, '$1 '));
    this.errorPago.set(null);
  }

  teclearVencimiento(valor: string) {
    const digitos = this.digitos(valor, 4);
    this.venceTarjeta.set(digitos.length > 2 ? `${digitos.slice(0, 2)}/${digitos.slice(2)}` : digitos);
  }

  teclearCvv(valor: string) {
    this.cvvTarjeta.set(this.digitos(valor, 4));
  }

  teclearCelular(valor: string) {
    const digitos = this.digitos(valor, 9);
    this.celularBilletera.set(
      [digitos.slice(0, 3), digitos.slice(3, 6), digitos.slice(6)].filter(Boolean).join(' ')
    );
  }

  teclearCodigo(valor: string) {
    this.codigoBilletera.set(this.digitos(valor, 6));
  }

  private digitos(valor: string, maximo: number): string {
    return valor.replace(/\D/g, '').slice(0, maximo);
  }

  private validarPago(): string | null {
    const metodo = this.metodoPago();

    if (metodo === 'TARJETA') {
      if (this.digitos(this.numeroTarjeta(), 16).length !== 16) {
        return 'El número de tarjeta debe tener 16 dígitos.';
      }
      if (!this.titularTarjeta().trim()) {
        return 'Escribe el nombre del titular de la tarjeta.';
      }
      if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(this.venceTarjeta())) {
        return 'El vencimiento debe tener el formato MM/AA.';
      }
      if (this.vencida(this.venceTarjeta())) {
        return 'La tarjeta está vencida. Usa otra o cambia de método de pago.';
      }
      if (!/^\d{3,4}$/.test(this.cvvTarjeta())) {
        return 'El CVV debe tener 3 o 4 dígitos.';
      }
      return null;
    }

    if (metodo === 'YAPE' || metodo === 'PLIN') {
      if (!/^9\d{8}$/.test(this.digitos(this.celularBilletera(), 9))) {
        return 'El celular debe tener 9 dígitos y empezar con 9.';
      }
      if (!/^\d{6}$/.test(this.codigoBilletera())) {
        return 'El código de aprobación debe tener 6 dígitos.';
      }
      return null;
    }

    return null;
  }

  private vencida(vence: string): boolean {
    const [mes, anio] = vence.split('/').map((parte) => Number(parte));
    const finDeVigencia = new Date(2000 + anio, mes, 1);
    return finDeVigencia <= new Date();
  }

  pagar() {
    if (this.enviando()) return;

    const sesion = this.auth.usuario();
    if (!sesion) {
      this.router.navigate(['/cuenta/login'], { queryParams: { returnUrl: this.returnUrl } });
      return;
    }

    const funcion = this.carrito.funcion();
    const asientos = this.carrito.asientos();
    if (!funcion?.idFuncion || asientos.length === 0) {
      this.alertas.info('Falta elegir butacas', 'Selecciona una función y tus butacas primero.');
      return;
    }

    const problemaPago = this.validarPago();
    if (problemaPago) {
      this.errorPago.set(problemaPago);
      return;
    }

    this.enviando.set(true);
    this.mensajeError.set(null);
    this.errorPago.set(null);

    this.compraService
      .procesar({
        funcionId: funcion.idFuncion,
        asientosIds: asientos.map((a) => a.idAsiento),
        metodoPago: this.metodoPago(),
        referenciaPago: this.referenciaPago(),
        productos: this.carrito.itemsDulceria(),
        codigoPromocion: this.promocion()?.codigo ?? null
      })
      .subscribe({
        next: (compras) => {
          this.enviando.set(false);
          const primera = compras[0];
          this.carrito.limpiar();
          this.router.navigate(['/confirmacion', primera.idCompra]);
        },
        error: (err) => {
          this.enviando.set(false);
          const texto =
            err?.error?.message ??
            'No se pudo procesar la compra. Puede que otra persona haya tomado esas butacas.';
          this.mensajeError.set(texto);
          this.alertas.error('Compra no completada', texto);
        }
      });
  }
}

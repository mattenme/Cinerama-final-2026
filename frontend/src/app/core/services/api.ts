export const API = '/api/v1';

export function urlImagen(ruta?: string | null): string | null {
  if (!ruta) return null;
  const valor = ruta.trim();
  if (!valor) return null;

  if (/^https?:\/\//i.test(valor)) return valor;

  if (valor.startsWith('/cinerama/uploads/')) return valor;
  if (valor.startsWith('/uploads/')) return `/cinerama${valor}`;

  return null;
}

export function formatoSoles(valor: number): string {
  return `S/ ${Number(valor ?? 0).toFixed(2)}`;
}

export function formatoFechaHora(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleString('es-PE', {
    weekday: 'short', day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
  });
}

export function soloFecha(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('es-PE', { weekday: 'long', day: '2-digit', month: 'long' });
}

export function soloHora(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  return d.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

export function AIsoDatetimeLocal(iso?: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

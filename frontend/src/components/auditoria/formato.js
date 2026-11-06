export const ACCIONES = {
  CREAR_EXPEDIENTE: { texto: 'Creó expediente', icono: 'mas', tipo: 'crear' },
  EDITAR_EXPEDIENTE: { texto: 'Editó expediente', icono: 'editar', tipo: 'editar' },
  ELIMINAR_EXPEDIENTE: { texto: 'Eliminó expediente', icono: 'basura', tipo: 'critica' },
  INICIO_FALLIDO: { texto: 'Intento fallido', icono: 'alerta', tipo: 'critica' },
  INICIO_SESION: { texto: 'Inició sesión', icono: 'entrar', tipo: 'sesion' },
  CIERRE_SESION: { texto: 'Cerró sesión', icono: 'salir', tipo: 'sesion' },
  CAMBIO_PASSWORD: { texto: 'Cambió su contraseña', icono: 'llave', tipo: 'sesion' },
  CREAR_USUARIO: { texto: 'Creó usuario', icono: 'usuarios', tipo: 'crear' },
  EDITAR_USUARIO: { texto: 'Editó usuario', icono: 'usuarios', tipo: 'editar' },
  ELIMINAR_USUARIO: { texto: 'Eliminó usuario', icono: 'basura', tipo: 'critica' },
  COPIA_USB: { texto: 'Copió respaldo a USB', icono: 'usb', tipo: 'sesion' },
};

export const ACCIONES_EXPEDIENTE = ['CREAR_EXPEDIENTE', 'EDITAR_EXPEDIENTE', 'ELIMINAR_EXPEDIENTE'];

const LOCALES = ['127.0.0.1', '::1', '0:0:0:0:0:0:0:1'];

export function accion(clave) {
  return ACCIONES[clave] || { texto: clave, icono: 'historial', tipo: 'sesion' };
}

export function fechaDe(texto) {
  return texto ? new Date(texto.replace(' ', 'T')) : null;
}

export function hora(texto) {
  return texto ? texto.slice(11, 16) : '';
}

const dosDigitos = (n) => String(n).padStart(2, '0');
export const diaDe = (d) => `${d.getFullYear()}-${dosDigitos(d.getMonth() + 1)}-${dosDigitos(d.getDate())}`;

export function fechaLarga(texto) {
  const d = fechaDe(texto);
  return d ? d.toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' }) : '';
}

export function etiquetaDia(texto) {
  const hoy = new Date();
  const ayer = new Date(hoy);
  ayer.setDate(hoy.getDate() - 1);
  const dia = texto.slice(0, 10);
  if (dia === diaDe(hoy)) return 'Hoy';
  if (dia === diaDe(ayer)) return 'Ayer';
  return fechaLarga(texto);
}

export function haceCuanto(texto, ahora = new Date()) {
  const d = fechaDe(texto);
  if (!d) return '';
  const minutos = Math.floor((ahora - d) / 60000);
  if (minutos < 1) return 'hace un momento';
  if (minutos < 60) return `hace ${minutos} min`;
  const horas = Math.floor(minutos / 60);
  if (horas < 24) return `hace ${horas} h`;
  const dias = Math.floor(horas / 24);
  return dias === 1 ? 'hace 1 día' : `hace ${dias} días`;
}

export function duracion(inicio, fin) {
  const a = fechaDe(inicio);
  const b = fechaDe(fin);
  if (!a || !b) return '';
  const minutos = Math.max(0, Math.round((b - a) / 60000));
  if (minutos < 1) return 'menos de 1 min';
  if (minutos < 60) return `${minutos} min`;
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return m ? `${h} h ${m} min` : `${h} h`;
}

export function agruparPorDia(filas, campo) {
  const grupos = [];
  for (const fila of filas) {
    const dia = fila[campo].slice(0, 10);
    const ultimo = grupos[grupos.length - 1];
    if (ultimo && ultimo.dia === dia) ultimo.filas.push(fila);
    else grupos.push({ dia, etiqueta: etiquetaDia(fila[campo]), filas: [fila] });
  }
  return grupos;
}

export function equipo(r) {
  if (LOCALES.includes(r.equipo)) return { nombre: 'Computadora principal', ip: null, titulo: 'Esta computadora' };
  if (r.equipoNombre) return { nombre: r.equipoNombre, ip: r.equipo, titulo: r.equipo };
  return { nombre: r.equipo || '—', ip: null, titulo: r.equipo || '' };
}

export function persona(r) {
  return { nombre: r.nombre || r.usuario, usuario: r.usuario };
}

export function cambiosDe(m) {
  if (!m.cambios) return null;
  try {
    return JSON.parse(m.cambios);
  } catch {
    return null;
  }
}

export function partesObjeto(objeto) {
  if (!objeto) return { numero: null, resto: null };
  const i = objeto.indexOf(' · ');
  return i < 0 ? { numero: objeto, resto: null } : { numero: objeto.slice(0, i), resto: objeto.slice(i + 3) };
}

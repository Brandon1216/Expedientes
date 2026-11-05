const CLAVE_TOKEN = 'expedientes.token';

localStorage.removeItem(CLAVE_TOKEN);
if (performance.getEntriesByType('navigation')[0]?.type !== 'reload') {
  sessionStorage.removeItem(CLAVE_TOKEN);
}

export const sesion = {
  token: () => sessionStorage.getItem(CLAVE_TOKEN),
  guardar: (token) => sessionStorage.setItem(CLAVE_TOKEN, token),
  borrar: () => sessionStorage.removeItem(CLAVE_TOKEN),
};

async function peticion(ruta, { metodo = 'GET', datos } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = sesion.token();
  if (token) headers.Authorization = `Bearer ${token}`;

  let respuesta;
  try {
    respuesta = await fetch(`/api${ruta}`, {
      method: metodo,
      headers,
      body: datos !== undefined ? JSON.stringify(datos) : undefined,
    });
  } catch {
    throw new Error('No hay conexión con el servidor. Revisa que el backend esté encendido.');
  }

  if (respuesta.status === 401 && ruta !== '/auth/login') {
    sesion.borrar();
    window.dispatchEvent(new Event('sesion-expirada'));
  }

  if (respuesta.status === 204) return null;
  const cuerpo = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new Error(cuerpo?.error || `Error ${respuesta.status}`);
  }
  return cuerpo;
}

export const api = {
  login: (usuario, password) => peticion('/auth/login', { metodo: 'POST', datos: { usuario, password } }),
  yo: () => peticion('/auth/yo'),
  cambiarPassword: (actual, nueva) => peticion('/auth/password', { metodo: 'PUT', datos: { actual, nueva } }),

  expedientes: (q = '', status = '', secretaria = '') => {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (status) params.set('status', status);
    if (secretaria) params.set('secretaria', secretaria);
    const qs = params.toString();
    return peticion(`/expedientes${qs ? `?${qs}` : ''}`);
  },
  expediente: (id) => peticion(`/expedientes/${id}`),
  resumen: () => peticion('/expedientes/resumen'),
  crearExpediente: (datos) => peticion('/expedientes', { metodo: 'POST', datos }),
  actualizarExpediente: (id, datos) => peticion(`/expedientes/${id}`, { metodo: 'PUT', datos }),
  eliminarExpediente: (id) => peticion(`/expedientes/${id}`, { metodo: 'DELETE' }),

  avisoRespaldo: () => peticion('/respaldos/aviso'),
  estadoRespaldos: () => peticion('/respaldos/estado'),
  copiarRespaldo: (unidad) => peticion('/respaldos/copiar', { metodo: 'POST', datos: { unidad } }),

  direccionRed: () => peticion('/usuarios/direccion-red'),
  usuarios: () => peticion('/usuarios'),
  crearUsuario: (datos) => peticion('/usuarios', { metodo: 'POST', datos }),
  actualizarUsuario: (id, datos) => peticion(`/usuarios/${id}`, { metodo: 'PUT', datos }),
  eliminarUsuario: (id) => peticion(`/usuarios/${id}`, { metodo: 'DELETE' }),
};

export const STATUS = {
  ACTIVO: 'Activo',
  INACTIVO: 'Inactivo',
};

export const SECRETARIAS = [
  'Organización',
  'Finanzas',
  'Asuntos Legales',
  'Transparencia',
  'Fomento al Deporte',
  'Fomento al Ahorro',
  'Pensiones y Jubilaciones',
  'Oficialía Mayor',
  'Actas y Acuerdos',
  'De Interior',
  'Prensa y Propaganda',
  'Trabajo y Conflictos',
  'Patrimonio',
  'Fomento a la Vivienda',
  'Educación y Cultura',
  'Auditoría',
  'Acción Social',
];

export function iniciales(nombre = '') {
  return nombre
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0])
    .join('')
    .toUpperCase();
}

export function edad(fechaNacimiento) {
  if (!fechaNacimiento) return '';
  const [a, m, d] = fechaNacimiento.slice(0, 10).split('-').map(Number);
  const h = new Date();
  let anos = h.getFullYear() - a;
  if (h.getMonth() + 1 < m || (h.getMonth() + 1 === m && h.getDate() < d)) anos--;
  return anos >= 0 ? anos : '';
}

export function formatoFecha(iso) {
  if (!iso) return '';
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  return new Date(a, m - 1, d).toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' });
}

export function reducirImagen(archivo, maximo = 1280) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(archivo);
    const img = new Image();
    img.onload = () => {
      const escala = Math.min(1, maximo / Math.max(img.width, img.height));
      const lienzo = document.createElement('canvas');
      lienzo.width = Math.round(img.width * escala);
      lienzo.height = Math.round(img.height * escala);
      const ctx = lienzo.getContext('2d');
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, lienzo.width, lienzo.height);
      ctx.drawImage(img, 0, 0, lienzo.width, lienzo.height);
      URL.revokeObjectURL(url);
      resolve(lienzo.toDataURL('image/jpeg', 0.82));
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('No se pudo leer la imagen. Prueba con otra foto.'));
    };
    img.src = url;
  });
}

export function hoy() {
  const d = new Date();
  const dos = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}`;
}

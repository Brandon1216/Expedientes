const trazos = {
  carpeta: <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />,
  usuarios: (
    <>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3 19c.8-3.2 3.2-5 6-5s5.2 1.8 6 5" />
      <path d="M16 5.2a3 3 0 0 1 0 5.6M18 14.4c1.5.7 2.6 2.3 3 4.6" />
    </>
  ),
  mas: <path d="M12 5v14M5 12h14" />,
  buscar: (
    <>
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-4.2-4.2" />
    </>
  ),
  cerrar: <path d="M6 6l12 12M18 6 6 18" />,
  salir: (
    <>
      <path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4" />
      <path d="M10 16l-4-4 4-4M6 12h10" />
    </>
  ),
  llave: (
    <>
      <circle cx="8" cy="14" r="4" />
      <path d="M11 11l8-8M16 6l2 2M14 8l2 2" />
    </>
  ),
  basura: <path d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3" />,
  editar: <path d="M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  imprimir: (
    <>
      <path d="M7 9V4h10v5" />
      <path d="M7 17H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-2" />
      <path d="M7 14h10v6H7z" />
    </>
  ),
  usb: (
    <>
      <path d="M9 3h6v5H9z" />
      <path d="M7 8h10v8a4 4 0 0 1-4 4h-2a4 4 0 0 1-4-4z" />
      <path d="M11 5.2v.1M13 5.2v.1" />
    </>
  ),
  historial: (
    <>
      <path d="M4 12a8 8 0 1 0 2.3-5.6" />
      <path d="M4 4v3.5h3.5M12 8v4.5l3 1.8" />
    </>
  ),
  entrar: (
    <>
      <path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4" />
      <path d="M14 8l4 4-4 4M18 12H8" />
    </>
  ),
  abajo: <path d="m6 9 6 6 6-6" />,
  arriba: <path d="m6 15 6-6 6 6" />,
  flecha: <path d="M5 12h14M13 6l6 6-6 6" />,
  descargar: (
    <>
      <path d="M12 4v11M7 10l5 5 5-5" />
      <path d="M5 19h14" />
    </>
  ),
  check: <path d="m5 12.5 4.5 4.5L19 7.5" />,
  alerta: (
    <>
      <path d="M12 4 2.8 19.5h18.4z" />
      <path d="M12 10v4.5M12 17.2v.1" />
    </>
  ),
  camara: (
    <>
      <path d="M4 8a2 2 0 0 1 2-2h2l1.5-2h5L16 6h2a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z" />
      <circle cx="12" cy="12.5" r="3.5" />
    </>
  ),
};

export default function Icono({ nombre, tamano = 18 }) {
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {trazos[nombre]}
    </svg>
  );
}

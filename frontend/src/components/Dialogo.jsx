import { useEffect } from 'react';
import Icono from './Icono.jsx';

// Ventana centrada para formularios cortos y confirmaciones.
export default function Dialogo({ titulo, onCerrar, children, ancho = 420 }) {
  useEffect(() => {
    const tecla = (e) => e.key === 'Escape' && onCerrar();
    window.addEventListener('keydown', tecla);
    return () => window.removeEventListener('keydown', tecla);
  }, [onCerrar]);

  return (
    <div className="velo" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div className="dialogo" role="dialog" aria-modal="true" aria-label={titulo} style={{ maxWidth: ancho }}>
        <header className="dialogo-cabeza">
          <h2>{titulo}</h2>
          <button className="boton-icono" onClick={onCerrar} aria-label="Cerrar">
            <Icono nombre="cerrar" />
          </button>
        </header>
        {children}
      </div>
    </div>
  );
}

export function Confirmar({ titulo, mensaje, accion = 'Eliminar', onConfirmar, onCerrar }) {
  return (
    <Dialogo titulo={titulo} onCerrar={onCerrar} ancho={380}>
      <div className="dialogo-aviso">
        <span className="dialogo-icono">
          <Icono nombre="alerta" />
        </span>
        <p className="dialogo-texto">{mensaje}</p>
      </div>
      <div className="acciones">
        <button className="boton boton-suave" onClick={onCerrar}>
          Cancelar
        </button>
        <button className="boton boton-peligro" onClick={onConfirmar} autoFocus>
          {accion}
        </button>
      </div>
    </Dialogo>
  );
}

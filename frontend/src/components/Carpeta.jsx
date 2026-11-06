import { useEffect, useState } from 'react';
import { api, STATUS, formatoFecha } from '../api.js';
import Icono from './Icono.jsx';
import { Confirmar } from './Dialogo.jsx';
import Impresion from './Impresion.jsx';
import CamposExpediente, { valoresExpediente } from './CamposExpediente.jsx';

export default function Carpeta({ expediente, esAdmin, onCerrar, onGuardado }) {
  const [form, setForm] = useState(() => valoresExpediente(expediente));
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [foto, setFoto] = useState(undefined);

  useEffect(() => {
    api
      .expediente(expediente.id)
      .then((e) => setFoto(e.fotografia))
      .catch(() => setFoto(null));
  }, [expediente.id]);

  useEffect(() => {
    const tecla = (e) => e.key === 'Escape' && !confirmando && onCerrar();
    window.addEventListener('keydown', tecla);
    return () => window.removeEventListener('keydown', tecla);
  }, [onCerrar, confirmando]);

  const guardar = async (e) => {
    e.preventDefault();
    setError('');
    setGuardando(true);
    try {
      await api.actualizarExpediente(expediente.id, { ...form, actualizadoEn: expediente.actualizadoEn });
      onGuardado('Cambios guardados');
    } catch (err) {
      setError(err.message);
      setGuardando(false);
    }
  };

  const eliminar = async () => {
    try {
      await api.eliminarExpediente(expediente.id);
      onGuardado(`Expediente ${expediente.numero} eliminado`);
    } catch (err) {
      setError(err.message);
      setConfirmando(false);
    }
  };

  return (
    <div className="velo velo-lateral" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <aside className="carpeta" role="dialog" aria-modal="true" aria-label={expediente.numero}>
        <form className="carpeta-hoja" onSubmit={guardar}>
          <header className="carpeta-cabeza">
            <div>
              <div className="carpeta-folio">
                <span className="numero">{form.numero}</span>
                <span className={`estado estado-${form.status}`}>{STATUS[form.status]}</span>
              </div>
              <h2>{form.nombre || 'Expediente'}</h2>
            </div>
            <button type="button" className="boton-icono" onClick={onCerrar} aria-label="Cerrar">
              <Icono nombre="cerrar" />
            </button>
          </header>

          <dl className="metadatos">
            <div>
              <dt>Registrado por</dt>
              <dd>{expediente.creadoPorNombre || 'Usuario eliminado'}</dd>
            </div>
            <div>
              <dt>Última modificación</dt>
              <dd className="numero">{formatoFecha(expediente.actualizadoEn)}</dd>
            </div>
          </dl>

          <div className="formulario">
            <CamposExpediente form={form} setForm={setForm} foto={foto} setFoto={setFoto} setError={setError} />
            {error && <p className="error">{error}</p>}
          </div>

          <footer className="carpeta-pie">
            {esAdmin && (
              <button type="button" className="boton boton-texto-peligro" onClick={() => setConfirmando(true)}>
                <Icono nombre="basura" tamano={16} /> Eliminar
              </button>
            )}
            <span className="separador" />
            <button
              type="button"
              className="boton boton-suave"
              onClick={() => window.print()}
              disabled={foto === undefined}
              title="Imprime lo que se ve en el formulario"
            >
              <Icono nombre="imprimir" tamano={16} /> Imprimir
            </button>
            <button type="button" className="boton boton-suave" onClick={onCerrar}>
              Cancelar
            </button>
            <button className="boton boton-primario" disabled={guardando}>
              {guardando ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </footer>
        </form>
      </aside>

      <Impresion datos={form} foto={foto} />

      {confirmando && (
        <Confirmar
          titulo="Eliminar expediente"
          mensaje={`Se borrará ${expediente.numero} de forma permanente. Esta acción no se puede deshacer.`}
          onConfirmar={eliminar}
          onCerrar={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}

import { useState } from 'react';
import { api } from '../api.js';
import Dialogo from '../components/Dialogo.jsx';
import { useAvisos } from '../components/Avisos.jsx';

export default function CambiarPassword({ onCerrar }) {
  const avisar = useAvisos();
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [error, setError] = useState('');

  const guardar = async (e) => {
    e.preventDefault();
    if (nueva !== confirmacion) {
      setError('Las contraseñas nuevas no coinciden.');
      return;
    }
    try {
      await api.cambiarPassword(actual, nueva);
      avisar('Contraseña actualizada');
      onCerrar();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Dialogo titulo="Cambiar contraseña" onCerrar={onCerrar}>
      <form onSubmit={guardar} className="formulario">
        <label className="campo">
          <span>Contraseña actual</span>
          <input type="password" value={actual} onChange={(e) => setActual(e.target.value)} required autoFocus />
        </label>
        <label className="campo">
          <span>Contraseña nueva</span>
          <input type="password" value={nueva} onChange={(e) => setNueva(e.target.value)} minLength={6} required />
          <small>Mínimo 6 caracteres.</small>
        </label>
        <label className="campo">
          <span>Repite la contraseña nueva</span>
          <input type="password" value={confirmacion} onChange={(e) => setConfirmacion(e.target.value)} required />
        </label>
        {error && <p className="error">{error}</p>}
        <div className="acciones">
          <button type="button" className="boton boton-suave" onClick={onCerrar}>
            Cancelar
          </button>
          <button className="boton boton-primario">Guardar contraseña</button>
        </div>
      </form>
    </Dialogo>
  );
}

import { useState } from 'react';
import { api } from '../api.js';
import CampoClave from '../components/CampoClave.jsx';
import { MarcaLogin } from './Login.jsx';

// Pantalla que no se puede saltar: quien entra con la contraseña inicial debe poner una propia.
export default function PasswordObligatoria({ onListo, onSalir }) {
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const guardar = async (e) => {
    e.preventDefault();
    setError('');
    if (nueva !== confirmacion) {
      setError('Las contraseñas nuevas no coinciden.');
      return;
    }
    setEnviando(true);
    try {
      await api.cambiarPassword(actual, nueva);
      onListo();
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <main className="login">
      <MarcaLogin />

      <form className="login-panel" onSubmit={guardar} autoComplete="off">
        <div className="login-cuerpo">
          <div>
            <h1>Crea tu contraseña</h1>
            <p className="login-sub">Estás usando la contraseña inicial. Cámbiala para poder continuar.</p>
          </div>

          <label className="campo">
            <span>Contraseña actual</span>
            <CampoClave campo="actual" value={actual} onChange={(e) => setActual(e.target.value)} autoFocus required />
          </label>

          <label className="campo">
            <span>Contraseña nueva</span>
            <CampoClave
              campo="nueva"
              value={nueva}
              onChange={(e) => setNueva(e.target.value)}
              minLength={6}
              maxLength={72}
              required
            />
            <small>Mínimo 6 caracteres.</small>
          </label>

          <label className="campo">
            <span>Repite la contraseña nueva</span>
            <CampoClave
              campo="repetir"
              value={confirmacion}
              onChange={(e) => setConfirmacion(e.target.value)}
              required
            />
          </label>

          {error && <p className="error">{error}</p>}

          <button className="boton boton-primario boton-ancho" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar y continuar'}
          </button>
          <button type="button" className="boton boton-suave boton-ancho" onClick={onSalir}>
            Cerrar sesión
          </button>
        </div>
      </form>
    </main>
  );
}

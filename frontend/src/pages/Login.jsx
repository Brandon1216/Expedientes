import { useState } from 'react';
import { api } from '../api.js';
import CampoClave from '../components/CampoClave.jsx';

// Panel con el escudo, compartido con la pantalla de contraseña nueva.
export function MarcaLogin() {
  return (
    <aside className="login-marca">
      <img className="login-logo" src="/logo.png" alt="S.U.T.S.P.E.M.I.D.B.C." />
      <strong>Sistema de Expedientes</strong>
      <span>S.U.T.S.P.E.M.I.D.B.C.</span>
      <em>Por un Estado al servicio del pueblo</em>
    </aside>
  );
}

export default function Login({ onEntrar }) {
  const [usuario, setUsuario] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e) => {
    e.preventDefault();
    setError('');
    setEnviando(true);
    try {
      onEntrar(await api.login(usuario, password));
    } catch (err) {
      setError(err.message);
      setEnviando(false);
    }
  };

  return (
    <main className="login">
      <MarcaLogin />

      <form className="login-panel" onSubmit={enviar} autoComplete="off">
        <div className="login-cuerpo">
          <div>
            <h1>Inicia sesión</h1>
            <p className="login-sub">Entra con el usuario que te asignó el administrador.</p>
          </div>

          <label className="campo">
            <span>Usuario</span>
            <input
              value={usuario}
              onChange={(e) => setUsuario(e.target.value)}
              data-campo="usuario"
              autoComplete="off"
              autoCorrect="off"
              autoCapitalize="off"
              spellCheck={false}
              autoFocus
              required
            />
          </label>

          <label className="campo">
            <span>Contraseña</span>
            <CampoClave campo="clave" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>

          {error && <p className="error">{error}</p>}

          <button className="boton boton-primario boton-ancho" disabled={enviando}>
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
        </div>
      </form>
    </main>
  );
}

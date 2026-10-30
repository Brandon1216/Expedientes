import { useState } from 'react';
import { api } from '../api.js';

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
      <form className="login-carpeta" onSubmit={enviar}>
        <img className="login-logo" src="/logo.png" alt="S.U.T.S.P.E.M.I.D.B.C." />
        <div className="login-pestana">Expedientes</div>
        <div className="login-cuerpo">
          <h1>Inicia sesión</h1>
          <p className="login-sub">Entra con el usuario que te asignó el administrador.</p>

          <label className="campo">
            <span>Usuario</span>
            <input
              value={usuario}
              onChange={(e) => setUsuario(e.target.value)}
              autoComplete="username"
              autoFocus
              required
            />
          </label>

          <label className="campo">
            <span>Contraseña</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
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

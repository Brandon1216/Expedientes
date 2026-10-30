import { useEffect, useState } from 'react';
import { api, sesion } from './api.js';
import { AvisosProvider } from './components/Avisos.jsx';
import Marco from './components/Marco.jsx';
import Login from './pages/Login.jsx';
import Expedientes from './pages/Expedientes.jsx';
import Usuarios from './pages/Usuarios.jsx';

export default function App() {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(Boolean(sesion.token()));
  const [vista, setVista] = useState('expedientes');

  // Si hay un token guardado, recupera la sesión al abrir la página
  useEffect(() => {
    if (!sesion.token()) return;
    api
      .yo()
      .then(setUsuario)
      .catch(() => sesion.borrar())
      .finally(() => setCargando(false));
  }, []);

  useEffect(() => {
    const expirada = () => setUsuario(null);
    window.addEventListener('sesion-expirada', expirada);
    return () => window.removeEventListener('sesion-expirada', expirada);
  }, []);

  const entrar = ({ token, usuario }) => {
    sesion.guardar(token);
    setUsuario(usuario);
    setVista('expedientes');
  };

  const salir = () => {
    sesion.borrar();
    setUsuario(null);
  };

  if (cargando) return <div className="pantalla-carga" />;

  return (
    <AvisosProvider>
      {!usuario ? (
        <Login onEntrar={entrar} />
      ) : (
        <Marco usuario={usuario} vista={vista} onVista={setVista} onSalir={salir}>
          {vista === 'usuarios' && usuario.rol === 'ADMIN' ? (
            <Usuarios yo={usuario} />
          ) : (
            <Expedientes usuario={usuario} />
          )}
        </Marco>
      )}
    </AvisosProvider>
  );
}

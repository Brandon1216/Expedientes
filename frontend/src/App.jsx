import { useCallback, useEffect, useState } from 'react';
import { api, sesion } from './api.js';
import { AvisosProvider } from './components/Avisos.jsx';
import Marco from './components/Marco.jsx';
import Login from './pages/Login.jsx';
import PasswordObligatoria from './pages/PasswordObligatoria.jsx';
import Expedientes from './pages/Expedientes.jsx';
import Auditoria from './pages/Auditoria.jsx';
import NuevoExpediente from './pages/NuevoExpediente.jsx';
import Respaldos from './pages/Respaldos.jsx';
import Usuarios from './pages/Usuarios.jsx';

export default function App() {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(Boolean(sesion.token()));
  const [vista, setVista] = useState('expedientes');
  const [borrador, setBorrador] = useState(null);
  const [abrirId, setAbrirId] = useState(null);
  const limpiarAbrir = useCallback(() => setAbrirId(null), []);

  useEffect(() => {
    if (!sesion.token()) return;
    api
      .yo()
      .then(setUsuario)
      .catch(() => sesion.borrar())
      .finally(() => setCargando(false));
  }, []);

  useEffect(() => {
    const expirada = () => {
      setUsuario(null);
      setBorrador(null);
    };
    window.addEventListener('sesion-expirada', expirada);
    return () => window.removeEventListener('sesion-expirada', expirada);
  }, []);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [vista]);

  const terminarNuevo = useCallback(() => {
    setVista('expedientes');
    setBorrador(null);
  }, []);

  const entrar = ({ token, usuario }) => {
    sesion.guardar(token);
    setUsuario(usuario);
    setVista('expedientes');
  };

  const salir = async () => {
    if (sesion.token()) await api.salir().catch(() => {});
    sesion.borrar();
    setUsuario(null);
    setBorrador(null);
  };

  if (cargando) return <div className="pantalla-carga" />;

  return (
    <AvisosProvider>
      {!usuario ? (
        <Login onEntrar={entrar} />
      ) : usuario.debeCambiarPassword ? (
        <PasswordObligatoria onListo={() => setUsuario({ ...usuario, debeCambiarPassword: false })} onSalir={salir} />
      ) : (
        <Marco usuario={usuario} vista={vista} onVista={setVista} onSalir={salir}>
          {vista === 'usuarios' && usuario.rol === 'ADMIN' ? (
            <Usuarios yo={usuario} />
          ) : vista === 'respaldos' && usuario.rol === 'ADMIN' ? (
            <Respaldos />
          ) : vista === 'auditoria' && usuario.rol === 'ADMIN' ? (
            <Auditoria
              onAbrirExpediente={(id) => {
                setAbrirId(id);
                setVista('expedientes');
              }}
            />
          ) : vista === 'nuevo' ? (
            <NuevoExpediente borrador={borrador} onBorrador={setBorrador} onTerminar={terminarNuevo} />
          ) : (
            <Expedientes
              usuario={usuario}
              onNuevo={() => setVista('nuevo')}
              abrirId={abrirId}
              onAbierto={limpiarAbrir}
            />
          )}
        </Marco>
      )}
    </AvisosProvider>
  );
}

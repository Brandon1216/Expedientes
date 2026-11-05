import { useEffect, useState } from 'react';
import { api, iniciales } from '../api.js';
import Icono from './Icono.jsx';
import CambiarPassword from '../pages/CambiarPassword.jsx';

export default function Marco({ usuario, vista, onVista, onSalir, children }) {
  const [cambiandoPassword, setCambiandoPassword] = useState(false);
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [direccion, setDireccion] = useState('');
  const esAdmin = usuario.rol === 'ADMIN';

  const [avisoRespaldo, setAvisoRespaldo] = useState(null);

  useEffect(() => {
    if (!esAdmin) return;
    api
      .direccionRed()
      .then((r) => setDireccion(r.direccion))
      .catch(() => setDireccion(''));
  }, [esAdmin]);

  useEffect(() => {
    if (!esAdmin) return undefined;
    const revisar = () =>
      api
        .avisoRespaldo()
        .then(setAvisoRespaldo)
        .catch(() => setAvisoRespaldo(null));
    revisar();
    window.addEventListener('respaldo-copiado', revisar);
    return () => window.removeEventListener('respaldo-copiado', revisar);
  }, [esAdmin]);

  const ir = (v) => {
    onVista(v);
    setMenuAbierto(false);
  };

  return (
    <div className="marco">
      <aside className={`lateral ${menuAbierto ? 'abierto' : ''}`}>
        <div className="marca">
          <img className="marca-logo" src="/logo.png" alt="" />
          <div className="marca-texto">
            Expedientes
            <small>S.U.T.S.P.E.M.I.D.B.C.</small>
          </div>
          <button className="boton-icono solo-movil" onClick={() => setMenuAbierto(!menuAbierto)} aria-label="Menú">
            <Icono nombre="menu" />
          </button>
        </div>

        <nav className="nav">
          <button className={vista === 'expedientes' ? 'activo' : ''} onClick={() => ir('expedientes')}>
            <Icono nombre="carpeta" /> Expedientes
          </button>
          <button className={vista === 'nuevo' ? 'activo' : ''} onClick={() => ir('nuevo')}>
            <Icono nombre="mas" /> Nuevo expediente
          </button>
          {esAdmin && (
            <button className={vista === 'usuarios' ? 'activo' : ''} onClick={() => ir('usuarios')}>
              <Icono nombre="usuarios" /> Usuarios
            </button>
          )}
          {esAdmin && (
            <button className={vista === 'respaldos' ? 'activo' : ''} onClick={() => ir('respaldos')}>
              <Icono nombre="usb" /> Respaldos
            </button>
          )}
        </nav>

        <div className="perfil">
          {avisoRespaldo?.avisar && (
            <button className="aviso-respaldo" onClick={() => ir('respaldos')}>
              <Icono nombre="alerta" tamano={15} />
              {avisoRespaldo.dias === null
                ? 'Aún no has copiado un respaldo a USB'
                : `Hace ${avisoRespaldo.dias} días que no copias un respaldo`}
            </button>
          )}
          {direccion && (
            <div className="direccion-red">
              <small>Para entrar desde otro equipo</small>
              <strong className="numero">{direccion}</strong>
            </div>
          )}
          <div className="perfil-datos">
            <span className="avatar">{iniciales(usuario.nombre)}</span>
            <div>
              <strong>{usuario.nombre}</strong>
              <small>{esAdmin ? 'Administrador' : 'Usuario'}</small>
            </div>
          </div>
          <button className="perfil-accion" onClick={() => setCambiandoPassword(true)}>
            <Icono nombre="llave" tamano={16} /> Cambiar contraseña
          </button>
          <button className="perfil-accion" onClick={onSalir}>
            <Icono nombre="salir" tamano={16} /> Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="contenido">{children}</main>

      {cambiandoPassword && <CambiarPassword onCerrar={() => setCambiandoPassword(false)} />}
    </div>
  );
}

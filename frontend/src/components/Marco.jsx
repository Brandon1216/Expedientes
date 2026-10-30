import { useState } from 'react';
import Icono from './Icono.jsx';
import CambiarPassword from '../pages/CambiarPassword.jsx';

// Estructura general: barra lateral + contenido.
export default function Marco({ usuario, vista, onVista, onSalir, children }) {
  const [cambiandoPassword, setCambiandoPassword] = useState(false);
  const [menuAbierto, setMenuAbierto] = useState(false);
  const esAdmin = usuario.rol === 'ADMIN';

  const ir = (v) => {
    onVista(v);
    setMenuAbierto(false);
  };

  const iniciales = usuario.nombre
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0])
    .join('')
    .toUpperCase();

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
          {esAdmin && (
            <button className={vista === 'usuarios' ? 'activo' : ''} onClick={() => ir('usuarios')}>
              <Icono nombre="usuarios" /> Usuarios
            </button>
          )}
        </nav>

        <div className="perfil">
          <div className="perfil-datos">
            <span className="avatar">{iniciales}</span>
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

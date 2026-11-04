import { useCallback, useEffect, useState } from 'react';
import { api, formatoFecha, iniciales } from '../api.js';
import CampoClave from '../components/CampoClave.jsx';
import Icono from '../components/Icono.jsx';
import Dialogo, { Confirmar } from '../components/Dialogo.jsx';
import { useAvisos } from '../components/Avisos.jsx';

export default function Usuarios({ yo }) {
  const avisar = useAvisos();
  const [lista, setLista] = useState([]);
  const [editando, setEditando] = useState(null); // usuario, o {} para uno nuevo
  const [eliminando, setEliminando] = useState(null);

  const cargar = useCallback(() => {
    api.usuarios().then(setLista).catch((e) => avisar(e.message, 'error'));
  }, [avisar]);

  useEffect(cargar, [cargar]);

  const eliminar = async () => {
    try {
      await api.eliminarUsuario(eliminando.id);
      avisar(`Usuario ${eliminando.usuario} eliminado`);
      cargar();
    } catch (e) {
      avisar(e.message, 'error');
    }
    setEliminando(null);
  };

  return (
    <section className="pagina">
      <header className="pagina-cabeza">
        <div>
          <h1>Usuarios</h1>
          <p className="pagina-sub">Quién puede entrar al sistema y con qué permisos.</p>
        </div>
        <button className="boton boton-primario" onClick={() => setEditando({})}>
          <Icono nombre="mas" tamano={16} /> Nuevo usuario
        </button>
      </header>

      <div className="tabla-marco">
        <div className="tabla-cuerpo">
        <table className="tabla tabla-fija">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Usuario</th>
              <th>Rol</th>
              <th className="col-fecha">Alta</th>
              <th aria-label="Acciones" />
            </tr>
          </thead>
          <tbody>
            {lista.map((u) => (
              <tr key={u.id} className={u.activo ? '' : 'inactivo'}>
                <td>
                  <div className="persona">
                    <span className="avatar">{iniciales(u.nombre)}</span>
                    <div className="titular">
                      {u.nombre}
                      {u.id === yo.id && <span className="tu">tú</span>}
                    </div>
                  </div>
                </td>
                <td className="atenuado">{u.usuario}</td>
                <td>
                  <span className={`rol rol-${u.rol}`}>{u.rol === 'ADMIN' ? 'Administrador' : 'Usuario'}</span>
                  {!u.activo && <span className="rol rol-inactivo">Desactivado</span>}
                </td>
                <td className="col-fecha atenuado numero">{formatoFecha(u.creadoEn)}</td>
                <td className="celda-acciones">
                  <button className="boton-icono" onClick={() => setEditando(u)} aria-label={`Editar ${u.nombre}`}>
                    <Icono nombre="editar" tamano={16} />
                  </button>
                  {u.id !== yo.id && (
                    <button
                      className="boton-icono peligro"
                      onClick={() => setEliminando(u)}
                      aria-label={`Eliminar ${u.nombre}`}
                    >
                      <Icono nombre="basura" tamano={16} />
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
      </div>

      {editando && (
        <FormularioUsuario
          usuario={editando}
          esYo={editando.id === yo.id}
          onCerrar={() => setEditando(null)}
          onGuardado={(m) => {
            avisar(m);
            setEditando(null);
            cargar();
          }}
        />
      )}

      {eliminando && (
        <Confirmar
          titulo="Eliminar usuario"
          mensaje={`${eliminando.nombre} ya no podrá entrar. Sus expedientes se conservan. Si solo quieres bloquear el acceso temporalmente, mejor desactívalo.`}
          onConfirmar={eliminar}
          onCerrar={() => setEliminando(null)}
        />
      )}
    </section>
  );
}

function FormularioUsuario({ usuario, esYo, onCerrar, onGuardado }) {
  const nuevo = !usuario.id;
  const [form, setForm] = useState({
    nombre: usuario.nombre || '',
    usuario: usuario.usuario || '',
    password: '',
    rol: usuario.rol || 'USUARIO',
    activo: usuario.activo ?? true,
  });
  const [error, setError] = useState('');

  const cambiar = (campo) => (e) =>
    setForm({ ...form, [campo]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const guardar = async (e) => {
    e.preventDefault();
    setError('');
    try {
      if (nuevo) {
        await api.crearUsuario(form);
        onGuardado(`Usuario ${form.usuario} creado`);
      } else {
        await api.actualizarUsuario(usuario.id, form);
        onGuardado('Usuario actualizado');
      }
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <Dialogo titulo={nuevo ? 'Nuevo usuario' : `Editar a ${usuario.nombre}`} onCerrar={onCerrar}>
      <form className="formulario" onSubmit={guardar} autoComplete="off">
        <label className="campo">
          <span>Nombre completo</span>
          <input value={form.nombre} onChange={cambiar('nombre')} autoComplete="off" required autoFocus />
        </label>

        <label className="campo">
          <span>Usuario para entrar</span>
          <input
            value={form.usuario}
            onChange={cambiar('usuario')}
            autoComplete="off"
            required
            disabled={!nuevo}
            pattern="[A-Za-z0-9._\-]+"
            title="Letras, números, punto, guion o guion bajo"
          />
          {nuevo && <small>Sin espacios. Ej. maria.lopez</small>}
        </label>

        <label className="campo">
          <span>{nuevo ? 'Contraseña' : 'Contraseña nueva'}</span>
          <CampoClave
            campo="clave-usuario"
            value={form.password}
            onChange={cambiar('password')}
            minLength={6}
            required={nuevo}
          />
          {!nuevo && <small>Déjala vacía para conservar la actual.</small>}
        </label>

        <fieldset className="campo">
          <span>Rol</span>
          <div className="selector-estado">
            {[
              ['USUARIO', 'Usuario'],
              ['ADMIN', 'Administrador'],
            ].map(([valor, etiqueta]) => (
              <label key={valor} className={form.rol === valor ? 'elegido' : ''}>
                <input type="radio" name="rol" value={valor} checked={form.rol === valor} onChange={cambiar('rol')} />
                {etiqueta}
              </label>
            ))}
          </div>
          <small>
            {form.rol === 'ADMIN'
              ? 'Puede administrar usuarios y eliminar expedientes.'
              : 'Puede consultar, crear y editar expedientes.'}
          </small>
        </fieldset>

        {!nuevo && !esYo && (
          <label className="interruptor">
            <input type="checkbox" checked={form.activo} onChange={cambiar('activo')} />
            <span>Puede iniciar sesión</span>
          </label>
        )}

        {error && <p className="error">{error}</p>}

        <div className="acciones">
          <button type="button" className="boton boton-suave" onClick={onCerrar}>
            Cancelar
          </button>
          <button className="boton boton-primario">{nuevo ? 'Crear usuario' : 'Guardar cambios'}</button>
        </div>
      </form>
    </Dialogo>
  );
}

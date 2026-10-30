import { useEffect, useRef, useState } from 'react';
import { api, SECRETARIAS, STATUS, edad, formatoFecha, hoy, reducirImagen } from '../api.js';
import Icono from './Icono.jsx';
import { Confirmar } from './Dialogo.jsx';
import Impresion from './Impresion.jsx';

// Panel lateral con forma de carpeta: muestra y edita un expediente.
export default function Carpeta({ expediente, esAdmin, onCerrar, onGuardado }) {
  const nuevo = !expediente.id;
  const [form, setForm] = useState({
    numero: expediente.numero || '',
    status: expediente.status || 'ACTIVO',
    fechaIngreso: expediente.fechaIngreso || hoy(),
    nombre: expediente.nombre || '',
    fechaNacimiento: expediente.fechaNacimiento || '',
    solicitud: expediente.solicitud || '',
    secretaria: expediente.secretaria || '',
    perfil: expediente.perfil || '',
    telefono: expediente.telefono || '',
    familiarSindicalizado: expediente.familiarSindicalizado || '',
    parentesco: expediente.parentesco || '',
    dependenciaFamiliar: expediente.dependenciaFamiliar || '',
    notas: expediente.notas || '',
  });
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  // Foto que se muestra: undefined mientras carga, null si no tiene.
  // El listado no trae las fotos, así que se pide el expediente completo al abrirlo.
  const [foto, setFoto] = useState(nuevo ? null : undefined);
  const entradaFoto = useRef(null);

  useEffect(() => {
    if (nuevo) return;
    api
      .expediente(expediente.id)
      .then((e) => setFoto(e.fotografia))
      .catch(() => setFoto(null));
  }, [nuevo, expediente.id]);

  // form.fotografia solo se envía si cambió: data URL nueva, o '' para quitarla
  const elegirFoto = async (e) => {
    const archivo = e.target.files[0];
    e.target.value = '';
    if (!archivo) return;
    try {
      const imagen = await reducirImagen(archivo);
      setFoto(imagen);
      setForm((f) => ({ ...f, fotografia: imagen }));
    } catch (err) {
      setError(err.message);
    }
  };

  const quitarFoto = () => {
    setFoto(null);
    setForm((f) => ({ ...f, fotografia: '' }));
  };

  useEffect(() => {
    const tecla = (e) => e.key === 'Escape' && !confirmando && onCerrar();
    window.addEventListener('keydown', tecla);
    return () => window.removeEventListener('keydown', tecla);
  }, [onCerrar, confirmando]);

  const cambiar = (campo) => (e) => setForm({ ...form, [campo]: e.target.value });

  const guardar = async (e) => {
    e.preventDefault();
    setError('');
    setGuardando(true);
    try {
      if (nuevo) {
        const creado = await api.crearExpediente(form);
        onGuardado(`Expediente ${creado.numero} creado`);
      } else {
        await api.actualizarExpediente(expediente.id, form);
        onGuardado('Cambios guardados');
      }
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
      <aside className="carpeta" role="dialog" aria-modal="true" aria-label={nuevo ? 'Nuevo expediente' : expediente.numero}>
        <div className="carpeta-pestana numero">{nuevo ? 'Nuevo' : expediente.numero}</div>

        <form className="carpeta-hoja" onSubmit={guardar}>
          <header className="carpeta-cabeza">
            <h2>{nuevo ? 'Nuevo expediente' : form.nombre || 'Expediente'}</h2>
            <button type="button" className="boton-icono" onClick={onCerrar} aria-label="Cerrar">
              <Icono nombre="cerrar" />
            </button>
          </header>

          {!nuevo && (
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
          )}

          <div className="formulario">
            <div className="campos-fila">
              <label className="campo">
                <span>Número</span>
                <input value={form.numero} onChange={cambiar('numero')} maxLength={30} required autoFocus={nuevo} />
              </label>

              <fieldset className="campo">
                <span>Status</span>
                <div className="selector-estado">
                  {Object.entries(STATUS).map(([valor, etiqueta]) => (
                    <label key={valor} className={form.status === valor ? `elegido estado-${valor}` : ''}>
                      <input
                        type="radio"
                        name="status"
                        value={valor}
                        checked={form.status === valor}
                        onChange={cambiar('status')}
                      />
                      {etiqueta}
                    </label>
                  ))}
                </div>
              </fieldset>
            </div>

            <label className="campo">
              <span>Fecha de ingreso</span>
              <input type="date" value={form.fechaIngreso} onChange={cambiar('fechaIngreso')} required />
            </label>

            <label className="campo">
              <span>Nombre y apellido</span>
              <input value={form.nombre} onChange={cambiar('nombre')} maxLength={150} required />
            </label>

            <div className="campos-fila">
              <label className="campo">
                <span>Fecha de nacimiento</span>
                <input type="date" value={form.fechaNacimiento} onChange={cambiar('fechaNacimiento')} max={hoy()} />
              </label>

              <div className="campo">
                <span>Edad</span>
                <input value={form.fechaNacimiento ? `${edad(form.fechaNacimiento)} años` : '—'} readOnly tabIndex={-1} />
              </div>
            </div>

            <label className="campo">
              <span>Solicitud</span>
              <input value={form.solicitud} onChange={cambiar('solicitud')} maxLength={300} />
            </label>

            <div className="campos-fila">
              <label className="campo">
                <span>Secretaría asignada</span>
                <select value={form.secretaria} onChange={cambiar('secretaria')}>
                  <option value="">Sin asignar</option>
                  {SECRETARIAS.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                  {/* Conserva un valor anterior que ya no esté en la lista */}
                  {form.secretaria && !SECRETARIAS.includes(form.secretaria) && (
                    <option value={form.secretaria}>{form.secretaria}</option>
                  )}
                </select>
              </label>

              <label className="campo">
                <span>Perfil</span>
                <input value={form.perfil} onChange={cambiar('perfil')} maxLength={150} />
              </label>
            </div>

            <label className="campo">
              <span>Número de teléfono</span>
              <input type="tel" value={form.telefono} onChange={cambiar('telefono')} maxLength={30} />
            </label>

            <label className="campo">
              <span>Familiar sindicalizado</span>
              <input
                value={form.familiarSindicalizado}
                onChange={cambiar('familiarSindicalizado')}
                maxLength={150}
                placeholder="Nombre del familiar"
              />
            </label>

            <div className="campos-fila">
              <label className="campo">
                <span>Parentesco con el familiar</span>
                <input value={form.parentesco} onChange={cambiar('parentesco')} maxLength={60} />
              </label>

              <label className="campo">
                <span>Dependencia del familiar</span>
                <input value={form.dependenciaFamiliar} onChange={cambiar('dependenciaFamiliar')} maxLength={150} />
              </label>
            </div>

            <div className="campo">
              <span>Foto de la persona</span>
              <div className="foto">
                {foto ? (
                  <img src={foto} alt="Foto de la persona" />
                ) : (
                  <div className="foto-vacia">
                    {foto === undefined ? (
                      'Cargando…'
                    ) : (
                      <>
                        <Icono nombre="camara" tamano={26} />
                        Sin fotografía
                      </>
                    )}
                  </div>
                )}
                <div className="foto-acciones">
                  <button
                    type="button"
                    className="boton boton-suave"
                    onClick={() => entradaFoto.current.click()}
                    disabled={foto === undefined}
                  >
                    <Icono nombre="camara" tamano={16} /> {foto ? 'Cambiar' : 'Agregar fotografía'}
                  </button>
                  {foto && (
                    <button type="button" className="boton boton-texto-peligro" onClick={quitarFoto}>
                      Quitar
                    </button>
                  )}
                  <input ref={entradaFoto} type="file" accept="image/*" onChange={elegirFoto} hidden />
                </div>
              </div>
            </div>

            <label className="campo">
              <span>Notas</span>
              <textarea value={form.notas} onChange={cambiar('notas')} rows={6} maxLength={5000} />
            </label>

            {error && <p className="error">{error}</p>}
          </div>

          <footer className="carpeta-pie">
            {!nuevo && esAdmin && (
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
              {guardando ? 'Guardando…' : nuevo ? 'Crear expediente' : 'Guardar cambios'}
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

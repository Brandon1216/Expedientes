import { useRef } from 'react';
import { SECRETARIAS, STATUS, edad, hoy, reducirImagen } from '../api.js';
import Icono from './Icono.jsx';

// Valores del formulario a partir de un expediente, o vacíos para uno nuevo.
export function valoresExpediente(expediente = {}) {
  return {
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
  };
}

// Secciones del expediente: las usan la carpeta (editar) y la página de registro (nuevo).
export default function CamposExpediente({ form, setForm, foto, setFoto, setError, enfocar }) {
  const entradaFoto = useRef(null);

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }));

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

  return (
    <>
      <section className="seccion">
        <h3>Registro</h3>
        <div className="campos-tres">
          <label className="campo">
            <span>Número</span>
            <input value={form.numero} onChange={cambiar('numero')} maxLength={30} required autoFocus={enfocar} />
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

          <label className="campo">
            <span>Fecha de ingreso</span>
            <input type="date" value={form.fechaIngreso} onChange={cambiar('fechaIngreso')} required />
          </label>
        </div>
      </section>

      <section className="seccion">
        <h3>Datos personales</h3>
        <div className="personales">
          <div className="personales-campos">
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
              <span>Número de teléfono</span>
              <input type="tel" value={form.telefono} onChange={cambiar('telefono')} maxLength={30} />
            </label>
          </div>

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
                className="boton boton-suave boton-chico"
                onClick={() => entradaFoto.current.click()}
                disabled={foto === undefined}
              >
                <Icono nombre="camara" tamano={15} /> {foto ? 'Cambiar' : 'Agregar'}
              </button>
              {foto && (
                <button type="button" className="boton boton-texto-peligro boton-chico" onClick={quitarFoto}>
                  Quitar
                </button>
              )}
              <input ref={entradaFoto} type="file" accept="image/*" onChange={elegirFoto} hidden />
            </div>
          </div>
        </div>
      </section>

      <section className="seccion">
        <h3>Solicitud</h3>
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
      </section>

      <section className="seccion">
        <h3>Familiar sindicalizado</h3>
        <label className="campo">
          <span>Nombre del familiar</span>
          <input value={form.familiarSindicalizado} onChange={cambiar('familiarSindicalizado')} maxLength={150} />
        </label>

        <div className="campos-fila">
          <label className="campo">
            <span>Parentesco</span>
            <input value={form.parentesco} onChange={cambiar('parentesco')} maxLength={60} />
          </label>

          <label className="campo">
            <span>Dependencia</span>
            <input value={form.dependenciaFamiliar} onChange={cambiar('dependenciaFamiliar')} maxLength={150} />
          </label>
        </div>
      </section>

      <section className="seccion">
        <h3>Notas</h3>
        <label className="campo">
          <span className="solo-lector">Notas</span>
          <textarea value={form.notas} onChange={cambiar('notas')} rows={5} maxLength={5000} />
        </label>
      </section>
    </>
  );
}

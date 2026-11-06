import { Fragment, useState } from 'react';
import Icono from '../Icono.jsx';
import { Accion, Equipo, Persona } from './Partes.jsx';
import {
  ACCIONES_EXPEDIENTE,
  agruparPorDia,
  cambiosDe,
  fechaLarga,
  haceCuanto,
  hora,
  partesObjeto,
} from './formato.js';

function Expediente({ m, onAbrir }) {
  const { numero, resto } = partesObjeto(m.objeto);
  const enlazable = m.expedienteId && m.expedienteExiste;
  return (
    <span className="aud-expediente">
      {enlazable ? (
        <button type="button" className="aud-enlace" onClick={() => onAbrir(m.expedienteId)}>
          {numero}
        </button>
      ) : (
        <span className="aud-numero">{numero}</span>
      )}
      {resto && <span> · {resto}</span>}
      {m.expedienteId && !m.expedienteExiste && <span className="aud-eliminado"> (eliminado)</span>}
    </span>
  );
}

function QuePaso({ m, onAbrir, abierto, onAlternar }) {
  if (ACCIONES_EXPEDIENTE.includes(m.accion)) {
    const cambios = m.accion === 'EDITAR_EXPEDIENTE' ? cambiosDe(m) : null;
    return (
      <>
        <Expediente m={m} onAbrir={onAbrir} />
        {m.accion === 'EDITAR_EXPEDIENTE' &&
          (cambios && cambios.length > 0 ? (
            <button
              type="button"
              className="aud-ver-cambios"
              aria-expanded={abierto}
              aria-controls={`cambios-${m.id}`}
              onClick={onAlternar}
            >
              <Icono nombre={abierto ? 'arriba' : 'abajo'} tamano={14} />
              {cambios.length === 1 ? '1 campo' : `${cambios.length} campos`}: {cambios.map((c) => c.campo).join(', ')}
            </button>
          ) : (
            <small className="aud-secundario">{cambios ? 'Guardó sin cambios' : m.detalle}</small>
          ))}
      </>
    );
  }
  const partes = [m.objeto, m.detalle].filter(Boolean);
  if (partes.length === 0) return <span className="aud-secundario">—</span>;
  return (
    <>
      {m.objeto && <span>{m.objeto}</span>}
      {m.detalle && <small className="aud-secundario">{m.detalle}</small>}
    </>
  );
}

function Diferencias({ m }) {
  const cambios = cambiosDe(m) || [];
  return (
    <dl className="aud-diferencias" id={`cambios-${m.id}`}>
      {cambios.map((c, i) => (
        <div key={i}>
          <dt>{c.campo}</dt>
          <dd>
            {c.antes === null && c.despues === null ? (
              <span className="aud-secundario">Modificado (sin detalle guardado)</span>
            ) : (
              <>
                <del>{c.antes ?? '(vacío)'}</del>
                <Icono nombre="flecha" tamano={14} />
                <ins>{c.despues ?? '(vacío)'}</ins>
              </>
            )}
          </dd>
        </div>
      ))}
    </dl>
  );
}

export default function TablaMovimientos({ filas, onAbrirExpediente }) {
  const [abiertos, setAbiertos] = useState(() => new Set());
  const ahora = new Date();
  const alternar = (id) =>
    setAbiertos((a) => {
      const n = new Set(a);
      if (n.has(id)) n.delete(id);
      else n.add(id);
      return n;
    });

  return (
    <table className="tabla aud-tabla">
      <thead>
        <tr>
          <th scope="col">Hora</th>
          <th scope="col">Usuario</th>
          <th scope="col">Acción</th>
          <th scope="col">Qué pasó</th>
          <th scope="col">Equipo</th>
        </tr>
      </thead>
      {agruparPorDia(filas, 'fecha').map((g) => (
        <tbody key={g.dia}>
          <tr className="aud-dia">
            <th colSpan={5} scope="colgroup">
              {g.etiqueta}
            </th>
          </tr>
          {g.filas.map((m) => {
            const abierto = abiertos.has(m.id);
            return (
              <Fragment key={m.id}>
                <tr className={`aud-fila aud-fila-${m.accion === 'ELIMINAR_EXPEDIENTE' ? 'critica' : 'normal'}`}>
                  <td data-etiqueta="Hora" className="aud-col-hora">
                    <time dateTime={m.fecha.replace(' ', 'T')} title={`${fechaLarga(m.fecha)}, ${hora(m.fecha)} · ${haceCuanto(m.fecha, ahora)}`}>
                      {hora(m.fecha)}
                    </time>
                    {g.etiqueta === 'Hoy' && <small>{haceCuanto(m.fecha, ahora)}</small>}
                  </td>
                  <td data-etiqueta="Usuario">
                    <Persona registro={m} />
                  </td>
                  <td data-etiqueta="Acción">
                    <Accion clave={m.accion} />
                  </td>
                  <td data-etiqueta="Qué pasó" className="aud-col-que">
                    <QuePaso m={m} onAbrir={onAbrirExpediente} abierto={abierto} onAlternar={() => alternar(m.id)} />
                  </td>
                  <td data-etiqueta="Equipo">
                    <Equipo registro={m} />
                  </td>
                </tr>
                {abierto && (
                  <tr className="aud-detalle">
                    <td colSpan={5}>
                      <Diferencias m={m} />
                    </td>
                  </tr>
                )}
              </Fragment>
            );
          })}
        </tbody>
      ))}
    </table>
  );
}

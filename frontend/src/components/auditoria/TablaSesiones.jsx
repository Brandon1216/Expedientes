import Icono from '../Icono.jsx';
import { Equipo, Persona } from './Partes.jsx';
import { agruparPorDia, duracion, fechaDe, haceCuanto, hora } from './formato.js';

const MINUTOS_EN_LINEA = 10;

function Estado({ s, ahora }) {
  if (s.estado === 'CERRADA') return <span className="aud-secundario">{s.motivoFin}</span>;
  if (s.estado === 'EXPIRADA') {
    return (
      <span className="aud-estado-alerta">
        <Icono nombre="alerta" tamano={15} />
        No cerró sesión
      </span>
    );
  }
  const reciente = (ahora - fechaDe(s.ultimaActividad)) / 60000 < MINUTOS_EN_LINEA;
  return reciente ? (
    <span className="aud-en-linea">
      <span className="aud-punto" aria-hidden="true" />
      En línea
    </span>
  ) : (
    <span className="aud-secundario">Abierta, sin actividad {haceCuanto(s.ultimaActividad, ahora)}</span>
  );
}

function Alertas({ s }) {
  const lista = [
    s.simultanea && 'Sesiones al mismo tiempo',
    s.equipoNuevo && 'Equipo nuevo para este usuario',
    s.fueraDeHorario && 'Fuera de horario',
  ].filter(Boolean);
  if (lista.length === 0) return null;
  return (
    <ul className="aud-alertas">
      {lista.map((a) => (
        <li key={a}>
          <Icono nombre="alerta" tamano={14} />
          {a}
        </li>
      ))}
    </ul>
  );
}

export default function TablaSesiones({ filas }) {
  const ahora = new Date();
  return (
    <table className="tabla aud-tabla">
      <thead>
        <tr>
          <th scope="col">Usuario</th>
          <th scope="col">Equipo</th>
          <th scope="col">Entrada → salida</th>
          <th scope="col">Duración</th>
          <th scope="col">Estado</th>
        </tr>
      </thead>
      {agruparPorDia(filas, 'inicio').map((g) => (
        <tbody key={g.dia}>
          <tr className="aud-dia">
            <th colSpan={5} scope="colgroup">
              {g.etiqueta}
            </th>
          </tr>
          {g.filas.map((s) => {
            const final = s.fin || s.ultimaActividad;
            return (
              <tr key={s.id} className="aud-fila">
                <td data-etiqueta="Usuario">
                  <Persona registro={s} />
                </td>
                <td data-etiqueta="Equipo">
                  <Equipo registro={s} />
                </td>
                <td data-etiqueta="Entrada → salida" className="aud-col-hora">
                  <span>
                    {hora(s.inicio)} → {s.fin ? hora(s.fin) : '…'}
                  </span>
                  {!s.fin && <small>Última actividad: {hora(s.ultimaActividad)}</small>}
                </td>
                <td data-etiqueta="Duración">
                  {s.fin ? duracion(s.inicio, final) : `${duracion(s.inicio, final)} o más`}
                </td>
                <td data-etiqueta="Estado">
                  <Estado s={s} ahora={ahora} />
                  <Alertas s={s} />
                </td>
              </tr>
            );
          })}
        </tbody>
      ))}
    </table>
  );
}

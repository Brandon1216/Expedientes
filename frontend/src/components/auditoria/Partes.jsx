import Icono from '../Icono.jsx';
import { iniciales } from '../../api.js';
import { accion, equipo, persona } from './formato.js';

export function Persona({ registro }) {
  const p = persona(registro);
  return (
    <div className="aud-persona">
      <span className="avatar aud-avatar" aria-hidden="true">
        {iniciales(p.nombre)}
      </span>
      <div>
        <strong>{p.nombre}</strong>
        <small>@{p.usuario}</small>
      </div>
    </div>
  );
}

export function Accion({ clave }) {
  const a = accion(clave);
  return (
    <span className={`aud-accion aud-${a.tipo}`}>
      <Icono nombre={a.icono} tamano={16} />
      {a.texto}
    </span>
  );
}

export function Equipo({ registro }) {
  const e = equipo(registro);
  return (
    <div className="aud-equipo" title={e.titulo}>
      <span>{e.nombre}</span>
      {e.ip && <small>{e.ip}</small>}
    </div>
  );
}

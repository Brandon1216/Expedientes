import { createPortal } from 'react-dom';
import { STATUS, edad, hoy } from '../api.js';

export default function Impresion({ datos, foto }) {
  const anos = edad(datos.fechaNacimiento);

  return createPortal(
    <article className="hoja-impresion">
      <img className="hi-marca-agua" src="/logo.png" alt="" />

      <header className="hi-cabeza">
        <img className="hi-logo" src="/logo.png" alt="" />
        <div className="hi-titulo">
          <span className="hi-sindicato">S.U.T.S.P.E.M.I.D.B.C.</span>
          <h1>Expediente personal</h1>
          <span className="hi-lema">Por un Estado al servicio del pueblo</span>
        </div>
        <div className="hi-folio">
          <span>Número</span>
          <strong>{datos.numero || '—'}</strong>
          <em className={`hi-status hi-status-${datos.status}`}>{STATUS[datos.status]}</em>
        </div>
      </header>
      <div className="hi-filete" />

      <section className="hi-seccion">
        <h2>
          <span>01</span> Datos personales
        </h2>
        <div className="hi-personales">
          <div className="hi-campos">
            <Dato etiqueta="Nombre y apellido" valor={datos.nombre} ancho="completo" grande />
            <Dato etiqueta="Fecha de nacimiento" valor={fechaLarga(datos.fechaNacimiento)} />
            <Dato etiqueta="Edad" valor={anos !== '' ? `${anos} años` : ''} />
            <Dato etiqueta="Número de teléfono" valor={datos.telefono} />
            <Dato etiqueta="Fecha de ingreso" valor={fechaLarga(datos.fechaIngreso)} />
          </div>
          <figure className="hi-foto">
            {foto ? <img src={foto} alt="" /> : <span>Fotografía</span>}
          </figure>
        </div>
      </section>

      <section className="hi-seccion">
        <h2>
          <span>02</span> Solicitud
        </h2>
        <div className="hi-campos">
          <Dato etiqueta="Solicitud" valor={datos.solicitud} ancho="completo" />
          <Dato etiqueta="Secretaría asignada" valor={datos.secretaria} />
          <Dato etiqueta="Perfil" valor={datos.perfil} />
        </div>
      </section>

      <section className="hi-seccion">
        <h2>
          <span>03</span> Familiar sindicalizado
        </h2>
        <div className="hi-campos hi-tres">
          <Dato etiqueta="Nombre del familiar" valor={datos.familiarSindicalizado} />
          <Dato etiqueta="Parentesco" valor={datos.parentesco} />
          <Dato etiqueta="Dependencia" valor={datos.dependenciaFamiliar} />
        </div>
      </section>

      <section className="hi-seccion hi-seccion-notas">
        <h2>
          <span>04</span> Notas
        </h2>
        <div className={`hi-notas ${datos.notas ? '' : 'hi-notas-vacias'}`}>{datos.notas}</div>
      </section>

      <footer className="hi-pie">
        <span>S.U.T.S.P.E.M.I.D.B.C. · Expediente {datos.numero}</span>
        <span>Impreso el {fechaLarga(hoy())}</span>
      </footer>
    </article>,
    document.body,
  );
}

function Dato({ etiqueta, valor, ancho, grande }) {
  return (
    <div className={`hi-dato ${ancho === 'completo' ? 'hi-completo' : ''}`}>
      <span className="hi-etiqueta">{etiqueta}</span>
      <span className={`hi-valor ${grande ? 'hi-valor-grande' : ''}`}>{valor || ' '}</span>
    </div>
  );
}

function fechaLarga(iso) {
  if (!iso) return '';
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  return new Date(a, m - 1, d).toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' });
}

import { useCallback, useEffect, useState } from 'react';
import { api, SECRETARIAS, STATUS, formatoFecha, iniciales } from '../api.js';
import Icono from '../components/Icono.jsx';
import { useAvisos } from '../components/Avisos.jsx';
import Carpeta from '../components/Carpeta.jsx';

const FILTROS = [
  { valor: '', etiqueta: 'Todos', clave: 'total' },
  { valor: 'ACTIVO', etiqueta: 'Activos', clave: 'activos' },
  { valor: 'INACTIVO', etiqueta: 'Inactivos', clave: 'inactivos' },
];

export default function Expedientes({ usuario, onNuevo, abrirId, onAbierto }) {
  const avisar = useAvisos();
  const [lista, setLista] = useState([]);
  const [resumen, setResumen] = useState(null);
  const [texto, setTexto] = useState('');
  const [status, setStatus] = useState('');
  const [secretaria, setSecretaria] = useState('');
  const [cargando, setCargando] = useState(true);
  const [abierto, setAbierto] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const [datos, res] = await Promise.all([api.expedientes(texto, status, secretaria), api.resumen()]);
      setLista(datos);
      setResumen(res);
    } catch (err) {
      avisar(err.message, 'error');
    } finally {
      setCargando(false);
    }
  }, [texto, status, secretaria, avisar]);

  useEffect(() => {
    const t = setTimeout(cargar, texto ? 250 : 0);
    return () => clearTimeout(t);
  }, [cargar, texto]);

  useEffect(() => {
    if (!abrirId) return;
    api
      .expediente(abrirId)
      .then(setAbierto)
      .catch(() => avisar('Ese expediente ya no existe.', 'error'))
      .finally(onAbierto);
  }, [abrirId, onAbierto, avisar]);

  const alGuardar = (mensaje) => {
    avisar(mensaje);
    setAbierto(null);
    cargar();
  };

  return (
    <section className="pagina">
      <header className="pagina-cabeza">
        <div>
          <h1>Expedientes</h1>
          <p className="pagina-sub">Busca, abre y actualiza cualquier expediente.</p>
        </div>
        <button className="boton boton-primario" onClick={onNuevo}>
          <Icono nombre="mas" tamano={16} /> Nuevo expediente
        </button>
      </header>

      <div className="tabla-marco">
        <div className="barra">
          <div className="barra-filtros">
            <div className="filtros" role="tablist" aria-label="Filtrar por status">
              {FILTROS.map((f) => (
                <button
                  key={f.valor}
                  role="tab"
                  aria-selected={status === f.valor}
                  className={status === f.valor ? 'activo' : ''}
                  onClick={() => setStatus(f.valor)}
                >
                  {f.etiqueta}
                  <span className="cuenta">{resumen ? resumen[f.clave] : '–'}</span>
                </button>
              ))}
            </div>

            <label className={`filtro-secretaria ${secretaria ? 'activo' : ''}`}>
              <span className="solo-lector">Filtrar por secretaría</span>
              <select value={secretaria} onChange={(e) => setSecretaria(e.target.value)}>
                <option value="">Todas las secretarías</option>
                <option value="sin-asignar">Sin asignar</option>
                {SECRETARIAS.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="buscador">
            <Icono nombre="buscar" tamano={16} />
            <input
              type="search"
              placeholder="Buscar por número, nombre, solicitud, secretaría o teléfono"
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              aria-label="Buscar expedientes"
            />
          </label>
        </div>

        {cargando ? (
          <div className="esqueleto" aria-label="Cargando expedientes" role="status">
            {[0, 1, 2, 3, 4].map((i) => (
              <span key={i} />
            ))}
          </div>
        ) : lista.length === 0 ? (
          <div className="vacio">
            <span className="vacio-icono">
              <Icono nombre={texto || status || secretaria ? 'buscar' : 'carpeta'} tamano={22} />
            </span>
            {texto || status || secretaria ? (
              <>
                <strong>Sin resultados</strong>
                <p>Prueba con otro término o cambia los filtros.</p>
                <button
                  className="boton boton-suave"
                  onClick={() => {
                    setTexto('');
                    setStatus('');
                    setSecretaria('');
                  }}
                >
                  Quitar filtros
                </button>
              </>
            ) : (
              <>
                <strong>Aún no hay expedientes</strong>
                <p>Crea el primero para empezar a llevar el control.</p>
                <button className="boton boton-primario" onClick={onNuevo}>
                  <Icono nombre="mas" tamano={16} /> Nuevo expediente
                </button>
              </>
            )}
          </div>
        ) : (
          <>
            <div className="tabla-cuerpo">
              <table className="tabla">
                <thead>
                  <tr>
                    <th className="col-folio">Número</th>
                    <th>Nombre y apellido</th>
                    <th className="col-asunto">Solicitud</th>
                    <th className="col-asunto col-secretaria">Secretaría</th>
                    <th>Status</th>
                    <th className="col-fecha">Ingreso</th>
                  </tr>
                </thead>
                <tbody>
                  {lista.map((e) => (
                    <tr
                      key={e.id}
                      onClick={() => setAbierto(e)}
                      onKeyDown={(ev) => ev.key === 'Enter' && setAbierto(e)}
                      tabIndex={0}
                    >
                      <td className="folio col-folio">{e.numero}</td>
                      <td>
                        <div className="persona">
                          <span className="avatar">{iniciales(e.nombre)}</span>
                          <div className="titular">
                            <span className="folio folio-movil">{e.numero}</span>
                            {e.nombre}
                          </div>
                        </div>
                      </td>
                      <td className="col-asunto atenuado" title={e.solicitud}>
                        {e.solicitud || '—'}
                      </td>
                      <td className="col-asunto col-secretaria atenuado" title={e.secretaria}>
                        {e.secretaria || '—'}
                      </td>
                      <td>
                        <span className={`estado estado-${e.status}`}>{STATUS[e.status]}</span>
                      </td>
                      <td className="col-fecha atenuado numero">{formatoFecha(e.fechaIngreso)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <footer className="tabla-pie numero">
              {resumen ? `Mostrando ${lista.length} de ${resumen.total} expedientes` : `${lista.length} expedientes`}
            </footer>
          </>
        )}
      </div>

      {abierto && (
        <Carpeta
          expediente={abierto}
          esAdmin={usuario.rol === 'ADMIN'}
          onCerrar={() => setAbierto(null)}
          onGuardado={alGuardar}
        />
      )}
    </section>
  );
}

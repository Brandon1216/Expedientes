import { useCallback, useEffect, useState } from 'react';
import { api, formatoFecha } from '../api.js';
import Icono from '../components/Icono.jsx';
import { useAvisos } from '../components/Avisos.jsx';

const ESPERA_REVISION = 4000;

const enPrincipal = ['localhost', '127.0.0.1'].includes(window.location.hostname);

function nombreUnidad(u) {
  return u.nombre ? `${u.nombre} (${u.id})` : u.id;
}

function tamano(bytes) {
  const megas = bytes / (1024 * 1024);
  return megas >= 1024 ? `${(megas / 1024).toFixed(1)} GB` : `${Math.max(megas, 0.1).toFixed(1)} MB`;
}

export default function Respaldos() {
  const avisar = useAvisos();
  const [estado, setEstado] = useState(null);
  const [elegida, setElegida] = useState('');
  const [copiando, setCopiando] = useState(false);
  const [error, setError] = useState('');

  const revisar = useCallback(async () => {
    try {
      setEstado(await api.estadoRespaldos());
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    let activo = true;
    let espera;
    const ciclo = async () => {
      await revisar();
      if (activo) espera = setTimeout(ciclo, ESPERA_REVISION);
    };
    ciclo();
    return () => {
      activo = false;
      clearTimeout(espera);
    };
  }, [revisar]);

  const unidades = estado?.unidades || [];
  const listas = unidades.filter((u) => u.disponible);
  const bloqueadas = unidades.filter((u) => !u.disponible);
  const destino = listas.find((u) => u.id === elegida) || (listas.length === 1 ? listas[0] : null);
  const historial = estado?.historial || [];

  const copiar = async () => {
    setError('');
    setCopiando(true);
    try {
      const copia = await api.copiarRespaldo(destino.id);
      avisar(`Respaldo copiado y verificado en ${copia.unidad}`);
      window.dispatchEvent(new Event('respaldo-copiado'));
      await revisar();
    } catch (err) {
      setError(err.message);
    } finally {
      setCopiando(false);
    }
  };

  return (
    <section className="pagina pagina-registro">
      <header className="pagina-cabeza">
        <div>
          <h1>Respaldos</h1>
          <p className="pagina-sub">Guarda una copia de los expedientes fuera de esta computadora.</p>
        </div>
      </header>

      <div className="tabla-marco respaldo">
        <div className="respaldo-cuerpo">
          <h2>Copiar respaldo a USB</h2>
          <p className="respaldo-nota">
            {enPrincipal
              ? 'Conecta la USB a esta computadora.'
              : 'Estás en otro equipo: la USB debe estar conectada a la computadora principal, no a este equipo.'}
          </p>

          {estado &&
            (listas.length === 0 ? (
              <p className="respaldo-estado">
                <Icono nombre="alerta" tamano={16} />
                {bloqueadas.length > 0
                  ? `No se puede leer ${nombreUnidad(bloqueadas[0])}. Si está cifrada, desbloquéala e inténtalo de nuevo.`
                  : 'Conecta una USB a la computadora principal.'}
              </p>
            ) : listas.length === 1 ? (
              <p className="respaldo-estado respaldo-lista">
                <Icono nombre="check" tamano={16} />
                USB detectada: {nombreUnidad(listas[0])}, {tamano(listas[0].libre)} libres
              </p>
            ) : (
              <fieldset className="campo respaldo-unidades">
                <span>Elige a qué USB copiar</span>
                {listas.map((u) => (
                  <label key={u.id} className="respaldo-unidad">
                    <input
                      type="radio"
                      name="unidad"
                      value={u.id}
                      checked={destino?.id === u.id}
                      onChange={() => setElegida(u.id)}
                    />
                    {nombreUnidad(u)}, {tamano(u.libre)} libres
                  </label>
                ))}
              </fieldset>
            ))}

          {error && <p className="error">{error}</p>}

          <button className="boton boton-primario" onClick={copiar} disabled={!destino || copiando}>
            {copiando
              ? 'Copiando y verificando…'
              : destino
                ? `Copiar respaldo a USB (${destino.id})`
                : 'Copiar respaldo a USB'}
          </button>
        </div>

        <div className="respaldo-cuerpo respaldo-seccion">
          <h2>Últimas copias</h2>
          {historial.length === 0 ? (
            <p className="respaldo-nota">Todavía no se ha copiado ningún respaldo a una USB.</p>
          ) : (
            <ul className="respaldo-historial">
              {historial.map((c) => (
                <li key={c.archivo}>
                  <strong>
                    {formatoFecha(c.fecha)}, {c.fecha.slice(11, 16)}
                  </strong>
                  <span>
                    {c.unidad} · {tamano(c.bytes)} · {c.usuario}
                  </span>
                  <small>{c.archivo}</small>
                </li>
              ))}
            </ul>
          )}
        </div>

        <details className="respaldo-cuerpo respaldo-seccion respaldo-restaurar">
          <summary>Cómo restaurar un respaldo</summary>
          <ol>
            <li>
              Cierra Expedientes: clic derecho en el escudo junto al reloj de Windows y <em>Cerrar Expedientes</em>.
            </li>
            <li>
              En una computadora nueva, instala el programa con <em>Instalar.bat</em>, ciérralo igual y sigue.
            </li>
            <li>
              En <code>C:\Expedientes\datos</code>, cambia el nombre de <code>expedientes.db</code> a{' '}
              <code>expedientes-anterior.db</code>.
            </li>
            <li>
              Copia el respaldo de la carpeta <code>Respaldos Expedientes</code> de la USB a{' '}
              <code>C:\Expedientes\datos</code> y cámbiale el nombre a <code>expedientes.db</code>.
            </li>
            <li>Abre Expedientes y entra con las contraseñas que había cuando se hizo ese respaldo.</li>
          </ol>
        </details>
      </div>
    </section>
  );
}

import { useCallback, useEffect, useMemo, useState } from 'react';
import { api } from '../api.js';
import Icono from '../components/Icono.jsx';
import { useAvisos } from '../components/Avisos.jsx';
import TablaMovimientos from '../components/auditoria/TablaMovimientos.jsx';
import TablaSesiones from '../components/auditoria/TablaSesiones.jsx';
import { ACCIONES, accion, diaDe, duracion, equipo, persona } from '../components/auditoria/formato.js';

const POR_PAGINA = 50;
const RANGOS = [
  ['todo', 'Todo'],
  ['hoy', 'Hoy'],
  ['7dias', 'Últimos 7 días'],
  ['mes', 'Este mes'],
  ['personalizado', 'Personalizado'],
];
const INICIALES = { usuario: '', accion: '', rango: 'todo', desde: '', hasta: '', ocultarSesiones: true };

function fechasDeRango(f) {
  const hoy = new Date();
  if (f.rango === 'hoy') return { desde: diaDe(hoy), hasta: diaDe(hoy) };
  if (f.rango === '7dias') {
    const d = new Date(hoy);
    d.setDate(hoy.getDate() - 6);
    return { desde: diaDe(d), hasta: diaDe(hoy) };
  }
  if (f.rango === 'mes') return { desde: diaDe(new Date(hoy.getFullYear(), hoy.getMonth(), 1)), hasta: diaDe(hoy) };
  if (f.rango === 'personalizado') return { desde: f.desde, hasta: f.hasta };
  return { desde: '', hasta: '' };
}

function aCsv(filas) {
  const celda = (v) => {
    const t = v == null ? '' : String(v);
    return /[;"\n\r]/.test(t) ? `"${t.replace(/"/g, '""')}"` : t;
  };
  return '﻿' + filas.map((f) => f.map(celda).join(';')).join('\r\n');
}

function escapar(t) {
  return String(t ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c]);
}

function tablaExportable(tipo, filas) {
  if (tipo === 'movimientos') {
    return [
      ['Fecha', 'Hora', 'Usuario', 'Nombre', 'Acción', 'Expediente / objeto', 'Detalle', 'Equipo', 'IP'],
      ...filas.map((m) => {
        const e = equipo(m);
        return [m.fecha.slice(0, 10), m.fecha.slice(11, 16), m.usuario, persona(m).nombre, accion(m.accion).texto,
          m.objeto || '', m.detalle || '', e.nombre, m.equipo || ''];
      }),
    ];
  }
  return [
    ['Usuario', 'Nombre', 'Equipo', 'IP', 'Entrada', 'Salida', 'Duración', 'Estado', 'Alertas'],
    ...filas.map((s) => {
      const e = equipo(s);
      const alertas = [s.simultanea && 'Sesiones al mismo tiempo', s.equipoNuevo && 'Equipo nuevo',
        s.fueraDeHorario && 'Fuera de horario'].filter(Boolean).join(', ');
      return [s.usuario, persona(s).nombre, e.nombre, s.equipo || '', s.inicio, s.fin || '',
        duracion(s.inicio, s.fin || s.ultimaActividad), s.fin ? s.motivoFin : s.estado === 'EXPIRADA' ? 'No cerró sesión' : 'Abierta',
        alertas];
    }),
  ];
}

export default function Auditoria({ onAbrirExpediente }) {
  const avisar = useAvisos();
  const [tipo, setTipo] = useState('movimientos');
  const [filtros, setFiltros] = useState(INICIALES);
  const [texto, setTexto] = useState('');
  const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0);
  const [datos, setDatos] = useState(null);
  const [resumen, setResumen] = useState(null);
  const [usuarios, setUsuarios] = useState([]);
  const [exportando, setExportando] = useState(false);

  useEffect(() => {
    api.usuariosAuditoria().then(setUsuarios).catch(() => setUsuarios([]));
  }, []);

  useEffect(() => {
    const t = setTimeout(() => setBusqueda(texto.trim()), 300);
    return () => clearTimeout(t);
  }, [texto]);

  const consulta = useMemo(() => {
    const { desde, hasta } = fechasDeRango(filtros);
    const base = { usuario: filtros.usuario, q: busqueda, desde, hasta };
    return tipo === 'movimientos'
      ? { ...base, accion: filtros.accion, ocultarSesiones: filtros.ocultarSesiones }
      : base;
  }, [tipo, filtros, busqueda]);

  useEffect(() => {
    setPagina(0);
  }, [consulta]);

  useEffect(() => {
    let vigente = true;
    setDatos(null);
    api
      .auditoria(tipo, { ...consulta, pagina, tamano: POR_PAGINA })
      .then((r) => vigente && setDatos({ ...r, tipo }))
      .catch((e) => vigente && avisar(e.message, 'error'));
    if (tipo === 'movimientos') {
      api
        .auditoria('resumen', consulta)
        .then((r) => vigente && setResumen(r))
        .catch(() => vigente && setResumen(null));
    }
    return () => {
      vigente = false;
    };
  }, [tipo, consulta, pagina, avisar]);

  const cambiar = (campo, valor) => setFiltros((f) => ({ ...f, [campo]: valor }));

  const chips = [
    filtros.usuario && { clave: 'usuario', texto: `Usuario: ${filtros.usuario}` },
    tipo === 'movimientos' && filtros.accion && { clave: 'accion', texto: `Acción: ${accion(filtros.accion).texto}` },
    busqueda && { clave: 'texto', texto: `Búsqueda: “${busqueda}”` },
    filtros.rango !== 'todo' && {
      clave: 'rango',
      texto:
        filtros.rango === 'personalizado'
          ? `Fechas: ${filtros.desde || '…'} a ${filtros.hasta || '…'}`
          : RANGOS.find(([v]) => v === filtros.rango)[1],
    },
  ].filter(Boolean);

  const quitar = (clave) => {
    if (clave === 'texto') {
      setTexto('');
      setBusqueda('');
    } else if (clave === 'rango') setFiltros((f) => ({ ...f, rango: 'todo', desde: '', hasta: '' }));
    else cambiar(clave, '');
  };

  const limpiar = () => {
    setFiltros(INICIALES);
    setTexto('');
    setBusqueda('');
  };

  const todas = useCallback(() => api.auditoria(tipo, { ...consulta, pagina: 0, tamano: 5000 }), [tipo, consulta]);

  const exportarCsv = async () => {
    setExportando(true);
    try {
      const r = await todas();
      const blob = new Blob([aCsv(tablaExportable(tipo, r.filas))], { type: 'text/csv;charset=utf-8' });
      const enlace = document.createElement('a');
      enlace.href = URL.createObjectURL(blob);
      enlace.download = `auditoria-${tipo}-${diaDe(new Date())}.csv`;
      enlace.click();
      URL.revokeObjectURL(enlace.href);
      if (r.total > r.filas.length) avisar(`Se exportaron los ${r.filas.length} registros más recientes de ${r.total}.`);
    } catch (e) {
      avisar(e.message, 'error');
    } finally {
      setExportando(false);
    }
  };

  const exportarPdf = async () => {
    const ventana = window.open('', '_blank');
    if (!ventana) {
      avisar('El navegador bloqueó la ventana para imprimir. Permite ventanas emergentes para este sitio.', 'error');
      return;
    }
    ventana.document.write('<p style="font-family:sans-serif">Preparando…</p>');
    setExportando(true);
    try {
      const r = await todas();
      const [cabeza, ...filas] = tablaExportable(tipo, r.filas);
      const filtrosTexto = chips.map((c) => c.texto).join(' · ') || 'Sin filtros';
      ventana.document.open();
      ventana.document.write(`<!doctype html><html lang="es"><head><meta charset="utf-8">
        <title>Auditoría - ${tipo === 'movimientos' ? 'Movimientos' : 'Sesiones'}</title>
        <style>
          body{font-family:'Segoe UI',system-ui,sans-serif;color:#221b1d;margin:24px;font-size:11px}
          h1{font-size:18px;margin:0 0 4px;color:#7d1a27}
          p{margin:0 0 14px;color:#6f6569}
          table{width:100%;border-collapse:collapse}
          th{text-align:left;border-bottom:2px solid #7d1a27;padding:5px 6px;font-size:10px;text-transform:uppercase;letter-spacing:.04em}
          td{border-bottom:1px solid #e3ded9;padding:5px 6px;vertical-align:top}
          @page{size:letter landscape;margin:12mm}
        </style></head><body>
        <h1>S.U.T.S.P.E.M.I.D.B.C. · Auditoría de ${tipo === 'movimientos' ? 'movimientos' : 'sesiones'}</h1>
        <p>${escapar(filtrosTexto)} · ${r.filas.length} de ${r.total} registros · Generado el ${new Date().toLocaleString('es-MX')}</p>
        <table><thead><tr>${cabeza.map((c) => `<th>${escapar(c)}</th>`).join('')}</tr></thead>
        <tbody>${filas.map((f) => `<tr>${f.map((c) => `<td>${escapar(c)}</td>`).join('')}</tr>`).join('')}</tbody></table>
        </body></html>`);
      ventana.document.close();
      ventana.focus();
      ventana.print();
    } catch (e) {
      ventana.close();
      avisar(e.message, 'error');
    } finally {
      setExportando(false);
    }
  };

  const total = datos?.total ?? 0;
  const primero = total === 0 ? 0 : pagina * POR_PAGINA + 1;
  const ultimo = Math.min(total, (pagina + 1) * POR_PAGINA);
  const paginas = Math.max(1, Math.ceil(total / POR_PAGINA));

  return (
    <section className="pagina aud-pagina">
      <header className="pagina-cabeza">
        <div>
          <h1>Auditoría</h1>
          <p className="pagina-sub">Quién entró, a qué hora, y qué hizo con los expedientes.</p>
        </div>
        <div className="aud-exportar">
          <button className="boton boton-suave" onClick={exportarCsv} disabled={exportando || total === 0}>
            <Icono nombre="descargar" tamano={16} /> Exportar CSV
          </button>
          <button className="boton boton-suave" onClick={exportarPdf} disabled={exportando || total === 0}>
            <Icono nombre="imprimir" tamano={16} /> Imprimir o PDF
          </button>
        </div>
      </header>

      <div className="tabla-marco">
        <div className="aud-controles">
          <div className="aud-fila-controles">
            <div className="filtros" role="tablist" aria-label="Tipo de registro">
              {[
                ['movimientos', 'Movimientos'],
                ['sesiones', 'Sesiones'],
              ].map(([valor, etiqueta]) => (
                <button
                  key={valor}
                  role="tab"
                  aria-selected={tipo === valor}
                  className={tipo === valor ? 'activo' : ''}
                  onClick={() => setTipo(valor)}
                >
                  {etiqueta}
                </button>
              ))}
            </div>
            <label className="buscador aud-buscador">
              <Icono nombre="buscar" tamano={16} />
              <input
                type="search"
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
                placeholder={tipo === 'movimientos' ? 'Buscar por expediente, nombre o usuario' : 'Buscar por usuario o equipo'}
                aria-label="Buscar en la auditoría"
              />
            </label>
          </div>

          <div className="aud-fila-controles aud-filtros">
            <div className="aud-rangos" role="group" aria-label="Periodo">
              {RANGOS.map(([valor, etiqueta]) => (
                <button
                  key={valor}
                  type="button"
                  className={filtros.rango === valor ? 'activo' : ''}
                  aria-pressed={filtros.rango === valor}
                  onClick={() => cambiar('rango', valor)}
                >
                  {etiqueta}
                </button>
              ))}
            </div>
            {filtros.rango === 'personalizado' && (
              <>
                <label className="campo aud-campo">
                  <span>Desde</span>
                  <input type="date" value={filtros.desde} onChange={(e) => cambiar('desde', e.target.value)} />
                </label>
                <label className="campo aud-campo">
                  <span>Hasta</span>
                  <input type="date" value={filtros.hasta} onChange={(e) => cambiar('hasta', e.target.value)} />
                </label>
              </>
            )}
            <label className="campo aud-campo">
              <span>Usuario</span>
              <select value={filtros.usuario} onChange={(e) => cambiar('usuario', e.target.value)}>
                <option value="">Todos</option>
                {usuarios.map((u) => (
                  <option key={u} value={u}>
                    {u}
                  </option>
                ))}
              </select>
            </label>
            {tipo === 'movimientos' && (
              <>
                <label className="campo aud-campo">
                  <span>Acción</span>
                  <select value={filtros.accion} onChange={(e) => cambiar('accion', e.target.value)}>
                    <option value="">Todas</option>
                    {Object.entries(ACCIONES).map(([valor, a]) => (
                      <option key={valor} value={valor}>
                        {a.texto}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="interruptor aud-interruptor">
                  <input
                    type="checkbox"
                    checked={filtros.ocultarSesiones}
                    onChange={(e) => cambiar('ocultarSesiones', e.target.checked)}
                  />
                  <span>Ocultar inicios y cierres de sesión</span>
                </label>
              </>
            )}
          </div>

          {chips.length > 0 && (
            <div className="aud-chips" aria-label="Filtros activos">
              {chips.map((c) => (
                <button key={c.clave} type="button" className="aud-chip" onClick={() => quitar(c.clave)}>
                  {c.texto}
                  <Icono nombre="cerrar" tamano={13} />
                  <span className="solo-lector">Quitar filtro</span>
                </button>
              ))}
              <button type="button" className="aud-limpiar" onClick={limpiar}>
                Limpiar filtros
              </button>
            </div>
          )}
        </div>

        {tipo === 'movimientos' && resumen && (
          <dl className="aud-resumen">
            <div>
              <dt>Movimientos</dt>
              <dd>{resumen.movimientos}</dd>
            </div>
            <div>
              <dt>Expedientes con cambios</dt>
              <dd>{resumen.expedientes}</dd>
            </div>
            <div className={resumen.eliminaciones > 0 ? 'aud-resumen-alerta' : ''}>
              <dt>
                {resumen.eliminaciones > 0 && <Icono nombre="basura" tamano={14} />}
                Eliminaciones
              </dt>
              <dd>{resumen.eliminaciones}</dd>
            </div>
            <div>
              <dt>Usuarios con actividad</dt>
              <dd>{resumen.usuarios}</dd>
            </div>
          </dl>
        )}

        {!datos || datos.tipo !== tipo ? (
          <div className="esqueleto" role="status" aria-label="Cargando">
            {[0, 1, 2, 3, 4].map((i) => (
              <span key={i} />
            ))}
          </div>
        ) : datos.filas.length === 0 ? (
          <div className="vacio">
            <span className="vacio-icono">
              <Icono nombre="historial" tamano={22} />
            </span>
            <strong>{chips.length ? 'Nada coincide con estos filtros' : 'Aún no hay actividad registrada'}</strong>
            <p>
              {chips.length
                ? 'Prueba con otro periodo, usuario o búsqueda.'
                : 'Aquí aparecerá lo que hagan los usuarios en el sistema.'}
            </p>
            {chips.length > 0 && (
              <button className="boton boton-suave" onClick={limpiar}>
                Limpiar filtros
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="aud-scroll">
              {tipo === 'movimientos' ? (
                <TablaMovimientos filas={datos.filas} onAbrirExpediente={onAbrirExpediente} />
              ) : (
                <TablaSesiones filas={datos.filas} />
              )}
            </div>
            <footer className="tabla-pie aud-pie">
              <span>
                Mostrando {primero}–{ultimo} de {total}
              </span>
              <div className="aud-paginas">
                <button
                  className="boton boton-suave boton-chico"
                  onClick={() => setPagina((p) => p - 1)}
                  disabled={pagina === 0}
                >
                  Anterior
                </button>
                <span>
                  Página {pagina + 1} de {paginas}
                </span>
                <button
                  className="boton boton-suave boton-chico"
                  onClick={() => setPagina((p) => p + 1)}
                  disabled={pagina + 1 >= paginas}
                >
                  Siguiente
                </button>
              </div>
            </footer>
          </>
        )}
      </div>
    </section>
  );
}

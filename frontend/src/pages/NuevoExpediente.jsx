import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Icono from '../components/Icono.jsx';
import { useAvisos } from '../components/Avisos.jsx';
import Impresion from '../components/Impresion.jsx';
import CamposExpediente, { valoresExpediente } from '../components/CamposExpediente.jsx';

// Página de registro. `borrador` vive en App: lo escrito se conserva al cambiar de sección.
export default function NuevoExpediente({ borrador, onBorrador, onTerminar }) {
  const avisar = useAvisos();
  const [form, setForm] = useState(() => borrador || valoresExpediente());
  const [foto, setFoto] = useState(borrador?.fotografia || null);
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    onBorrador(form);
  }, [form, onBorrador]);

  const guardar = async (e) => {
    e.preventDefault();
    setError('');
    setGuardando(true);
    try {
      const creado = await api.crearExpediente(form);
      avisar(`Expediente ${creado.numero} creado`);
      onTerminar();
    } catch (err) {
      setError(err.message);
      setGuardando(false);
    }
  };

  return (
    <section className="pagina pagina-registro">
      <header className="pagina-cabeza">
        <div>
          <h1>Nuevo expediente</h1>
          <p className="pagina-sub">Solo el número, la fecha de ingreso y el nombre son obligatorios.</p>
        </div>
      </header>

      <form className="registro" onSubmit={guardar}>
        <div className="formulario">
          <CamposExpediente form={form} setForm={setForm} foto={foto} setFoto={setFoto} setError={setError} enfocar />
        </div>

        <footer className="registro-pie">
          {error && <p className="error">{error}</p>}
          <button
            type="button"
            className="boton boton-suave"
            onClick={() => window.print()}
            title="Imprime lo que se ve en el formulario"
          >
            <Icono nombre="imprimir" tamano={16} /> Imprimir
          </button>
          <span className="separador" />
          <button type="button" className="boton boton-suave" onClick={onTerminar}>
            Cancelar
          </button>
          <button className="boton boton-primario" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Crear expediente'}
          </button>
        </footer>
      </form>

      <Impresion datos={form} foto={foto} />
    </section>
  );
}

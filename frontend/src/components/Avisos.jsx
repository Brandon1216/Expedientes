import { createContext, useCallback, useContext, useState } from 'react';
import Icono from './Icono.jsx';

// Avisos breves en la esquina ("Expediente guardado", errores, etc.)
const AvisosContext = createContext(() => {});

export function AvisosProvider({ children }) {
  const [avisos, setAvisos] = useState([]);

  const avisar = useCallback((texto, tipo = 'ok') => {
    const id = Date.now() + Math.random();
    setAvisos((a) => [...a, { id, texto, tipo }]);
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), 3200);
  }, []);

  return (
    <AvisosContext.Provider value={avisar}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`}>
            <Icono nombre={a.tipo === 'error' ? 'alerta' : 'check'} tamano={16} />
            {a.texto}
          </div>
        ))}
      </div>
    </AvisosContext.Provider>
  );
}

export const useAvisos = () => useContext(AvisosContext);

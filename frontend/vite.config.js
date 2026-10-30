import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Las peticiones a /api se reenvían al backend de Java (puerto 8080)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        // Quita el encabezado Origin: así el backend no rechaza por CORS
        // cuando entras desde otro dispositivo (celular, otra PC).
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
    },
  },
});

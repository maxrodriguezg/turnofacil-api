import { useEffect, useState } from 'react';
import { getHealth } from './api/client.js';
import './App.css';

const ETIQUETAS = {
  verificando: 'Verificando API…',
  conectada: 'API conectada',
  'sin-conexion': 'API sin conexión',
};

export default function App() {
  const [estadoApi, setEstadoApi] = useState('verificando');

  useEffect(() => {
    let activo = true;
    getHealth()
      .then(() => activo && setEstadoApi('conectada'))
      .catch(() => activo && setEstadoApi('sin-conexion'));
    return () => {
      activo = false;
    };
  }, []);

  return (
    <div className="app">
      <header className="app__cabecera">
        <h1 className="app__titulo">TurnoFácil</h1>
        <p className={`estado-api estado-api--${estadoApi}`} role="status">
          <span className="estado-api__punto" aria-hidden="true" />
          {ETIQUETAS[estadoApi]}
        </p>
      </header>
      <main className="app__contenido">
        <p>Aquí irán tus reservas.</p>
      </main>
    </div>
  );
}

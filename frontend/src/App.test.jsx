import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from './App.jsx';
import { getHealth } from './api/client.js';

vi.mock('./api/client.js', () => ({ getHealth: vi.fn() }));

describe('App', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra "API conectada" cuando /health responde', async () => {
    getHealth.mockResolvedValue({ status: 'UP' });
    render(<App />);
    expect(screen.getByRole('status')).toHaveTextContent('Verificando API');
    expect(await screen.findByText('API conectada')).toBeInTheDocument();
  });

  it('muestra "API sin conexión" cuando /health falla', async () => {
    getHealth.mockRejectedValue(new Error('caída'));
    render(<App />);
    expect(await screen.findByText('API sin conexión')).toBeInTheDocument();
  });
});

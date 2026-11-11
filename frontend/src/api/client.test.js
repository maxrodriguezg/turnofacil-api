import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  ApiError,
  createReserva,
  getHealth,
  getReserva,
  listReservas,
  normalizarFechaHora,
} from './client.js';

// Simula una respuesta de fetch.
function respuesta(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: body === undefined ? () => Promise.reject(new Error('sin cuerpo')) : () => Promise.resolve(body),
  };
}

describe('cliente de la API', () => {
  let fetchMock;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('getHealth llama a /api/health', async () => {
    fetchMock.mockResolvedValue(respuesta(200, { status: 'UP' }));
    await expect(getHealth()).resolves.toEqual({ status: 'UP' });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/health');
  });

  it('listReservas sin filtro llama a /api/reservas', async () => {
    fetchMock.mockResolvedValue(respuesta(200, []));
    await listReservas();
    expect(fetchMock.mock.calls[0][0]).toBe('/api/reservas');
  });

  it('listReservas codifica el filtro de estado', async () => {
    fetchMock.mockResolvedValue(respuesta(200, []));
    await listReservas('en proceso');
    expect(fetchMock.mock.calls[0][0]).toBe('/api/reservas?estado=en%20proceso');
  });

  it('getReserva llama a /api/reservas/{id}', async () => {
    fetchMock.mockResolvedValue(respuesta(200, { id: 7 }));
    await expect(getReserva(7)).resolves.toEqual({ id: 7 });
    expect(fetchMock.mock.calls[0][0]).toBe('/api/reservas/7');
  });

  it('createReserva envía POST con JSON y completa los segundos de la fecha', async () => {
    fetchMock.mockResolvedValue(respuesta(201, { id: 1, estado: 'pendiente' }));
    await createReserva({ cliente: 'Ana', servicio: 'Corte', fechaHora: '2026-12-01T10:30' });

    const [url, opciones] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/reservas');
    expect(opciones.method).toBe('POST');
    expect(JSON.parse(opciones.body)).toEqual({
      cliente: 'Ana',
      servicio: 'Corte',
      fechaHora: '2026-12-01T10:30:00',
    });
  });

  it('lanza ApiError con validationErrors cuando el backend responde 400', async () => {
    fetchMock.mockResolvedValue(
      respuesta(400, {
        status: 400,
        message: 'Datos inválidos',
        validationErrors: { cliente: 'El cliente es obligatorio' },
      }),
    );

    const error = await createReserva({ cliente: '', servicio: 'x', fechaHora: '' }).catch((e) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.message).toBe('Datos inválidos');
    expect(error.validationErrors).toEqual({ cliente: 'El cliente es obligatorio' });
  });

  it('lanza ApiError 404 con el mensaje del backend', async () => {
    fetchMock.mockResolvedValue(respuesta(404, { status: 404, message: 'Reserva no encontrada' }));
    const error = await getReserva(99).catch((e) => e);
    expect(error.status).toBe(404);
    expect(error.message).toBe('Reserva no encontrada');
    expect(error.validationErrors).toEqual({});
  });

  it('usa un mensaje genérico si el error no trae JSON', async () => {
    fetchMock.mockResolvedValue(respuesta(502, undefined));
    const error = await getHealth().catch((e) => e);
    expect(error.status).toBe(502);
    expect(error.message).toBe('Error 502');
  });

  it('lanza ApiError con status 0 si no hay conexión', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'));
    const error = await getHealth().catch((e) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(0);
  });
});

describe('normalizarFechaHora', () => {
  it('agrega :00 cuando faltan los segundos', () => {
    expect(normalizarFechaHora('2026-12-01T10:30')).toBe('2026-12-01T10:30:00');
  });

  it('no modifica una fecha que ya tiene segundos ni un valor vacío', () => {
    expect(normalizarFechaHora('2026-12-01T10:30:45')).toBe('2026-12-01T10:30:45');
    expect(normalizarFechaHora('')).toBe('');
  });
});

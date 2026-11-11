// Cliente HTTP de TurnoFácil API.
// El navegador siempre llama a /api/...; Vite (desarrollo) o nginx (producción) quitan el
// prefijo y reenvían a Spring Boot. Así no hace falta configurar CORS en el backend.
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api';

/**
 * Error de la API. Refleja el ErrorResponse del backend:
 * { timestamp, status, error, message, path, validationErrors }
 *  - status: código HTTP (0 si no hubo conexión)
 *  - validationErrors: { campo: "mensaje" } cuando el backend responde 400 por validación
 */
export class ApiError extends Error {
  constructor(message, { status = 0, validationErrors = {} } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
  }
}

async function leerJson(response) {
  try {
    return await response.json();
  } catch {
    return null; // respuesta vacía o que no es JSON
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      ...options,
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Inténtalo de nuevo en unos segundos.');
  }

  const body = await leerJson(response);

  if (!response.ok) {
    throw new ApiError(body?.message ?? `Error ${response.status}`, {
      status: response.status,
      validationErrors: body?.validationErrors ?? {},
    });
  }
  return body;
}

/**
 * El input <input type="datetime-local"> entrega "2026-11-01T10:30" (sin segundos).
 * El backend espera LocalDateTime ISO, así que completamos los segundos si faltan.
 */
export function normalizarFechaHora(valor) {
  return valor && valor.length === 16 ? `${valor}:00` : valor;
}

// GET /health
export function getHealth() {
  return request('/health');
}

// GET /reservas  (opcionalmente ?estado=pendiente)
export function listReservas(estado) {
  const query = estado ? `?estado=${encodeURIComponent(estado)}` : '';
  return request(`/reservas${query}`);
}

// GET /reservas/{id}
export function getReserva(id) {
  return request(`/reservas/${encodeURIComponent(id)}`);
}

// POST /reservas  -> 201 con la reserva creada
export function createReserva({ cliente, servicio, fechaHora }) {
  return request('/reservas', {
    method: 'POST',
    body: JSON.stringify({ cliente, servicio, fechaHora: normalizarFechaHora(fechaHora) }),
  });
}

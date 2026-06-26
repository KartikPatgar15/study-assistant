/**
 * Thin wrapper around fetch for communicating with the Spring Boot backend.
 *
 * Design notes
 * ────────────
 * • All requests go through a single function so we can add auth headers,
 *   request IDs, and retry logic in one place in future modules.
 * • The base URL is read from the environment so the same build can target
 *   different backends without code changes.
 * • Errors are normalised into a consistent shape: { message, status }.
 */

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/**
 * @param {string} path   - e.g. '/api/health'
 * @param {RequestInit} [options]
 * @returns {Promise<any>} Parsed JSON response
 */
export async function apiFetch(path, options = {}) {
  const url = `${BASE_URL}${path}`;

  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let message = `Request failed: ${response.status} ${response.statusText}`;
    try {
      const body = await response.json();
      message = body.message ?? message;
    } catch {
      // Body was not JSON – keep the default message.
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return response.json();
}

/** Convenience aliases */
export const api = {
  get:  (path, opts)         => apiFetch(path, { method: 'GET',    ...opts }),
  post: (path, body, opts)   => apiFetch(path, { method: 'POST',   body: JSON.stringify(body), ...opts }),
  put:  (path, body, opts)   => apiFetch(path, { method: 'PUT',    body: JSON.stringify(body), ...opts }),
  del:  (path, opts)         => apiFetch(path, { method: 'DELETE', ...opts }),
};

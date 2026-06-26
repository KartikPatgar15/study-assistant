import { useState, useEffect } from 'react';
import { api } from '../utils/api.js';

/**
 * Polls GET /api/health once on mount and exposes the result.
 *
 * Returns:
 *   status  – 'loading' | 'up' | 'down'
 *   error   – string | null
 */
export function useHealthCheck() {
  const [status, setStatus] = useState('loading');
  const [error,  setError]  = useState(null);

  useEffect(() => {
    let cancelled = false;

    api.get('/api/health')
      .then(() => {
        if (!cancelled) setStatus('up');
      })
      .catch((err) => {
        if (!cancelled) {
          setStatus('down');
          setError(err.message);
        }
      });

    return () => { cancelled = true; };
  }, []);

  return { status, error };
}

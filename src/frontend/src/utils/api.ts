import { loadAuth } from './auth';

/**
 * Make an authenticated API request with the JWT token
 */
export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const auth = loadAuth();

  const headers: HeadersInit = {
    ...options.headers,
  };

  // Add Authorization header if we have a token
  if (auth.accessToken) {
    headers['Authorization'] = `Bearer ${auth.accessToken}`;
  }

  return fetch(url, {
    ...options,
    headers,
  });
}

/**
 * Make an authenticated API request and parse JSON response
 */
export async function fetchJsonWithAuth<T = any>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetchWithAuth(url, options);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  return response.json();
}

/**
 * Get auth headers for manual fetch calls
 */
export function getAuthHeaders(): HeadersInit {
  const auth = loadAuth();
  const headers: HeadersInit = {};

  if (auth.accessToken) {
    headers['Authorization'] = `Bearer ${auth.accessToken}`;
  }

  return headers;
}

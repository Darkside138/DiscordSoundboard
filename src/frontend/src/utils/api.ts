import { loadAuth } from './auth';
import Cookies from 'js-cookie';

/**
 * Get the CSRF token from cookie
 */
export function getCsrfToken(): string | null {
  return Cookies.get('XSRF-TOKEN') || null;
}

/**
 * Check if the request method requires CSRF token
 */
function requiresCsrfToken(method?: string): boolean {
  if (!method) return false;
  const upperMethod = method.toUpperCase();
  return ['POST', 'PUT', 'DELETE', 'PATCH'].includes(upperMethod);
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

/**
 * Get auth headers including CSRF token for mutation requests
 */
export function getAuthHeadersWithCsrf(): HeadersInit {
  const headers = getAuthHeaders();

  const csrfToken = getCsrfToken();
  if (csrfToken) {
    headers['X-XSRF-TOKEN'] = csrfToken;
  }

  return headers;
}

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

  // Add CSRF token for mutation requests
  if (requiresCsrfToken(options.method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  return fetch(url, {
    ...options,
    credentials: 'include', // Always include cookies for CSRF token
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
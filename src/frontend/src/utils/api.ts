import { loadAuth } from './auth';

// Store CSRF token in memory
let csrfToken: string | null = null;

/**
 * Fetch CSRF token from the backend
 */
export async function fetchCsrfToken(): Promise<string | null> {
  try {
    const response = await fetch('/api/auth/csrf-token', {
      credentials: 'include', // Include cookies
    });

    if (response.ok) {
      const data = await response.json();
      csrfToken = data.token;
      return csrfToken;
    }
  } catch (error) {
    console.error('Failed to fetch CSRF token:', error);
  }

  return null;
}

/**
 * Get the current CSRF token
 */
export function getCsrfToken(): string | null {
  return csrfToken;
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
  if (requiresCsrfToken(options.method) && csrfToken) {
    headers['X-CSRF-TOKEN'] = csrfToken;
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

/**
 * Get auth headers including CSRF token for mutation requests
 */
export function getAuthHeadersWithCsrf(): HeadersInit {
  const headers = getAuthHeaders();

  if (csrfToken) {
    headers['X-CSRF-TOKEN'] = csrfToken;
  }

  return headers;
}
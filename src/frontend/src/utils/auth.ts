import { API_ENDPOINTS } from '../config';
import { getCsrfToken } from './api';

export interface DiscordUser {
  id: string;
  username: string;
  discriminator: string;
  avatar: string | null;
  globalName: string | null;
  roles?: string[];
  permissions?: {
    upload: boolean;
    deleteSounds: boolean;
    manageUsers: boolean;
    editSounds: boolean;
    playSounds: boolean;
    downloadSounds: boolean;
    updateVolume: boolean;
  };
}

export interface AuthState {
  accessToken: string | null;
  user: DiscordUser | null;
}

const AUTH_STORAGE_KEY = 'discord_auth';

/**
 * Convert kebab-case permission names from backend to camelCase for frontend
 */
function transformPermissions(backendPermissions: any): DiscordUser['permissions'] {
  if (!backendPermissions) {
    return undefined;
  }

  const permissionMap: Record<string, keyof NonNullable<DiscordUser['permissions']>> = {
    'upload': 'upload',
    'delete': 'deleteSounds',  // Backend uses "delete" instead of "deleteSounds"
    'delete-sounds': 'deleteSounds',
    'manage-users': 'manageUsers',
    'manageUsers': 'manageUsers',
    'edit-sounds': 'editSounds',
    'editSounds': 'editSounds',
    'play-sounds': 'playSounds',
    'playSounds': 'playSounds',
    'download-sounds': 'downloadSounds',
    'downloadSounds': 'downloadSounds',
    'update-volume': 'updateVolume',
    'updateVolume': 'updateVolume',
  };

  const transformed: any = {};

  // Handle array format: ["upload", "delete-sounds", ...]
  if (Array.isArray(backendPermissions)) {
    for (const permission of backendPermissions) {
      const frontendKey = permissionMap[permission];
      if (frontendKey) {
        transformed[frontendKey] = true;
      }
    }
  }
  // Handle object format: { "upload": true, "delete": false, ... }
  else if (typeof backendPermissions === 'object') {
    for (const [backendKey, value] of Object.entries(backendPermissions)) {
      const frontendKey = permissionMap[backendKey];
      if (frontendKey) {
        transformed[frontendKey] = value;
      }
    }
  }

  return transformed;
}

/**
 * Decode JWT token to extract payload
 */
function decodeJWT(token: string): any {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    const payload = parts[1];
    const decoded = atob(payload);
    return JSON.parse(decoded);
  } catch (error) {
    console.error('Error decoding JWT:', error);
    return null;
  }
}

export function saveAuth(authState: AuthState): void {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authState));
}

export function loadAuth(): AuthState {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY);
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch {
      return { accessToken: null, user: null };
    }
  }
  return { accessToken: null, user: null };
}

export function clearAuth(): void {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function initiateDiscordLogin(): void {
  // Redirect to Spring Boot OAuth endpoint
  // Spring Boot will handle the OAuth flow and redirect back with a token
  window.location.href = `${API_ENDPOINTS.OAUTH_LOGIN}`;
}

export async function handleOAuthRedirect(token: string): Promise<AuthState> {
  // Fetch user info using the token provided by Spring Boot
  const user = await fetchUserInfo(token);

  if (!user) {
    throw new Error('Failed to fetch user info');
  }

  const authState: AuthState = {
    accessToken: token,
    user: user
  };

  saveAuth(authState);
  return authState;
}

async function fetchUserInfo(accessToken: string): Promise<DiscordUser | null> {
  try {
    const response = await fetch(`${API_ENDPOINTS.AUTH_USER}`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    if (!response.ok) {
      return null;
    }

    const userData = await response.json();

    // Try to decode JWT to get permissions directly if backend is missing them
    const jwtPayload = decodeJWT(accessToken);

    if (jwtPayload && jwtPayload.permissions) {
      // Prefer JWT permissions over backend response since JWT is authoritative
      userData.permissions = jwtPayload.permissions;

      // Also get roles from JWT if available
      if (jwtPayload.roles) {
        userData.roles = jwtPayload.roles;
      }
    }

    if (userData.permissions) {
      const transformed = transformPermissions(userData.permissions);
      userData.permissions = transformed;
    }

    return userData;
  } catch (error) {
    console.error('Error fetching user info:', error);
    return null;
  }
}

export async function validateToken(accessToken: string): Promise<DiscordUser | null> {
  return fetchUserInfo(accessToken);
}

export async function logout(accessToken: string): Promise<void> {
  try {
    const headers: Record<string, string> = {
      'Authorization': `Bearer ${accessToken}`
    };

    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers['X-CSRF-TOKEN'] = csrfToken;
    }

    await fetch(`${API_ENDPOINTS.AUTH_LOGOUT}`, {
      method: 'POST',
      headers
    });
  } catch {
    // Ignore errors during logout
  }
  clearAuth();
}
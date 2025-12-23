// API configuration
// Force development mode when running in this environment
const isDevelopment = false; // Always use localhost:8080 in this environment

// In development, use localhost:8080
// In production (built app served by Spring Boot), use relative URLs
export const API_BASE_URL = isDevelopment ? 'http://localhost:8080' : '';

export const API_ENDPOINTS = {
  BASE: API_BASE_URL,
  SOUNDS_STREAM: `${API_BASE_URL}/api/soundFiles/stream`,
  VOLUME: `${API_BASE_URL}/api/volume`,
  VOLUME_STREAM: `${API_BASE_URL}/api/volume/stream`,
  FAVORITE: `${API_BASE_URL}/api/soundFiles/favorite`,
  SOUND_FILE: `${API_BASE_URL}/api/soundFiles`,
  DOWNLOAD: `${API_BASE_URL}/api/soundFiles/download`,
  UPLOAD: `${API_BASE_URL}/api/soundFiles/upload`,
  PLAY_FILE: `${API_BASE_URL}/bot/playFile`,
  RANDOM: `${API_BASE_URL}/bot/random`,
  STOP: `${API_BASE_URL}/bot/stop`,
  USERS_STREAM: `${API_BASE_URL}/api/users/stream`,
  DISCORD_USERS_STREAM: `${API_BASE_URL}/api/discordUsers/invoiceorselected/stream`,
  DISCORD_USERS: `${API_BASE_URL}/api/discordUsers`,
  PLAYBACK_STREAM: `${API_BASE_URL}/api/playback/stream`,
  AUDIO_FILE: `${API_BASE_URL}/api/soundFiles`,
  // Auth endpoints
  AUTH_CALLBACK: `${API_BASE_URL}/api/auth/callback`,
  AUTH_USER: `${API_BASE_URL}/api/auth/user`,
  AUTH_LOGOUT: `${API_BASE_URL}/api/auth/logout`,
  AUTH_REFRESH: `${API_BASE_URL}/api/auth/refresh`,
  AUTH_DEFAULT_PERMISSIONS: `${API_BASE_URL}/api/auth/default-permissions`,
  OAUTH_LOGIN: `${API_BASE_URL}/oauth2/authorization/discord`,
  CSRF_TOKEN: `${API_BASE_URL}/api/auth/csrf-token`,
  // Role management endpoints
  USER_ROLES: `${API_BASE_URL}/api/discordUsers/roles`,
  ASSIGN_ROLE: (userId: string) => `${API_BASE_URL}/api/discordUsers/${userId}/role`,
  REMOVE_ROLE: (userId: string) => `${API_BASE_URL}/api/discordUsers/${userId}/role`,
  // Permission management endpoints
  ROLE_PERMISSIONS: `${API_BASE_URL}/api/rolePermissions`,
  ROLE_PERMISSIONS_CONFIGURED: `${API_BASE_URL}/api/rolePermissions/configured`,
  ROLE_PERMISSIONS_FOR_ROLE: (role: string) => `${API_BASE_URL}/api/rolePermissions/${role}`,
  ADD_PERMISSION_TO_ROLE: (role: string) => `${API_BASE_URL}/api/rolePermissions/${role}/permissions`,
  REMOVE_PERMISSION_FROM_ROLE: (role: string, permission: string) => `${API_BASE_URL}/api/rolePermissions/${role}/permissions/${permission}`,
  RESET_ROLE_TO_DEFAULTS: (role: string) => `${API_BASE_URL}/api/rolePermissions/${role}/reset`,
  // Bot version
  BOT_VERSION: `${API_BASE_URL}/bot/version`,
};
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
  OAUTH_LOGIN: `${API_BASE_URL}/oauth2/authorization/discord`,
  // Bot version
  BOT_VERSION: `${API_BASE_URL}/bot/version`,
};
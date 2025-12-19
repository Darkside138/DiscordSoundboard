# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Discord Soundboard is a Spring Boot + React application that allows users to play sound files through a Discord bot in voice channels. The application features Discord OAuth2 authentication, role-based permissions, real-time updates via Server-Sent Events, and both web UI and Discord chat command interfaces.

**Tech Stack:**
- Backend: Java 17, Spring Boot 3.5.7, JDA 6.1.1 (Discord API), Lavaplayer (audio), H2 Database
- Frontend: React 18.3.1, TypeScript, Vite 6.3.5, Tailwind CSS, Radix UI
- Build: Gradle with node-gradle plugin for frontend integration

## Common Commands

### Backend Development

```bash
# Run application locally (requires bot token in application-local.properties)
./gradlew bootRun

# Run backend tests
./gradlew test

# Build entire application (includes frontend build)
./gradlew build

# Create distribution zip for release
./gradlew assembleBootDist

# Clean all build artifacts (includes frontend)
./gradlew clean
```

### Frontend Development

```bash
cd src/frontend

# Install dependencies
npm install

# Run frontend dev server (Vite)
npm run dev

# Build frontend for production
npm run build

# Run frontend tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in watch mode
npm run test:watch
```

### Running Single Tests

**Backend (JUnit):**
```bash
./gradlew test --tests "net.dirtydeeds.discordsoundboard.controllers.SoundControllerTest"
./gradlew test --tests "*.SoundServiceTest.testFindAll"
```

**Frontend (Vitest):**
```bash
cd src/frontend
npm test -- SoundButton.test.tsx
npm test -- --grep "should render favorite icon"
```

## Architecture Overview

### Backend Architecture

**Entry Point:** `src/main/java/net/dirtydeeds/discordsoundboard/MainController.java`

The backend follows a layered Spring Boot architecture with Discord bot integration:

```
Controllers (REST API + SSE)
    ↓
Services (Business Logic)
    ↓
Repositories (JPA/H2 Database)
    ↓
Discord Bot Layer (JDA + Lavaplayer)
```

**Key Backend Components:**

1. **REST Controllers** - Located in `src/main/java/net/dirtydeeds/discordsoundboard/controllers/`
   - `AuthController` - JWT authentication and CSRF token endpoints
   - `SoundController` - Sound file CRUD, upload, download, favorites, SSE streaming
   - `BotCommandController` - Play sounds, control volume, stop playback
   - `BotVolumeController` - Volume management with SSE updates
   - `DiscordUserController` - User management, entrance/leave sounds, SSE streaming
   - `PlaybackController` - Real-time playback events via SSE

2. **Services** - Located in `src/main/java/net/dirtydeeds/discordsoundboard/service/`
   - `SoundService` / `SoundServiceImpl` - Sound file operations
   - `DiscordUserService` / `DiscordUserServiceImpl` - User operations
   - `PlaybackService` / `PlaybackServiceImpl` - SSE emitter management for playback events

3. **Discord Bot Integration** - Located in `src/main/java/net/dirtydeeds/discordsoundboard/`
   - `JDABot` - Discord connection initialization with gateway intents
   - `PlayerManager` - Lavaplayer audio player management per guild
   - `AudioHandler` - Low-level audio transmission to Discord
   - `SoundPlayer` - Orchestrates sound playback across services
   - Event Listeners: `UserEventListener`, `EntranceSoundBoardListener`, `LeaveSoundBoardListener`, `MovedChannelListener`, `CommandListener`
   - Chat Commands: Located in `src/main/java/net/dirtydeeds/discordsoundboard/chat/commands/` (PlayCommand, RandomCommand, VolumeCommand, etc.)

4. **Security & Authentication**
   - `SecurityConfig` - OAuth2 login, logout, CORS configuration
   - `JwtUtil` - JWT token generation/validation (HMAC-SHA256)
   - `OAuth2LoginSuccessHandler` - Intercepts Discord OAuth, generates JWT with roles/permissions
   - `UserRoleConfig` - Role-based permission mapping (Admin, DJ, Moderator, User)
   - `CrsfCheckFilter` - CSRF token validation for mutation requests

5. **Database** - H2 file-based at `./discordDB/discordDB`
   - `SoundFileRepository` - JPA repository for sound metadata
   - `DiscordUserRepository` - JPA repository for user data
   - Entities: `SoundFile` (soundFileId, category, timesPlayed, favorite, volumeOffsetPercentage), `DiscordUser` (id, username, entranceSound, leaveSound, inVoice, avatarUrl)

**Real-Time Communication:**
- Multiple SSE endpoints provide live updates to frontend (5-minute timeout, 25-second heartbeats)
- Events: sound file updates, volume changes, user status updates, playback tracking

### Frontend Architecture

**Entry Point:** `src/frontend/src/App.tsx`

The frontend uses React hooks for state management with no external state library:

```
App Component (Orchestration)
    ↓
Custom Hooks (Data + Actions)
    ↓
UI Components (Presentation)
    ↓
API Utils (HTTP + SSE)
```

**Key Frontend Components:**

1. **Custom Hooks** - Located in `src/frontend/src/hooks/`
   - `useAuth.ts` - Discord OAuth flow, JWT token management, user permissions
   - `useSounds.ts` - SSE connection to sound files stream, favorites management
   - `useSoundActions.ts` - Play, favorite, delete, upload, download operations
   - `useVolume.ts` - Local volume state with localStorage persistence
   - `useVolumeSSE.ts` - Real-time volume updates from backend
   - `useFilters.ts` - Category, search, favorites/popular/recent filtering
   - `usePlaybackTracking.ts` - SSE connection for current playback events
   - `useLocalPlayback.ts` - Client-side HTML5 audio playback
   - `useTheme.ts` - Light/dark theme toggle with localStorage

2. **UI Components** - Located in `src/frontend/src/components/`
   - `SoundButton.tsx` - Sound tile with play, favorite, popularity indicators
   - `DiscordUsersList.tsx` - User selection panel with SSE updates, avatars, voice status
   - `AuthButton.tsx` - Discord login/logout with avatar display
   - `SettingsMenu.tsx` - Configuration menu (theme, upload, user management)
   - `ContextMenu.tsx` - Right-click menu for sound operations
   - `PermissionGuard.tsx` - Permission-based component rendering
   - `ui/` directory - 50+ Radix UI primitives (dialog, dropdown, form, etc.)

3. **API Integration** - Located in `src/frontend/src/utils/`
   - `api.ts` - Authentication headers (JWT Bearer token + CSRF), typed fetch wrappers
   - `auth.ts` - JWT decoding, user info fetching, OAuth redirect handling
   - `config.ts` - Centralized API endpoint configuration

4. **State Persistence** - localStorage keys:
   - `soundboard-theme`, `soundboard-favorites`, `soundboard-volume`
   - `soundboard-popular-count`, `soundboard-recent-count`
   - `discord_auth` (JWT token + user info)

**Authentication Flow:**
1. User clicks login → redirects to `/oauth2/authorization/discord`
2. Discord OAuth → backend redirects to app with `?token=<JWT>`
3. Frontend extracts token, decodes permissions, stores in localStorage
4. All API requests use `Authorization: Bearer {token}` header
5. Mutation requests include CSRF token from cookies

## Important Development Notes

### OAuth2 Configuration

OAuth must be configured in `application.properties` for authentication to work:
- `spring.security.oauth2.client.registration.discord.client-id`
- `spring.security.oauth2.client.registration.discord.client-secret`
- `jwt.secret` - Generate unique secret at https://jwtgenerator.com/tools/jwt-generator
- `adminUserList` - Comma-separated Discord user IDs for admin role

### Role-Based Permissions

**Roles:** Admin, DJ, Moderator, User (default: unauthenticated with play/download)

**Permission Mapping:**
- Admin: upload, delete-sounds, edit-sounds, manage-users, play-sounds, download-sounds, update-volume
- DJ: upload, edit-sounds, play-sounds, download-sounds
- Moderator: delete-sounds, edit-sounds, play-sounds, download-sounds, update-volume
- User: play-sounds, download-sounds

**Backend Validation:** `UserRoleConfig.hasPermission(token, permission)`
**Frontend Gating:** `PermissionGuard` component wraps protected UI elements

### CSRF Protection Configuration

The application uses CSRF protection for mutation requests (POST/PUT/DELETE/PATCH). This requires specific configuration for Spring Security 6.x (Spring Boot 3.x):

**Backend Configuration** (`SecurityConfig.java`):
```java
CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

http
    .cors(Customizer.withDefaults())  // Enable CORS
    .csrf(csrf -> csrf
        .csrfTokenRepository(tokenRepository)
        .csrfTokenRequestHandler(requestHandler)  // Required for Spring Security 6.x
    )
```

**CORS Configuration** (`WebConfiguration.java`):
```java
registry.addMapping("/**")
    .allowedOriginPatterns("*")      // NOT allowedOrigins("*")
    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true)          // Required for cookies
    .maxAge(3600);
```

**How It Works:**
1. Spring Security generates CSRF token and stores in `XSRF-TOKEN` cookie (httpOnly=false for JS access)
2. Frontend reads cookie via `Cookies.get('XSRF-TOKEN')`
3. Frontend sends token back in `X-XSRF-TOKEN` header on mutation requests
4. Spring Security validates token matches cookie value

**Common Pitfalls:**
- Using `allowedOrigins("*")` instead of `allowedOriginPatterns("*")` breaks credential support
- Missing `allowCredentials(true)` prevents cookies from being sent
- Missing `CsrfTokenRequestAttributeHandler` in Spring Security 6.x causes validation failures
- Missing `.cors(Customizer.withDefaults())` in SecurityFilterChain ignores CORS config

**Frontend Implementation** (`api.ts`):
- `fetchWithAuth()` automatically adds `X-XSRF-TOKEN` header for POST/PUT/DELETE/PATCH
- Uses `credentials: 'include'` to send cookies with requests

### Sound File Upload Validation

Backend enforces strict validation in `SoundController.uploadSoundFile()`:
- **Allowed formats:** MP3, WAV, OGG, M4A (validated via magic bytes, not just extension)
- **Max file size:** 10MB (configurable via `BotConfig.maxFileSizeInBytes`)
- **Naming:** Files sanitized, duplicates rejected

### Discord Bot Configuration

Bot requires specific Discord Developer Portal settings:
- **Privileged Gateway Intents:** Must enable Server Members Intent, Presence Intent, Message Content Intent
- **Bot Permissions:** Connect, Speak, Use Voice Activity (minimum)
- **OAuth2 Redirect:** `https://your_soundboard_url/login/oauth2/code/discord`

### Frontend Build Integration

The Gradle build automatically builds the frontend via `node-gradle` plugin:
- `buildFrontend` task runs `npm run build` in `src/frontend/`
- `copyFrontend` task copies `src/frontend/build/` to Spring Boot's `static/` resources
- `processResources` depends on `copyFrontend`
- Frontend build is included in `assembleBootDist` for releases

### Testing Patterns

**Backend Tests:**
- Use `@SpringBootTest` for integration tests with full application context
- Use `@WebMvcTest` for controller-only tests with mocked services
- Mock JDA bot components to avoid Discord API calls during tests

**Frontend Tests:**
- Vitest with `@testing-library/react` for component tests
- Tests located in `src/frontend/src/components/__tests__/`
- Mock SSE connections and API calls using `vi.fn()` and `vi.mock()`
- Use `vitest.setup.ts` for global test configuration

### Server-Sent Events (SSE) Usage

SSE is heavily used for real-time updates. Key patterns:
- **Connection Timeout:** 5 minutes, heartbeat every 25 seconds
- **Auto-Reconnection:** EventSource handles reconnection automatically
- **Cleanup:** Use `isMounted` flags in components to prevent updates after unmount
- **Endpoints:** `/api/soundFiles/stream`, `/api/discordUsers/invoiceorselected/stream`, `/api/volume/stream/{userId}`, `/api/playback/stream`

### Local Development Setup

1. Create `src/main/resources/application-local.properties` with at minimum:
   ```properties
   botToken=your-discord-bot-token
   username_to_join_channel=your-discord-username
   ```

2. Run backend: `./gradlew bootRun` (uses `application-local.properties` profile)

3. Run frontend dev server: `cd src/frontend && npm run dev` (Vite on port 5173)

4. Place test sound files in `src/main/resources/sounds/` for local development

### Common Integration Points

When adding new features, consider these integration touchpoints:

**Adding New Sound Metadata:**
1. Update `SoundFile` entity (`src/main/java/.../entity/SoundFile.java`)
2. Update `SoundService` interface/implementation
3. Update `SoundController` REST endpoints
4. Update frontend `Sound` interface (`src/frontend/src/types.ts` or inline in hooks)
5. Update `useSounds.ts` transformation logic
6. Update `SoundButton.tsx` UI rendering

**Adding New Permissions:**
1. Define permission in `application.yml` role mappings
2. Add permission constant to `UserRoleConfig`
3. Use `hasPermission()` check in relevant controller
4. Add permission to frontend `AuthUser` interface
5. Wrap UI elements with `PermissionGuard`

**Adding New Discord Bot Events:**
1. Create listener class implementing JDA `ListenerAdapter`
2. Override relevant event methods (e.g., `onGuildVoiceUpdate`)
3. Register listener in `JDABot.getJDA()` with `.addEventListeners(new YourListener())`
4. Update `DiscordUser` entity if tracking new user state
5. Broadcast updates via `DiscordUserService` and SSE if needed

### Version Bumping

Use Gradle task to bump patch version in `gradle.properties`:
```bash
./gradlew bumpPatchVersion
```

This auto-increments the patch version (e.g., 4.1.9 → 4.1.10) and is used in `assembleBootDist` for release naming.

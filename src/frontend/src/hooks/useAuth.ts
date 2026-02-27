import { useState, useEffect, useCallback } from 'react';
import {
  loadAuth,
  initiateDiscordLogin,
  logout,
  validateToken,
  clearAuth,
  handleOAuthRedirect,
  refreshToken,
  fetchDefaultPermissions,
  isTokenExpired,
  getTokenExpirationTime,
  type DiscordUser
} from '../utils/auth';
import { TOKEN_EXPIRED_EVENT } from '../utils/api';

export function useAuth() {
  const [authUser, setAuthUser] = useState<DiscordUser | null>(null);
  const [authLoading, setAuthLoading] = useState<boolean>(true);

  // Handle token expiration - switch to guest user
  const handleTokenExpired = useCallback(async () => {
    const guestUser = await fetchDefaultPermissions();
    setAuthUser(guestUser);
  }, []);

  // Listen for token expired events from API calls
  useEffect(() => {
    const handleExpiredEvent = () => {
      handleTokenExpired();
    };

    window.addEventListener(TOKEN_EXPIRED_EVENT, handleExpiredEvent);
    return () => {
      window.removeEventListener(TOKEN_EXPIRED_EVENT, handleExpiredEvent);
    };
  }, [handleTokenExpired]);

  // Periodic token expiration check
  useEffect(() => {
    const checkTokenExpiration = () => {
      const storedAuth = loadAuth();
      if (storedAuth.accessToken && isTokenExpired(storedAuth.accessToken)) {
        clearAuth();
        handleTokenExpired();
      }
    };

    // Check every minute
    const interval = setInterval(checkTokenExpiration, 60000);

    // Also set up a timer for exact expiration time
    const storedAuth = loadAuth();
    if (storedAuth.accessToken) {
      const expirationTime = getTokenExpirationTime(storedAuth.accessToken);
      if (expirationTime) {
        const timeUntilExpiry = expirationTime - Date.now();
        if (timeUntilExpiry > 0) {
          const timeout = setTimeout(() => {
            clearAuth();
            handleTokenExpired();
          }, timeUntilExpiry);
          return () => {
            clearInterval(interval);
            clearTimeout(timeout);
          };
        }
      }
    }

    return () => {
      clearInterval(interval);
    };
  }, [authUser, handleTokenExpired]);

  // Handle Discord OAuth callback
  useEffect(() => {
    const handleCallback = async () => {
      const urlParams = new URLSearchParams(window.location.search);
      const token = urlParams.get('token');

      if (token) {
        try {
          const authState = await handleOAuthRedirect(token);
          setAuthUser(authState.user);
          setAuthLoading(false);

          // Clean up URL
          window.history.replaceState({}, document.title, window.location.pathname);
        } catch (error) {
          console.error('Failed to authenticate:', error);
          setAuthLoading(false);
        }
      } else {
        // No OAuth callback, check for existing auth
        const storedAuth = loadAuth();
        if (storedAuth.accessToken && storedAuth.user) {
          // Validate token
          const user = await validateToken(storedAuth.accessToken);
          if (user) {
            setAuthUser(user);
          } else {
            // Token invalid, clear stored auth
            clearAuth();
            // Fetch default permissions for unauthenticated user
            const guestUser = await fetchDefaultPermissions();
            if (guestUser) {
              setAuthUser(guestUser);
            }
          }
        } else {
          // No authenticated user, fetch default permissions
          const guestUser = await fetchDefaultPermissions();
          if (guestUser) {
            setAuthUser(guestUser);
          }
        }
        setAuthLoading(false);
      }
    };

    handleCallback();
  }, []);

  const handleLogin = () => {
    initiateDiscordLogin();
  };

  const handleLogout = async () => {
    const storedAuth = loadAuth();
    if (storedAuth.accessToken) {
      await logout(storedAuth.accessToken);
    }
    // After logout, set user to guest with default permissions
    const guestUser = await fetchDefaultPermissions();
    if (guestUser) {
      setAuthUser(guestUser);
    } else {
      setAuthUser(null);
    }
  };

  const refreshAuthToken = async (): Promise<boolean> => {
    const storedAuth = loadAuth();
    if (!storedAuth.accessToken) return false;

    const newAuth = await refreshToken(storedAuth.accessToken);
    if (newAuth) {
      setAuthUser(newAuth.user);
      return true;
    }

    clearAuth();
    setAuthUser(null);
    return false;
  };

  return {
    authUser,
    authLoading,
    handleLogin,
    handleLogout,
    refreshAuthToken
  };
}
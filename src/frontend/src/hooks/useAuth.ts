import { useState, useEffect } from 'react';
import {
  loadAuth,
  initiateDiscordLogin,
  logout,
  validateToken,
  clearAuth,
  handleOAuthRedirect,
  type DiscordUser
} from '../utils/auth';
import { fetchCsrfToken } from '../utils/api';

export function useAuth() {
  const [authUser, setAuthUser] = useState<DiscordUser | null>(null);
  const [authLoading, setAuthLoading] = useState<boolean>(true);

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
          }
        }
        setAuthLoading(false);
      }
      
      // Fetch CSRF token
      await fetchCsrfToken();
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
    setAuthUser(null);
  };

  return {
    authUser,
    authLoading,
    handleLogin,
    handleLogout
  };
}

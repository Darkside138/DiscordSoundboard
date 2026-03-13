import React from 'react';
import { LogIn, LogOut, User } from 'lucide-react';

interface DiscordUser {
  id: string;
  username: string;
  discriminator: string;
  avatar: string | null;
  globalName: string | null;
}

interface AuthButtonProps {
  user: DiscordUser | null;
  onLogin: () => void;
  onLogout: () => void;
}

export function AuthButton({ user, onLogin, onLogout }: AuthButtonProps) {
  const getAvatarUrl = (user: DiscordUser) => {
    if (user.avatar) {
      return `https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png`;
    }
    // Default Discord avatar based on discriminator
    const defaultAvatarNum = user.discriminator === '0'
      ? (parseInt(user.id) >> 22) % 6
      : parseInt(user.discriminator) % 5;
    return `https://cdn.discordapp.com/embed/avatars/${defaultAvatarNum}.png`;
  };

  // Only show user info if actually authenticated (not guest user)
  if (user && user.id !== 'guest') {
    return (
      <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-white border border-gray-200 shadow-md dark:bg-gray-800 dark:border-gray-700 dark:shadow-none">
        <img
          src={getAvatarUrl(user)}
          alt={user.username}
          className="w-5 h-5 rounded-full"
        />
        <div className="flex flex-col min-w-0">
          <span className="text-sm truncate text-gray-900 dark:text-white">
            {user.globalName || user.username}
          </span>
        </div>
        <button
          onClick={onLogout}
          className="ml-2 p-1 rounded-lg transition-colors hover:bg-gray-100 text-gray-600 hover:text-gray-900 dark:hover:bg-gray-700 dark:text-gray-300 dark:hover:text-white"
          title="Logout"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    );
  }

  return (
    <button
      onClick={onLogin}
      className="flex items-center gap-2 px-4 py-2 rounded-lg transition-colors bg-[#5865F2] hover:bg-[#4752C4] text-white"
      title="Login with Discord"
    >
      <User className="w-4 h-4" />
      <span>Login</span>
    </button>
  );
}
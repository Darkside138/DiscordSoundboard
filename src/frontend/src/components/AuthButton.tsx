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
  theme: 'light' | 'dark';
}

export function AuthButton({ user, onLogin, onLogout, theme }: AuthButtonProps) {
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

  if (user) {
    return (
      <div className={`flex items-center gap-2 px-3 py-2 rounded-lg ${
        theme === 'dark' ? 'bg-gray-800 border border-gray-700' : 'bg-white border border-gray-200 shadow-md'
      }`}>
        <img
          src={getAvatarUrl(user)}
          alt={user.username}
          className="w-5 h-5 rounded-full"
        />
        <div className="flex flex-col min-w-0">
          <span className={`text-sm truncate ${
            theme === 'dark' ? 'text-white' : 'text-gray-900'
          }`}>
            {user.globalName || user.username}
          </span>
        </div>
        <button
          onClick={onLogout}
          className={`ml-2 p-1 rounded-lg transition-colors ${
            theme === 'dark'
              ? 'hover:bg-gray-700 text-gray-300 hover:text-white'
              : 'hover:bg-gray-100 text-gray-600 hover:text-gray-900'
          }`}
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
      className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
        theme === 'dark'
          ? 'bg-[#5865F2] hover:bg-[#4752C4] text-white'
          : 'bg-[#5865F2] hover:bg-[#4752C4] text-white'
      }`}
      title="Login with Discord"
    >
      <User className="w-4 h-4" />
      <span>Login</span>
    </button>
  );
}
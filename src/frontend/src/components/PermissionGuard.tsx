import React from 'react';
import { DiscordUser } from '../utils/auth';
import { hasPermission, Permissions } from '../hooks/userPermission';
import { Lock } from 'lucide-react';

interface PermissionGuardProps {
  user: DiscordUser | null;
  permission: keyof Permissions;
  children: React.ReactNode;
  fallback?: React.ReactNode;
  showLocked?: boolean;
  lockedMessage?: string;
  theme?: 'light' | 'dark';
}

/**
 * Component that conditionally renders content based on user permissions
 *
 * @example
 * <PermissionGuard user={authUser} permission="upload">
 *   <button>Upload Sound</button>
 * </PermissionGuard>
 */
export function PermissionGuard({
  user,
  permission,
  children,
  fallback,
  showLocked = false,
  lockedMessage,
  theme = 'dark',
}: PermissionGuardProps) {
  const hasAccess = hasPermission(user, permission);

  if (hasAccess) {
    return <>{children}</>;
  }

  if (fallback) {
    return <>{fallback}</>;
  }

  if (showLocked) {
    return (
      <div
        className={`flex items-center gap-2 px-4 py-2 rounded-lg opacity-50 cursor-not-allowed ${
          theme === 'dark'
            ? 'bg-gray-700 text-gray-400'
            : 'bg-gray-200 text-gray-500'
        }`}
        title={lockedMessage || `This feature requires the ${permission} permission`}
      >
        <Lock className="w-4 h-4" />
        {lockedMessage || 'Locked'}
      </div>
    );
  }

  return null;
}

interface PermissionTooltipProps {
  user: DiscordUser | null;
  permission: keyof Permissions;
  children: React.ReactElement;
  message?: string;
}

/**
 * Wraps a component and shows a tooltip if the user lacks permission
 *
 * @example
 * <PermissionTooltip user={authUser} permission="delete">
 *   <button onClick={handleDelete}>Delete</button>
 * </PermissionTooltip>
 */
export function PermissionTooltip({
  user,
  permission,
  children,
  message,
}: PermissionTooltipProps) {
  const hasAccess = hasPermission(user, permission);

  if (hasAccess) {
    return children;
  }

  const tooltipMessage = message || `You need the ${permission} permission to use this feature`;

  return (
    <div
      className="inline-block cursor-not-allowed opacity-50"
      title={tooltipMessage}
    >
      {React.cloneElement(children, {
        disabled: true,
        onClick: (e: React.MouseEvent) => {
          e.preventDefault();
          e.stopPropagation();
        },
      })}
    </div>
  );
}

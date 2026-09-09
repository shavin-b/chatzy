// StatusBadge component — consistent status badges across the dashboard
import { cn } from '../lib/cn';

type BadgeVariant =
  | 'active'
  | 'expiring'
  | 'expired'
  | 'suspended'
  | 'online'
  | 'offline'
  | 'locked'
  | 'pending'
  | 'sent'
  | 'success'
  | 'failed'
  | 'default';

const variantClasses: Record<BadgeVariant, string> = {
  active: 'bg-emerald-500/15 text-emerald-400 ring-1 ring-emerald-500/30',
  expiring: 'bg-amber-500/15 text-amber-400 ring-1 ring-amber-500/30',
  expired: 'bg-red-500/15 text-red-400 ring-1 ring-red-500/30',
  suspended: 'bg-orange-500/15 text-orange-400 ring-1 ring-orange-500/30',
  online: 'bg-teal-500/15 text-teal-400 ring-1 ring-teal-500/30',
  offline: 'bg-slate-500/15 text-slate-400 ring-1 ring-slate-500/30',
  locked: 'bg-red-900/30 text-red-400 ring-1 ring-red-700/50',
  pending: 'bg-yellow-500/15 text-yellow-400 ring-1 ring-yellow-500/30',
  sent: 'bg-blue-500/15 text-blue-400 ring-1 ring-blue-500/30',
  success: 'bg-emerald-500/15 text-emerald-400 ring-1 ring-emerald-500/30',
  failed: 'bg-red-500/15 text-red-400 ring-1 ring-red-500/30',
  default: 'bg-slate-700/50 text-slate-300 ring-1 ring-slate-600/30',
};

const variantDots: Record<BadgeVariant, string> = {
  active: 'bg-emerald-400',
  expiring: 'bg-amber-400',
  expired: 'bg-red-400',
  suspended: 'bg-orange-400',
  online: 'bg-teal-400 animate-pulse',
  offline: 'bg-slate-500',
  locked: 'bg-red-400',
  pending: 'bg-yellow-400 animate-pulse',
  sent: 'bg-blue-400 animate-pulse',
  success: 'bg-emerald-400',
  failed: 'bg-red-400',
  default: 'bg-slate-400',
};

function statusToVariant(status: string): BadgeVariant {
  switch (status.toLowerCase()) {
    case 'active': return 'active';
    case 'expiring soon': return 'expiring';
    case 'expired': return 'expired';
    case 'suspended': return 'suspended';
    case 'online': return 'online';
    case 'offline': return 'offline';
    case 'locked': return 'locked';
    case 'pending': return 'pending';
    case 'sent': return 'sent';
    case 'success': return 'success';
    case 'failed': return 'failed';
    default: return 'default';
  }
}

interface StatusBadgeProps {
  status: string;
  variant?: BadgeVariant;
  showDot?: boolean;
  size?: 'sm' | 'md';
  className?: string;
}

export function StatusBadge({
  status,
  variant,
  showDot = true,
  size = 'sm',
  className,
}: StatusBadgeProps) {
  const v = variant ?? statusToVariant(status);
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full font-medium',
        size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm',
        variantClasses[v],
        className
      )}
    >
      {showDot && (
        <span className={cn('h-1.5 w-1.5 rounded-full', variantDots[v])} />
      )}
      {status}
    </span>
  );
}

// Online/Offline badge specifically
export function OnlineBadge({ isOnline }: { isOnline: boolean }) {
  return (
    <StatusBadge
      status={isOnline ? 'Online' : 'Offline'}
      variant={isOnline ? 'online' : 'offline'}
    />
  );
}

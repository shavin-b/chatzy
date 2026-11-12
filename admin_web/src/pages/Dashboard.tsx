// Dashboard page — overview cards with real Firestore data

import { Smartphone, Wifi, WifiOff, CreditCard, Clock, XCircle, AlertOctagon } from 'lucide-react';
import { useDevices, useDashboardStats } from '../hooks/useDevices';
import { useAuth } from '../hooks/useAuth';
import { timeAgo } from '../lib/dateUtils';
import { StatusBadge, OnlineBadge } from '../components/StatusBadge';
import { Link } from 'react-router-dom';

function StatCard({
  label,
  value,
  icon: Icon,
  iconColor,
  loading,
}: {
  label: string;
  value: number | string;
  icon: typeof Smartphone;
  iconColor: string;
  loading?: boolean;
}) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5 transition hover:border-slate-700">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">{label}</p>
        <div className={`flex h-8 w-8 items-center justify-center rounded-lg bg-slate-800`}>
          <Icon className={`h-4 w-4 ${iconColor}`} />
        </div>
      </div>
      {loading ? (
        <div className="mt-3 h-8 w-16 skeleton rounded-lg" />
      ) : (
        <p className="mt-3 text-3xl font-bold text-white">{value}</p>
      )}
    </div>
  );
}

export function DashboardPage() {
  const { admin } = useAuth();
  const { devices, loading, error } = useDevices();
  const stats = useDashboardStats(devices);

  const recentDevices = devices.slice(0, 8);

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold text-white">Dashboard</h1>
        <p className="text-sm text-slate-500">
          Welcome back, {admin?.name ?? 'Admin'} · {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </p>
      </div>

      {/* Error state */}
      {error && (
        <div className="flex items-center gap-3 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-sm text-red-300">
          <XCircle className="h-5 w-5 flex-shrink-0 text-red-400" />
          {error}
        </div>
      )}

      {/* Stats grid */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7">
        <StatCard label="Total Devices" value={error ? "Unavailable" : stats.totalDevices} icon={Smartphone} iconColor="text-slate-400" loading={loading} />
        <StatCard label="Online" value={error ? "Unavailable" : stats.onlineDevices} icon={Wifi} iconColor="text-teal-400" loading={loading} />
        <StatCard label="Offline" value={error ? "Unavailable" : stats.offlineDevices} icon={WifiOff} iconColor="text-slate-500" loading={loading} />
        <StatCard label="Active" value={error ? "Unavailable" : stats.activeSubscriptions} icon={CreditCard} iconColor="text-emerald-400" loading={loading} />
        <StatCard label="Expiring Soon" value={error ? "Unavailable" : stats.expiringSoon} icon={Clock} iconColor="text-amber-400" loading={loading} />
        <StatCard label="Expired" value={error ? "Unavailable" : stats.expired} icon={XCircle} iconColor="text-red-400" loading={loading} />
        <StatCard label="Suspended" value={error ? "Unavailable" : stats.suspended} icon={AlertOctagon} iconColor="text-orange-400" loading={loading} />
      </div>

      {/* Recent Devices */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900">
        <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4">
          <h2 className="text-sm font-semibold text-white">Recent Devices</h2>
          <Link to="/devices" className="text-xs font-medium text-teal-400 hover:text-teal-300 transition">
            View all →
          </Link>
        </div>

        {loading ? (
          <div className="p-4 space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-10 skeleton rounded-lg" />
            ))}
          </div>
        ) : error ? null : devices.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-12 text-center">
            <Smartphone className="h-10 w-10 text-slate-700" />
            <p className="text-sm font-medium text-slate-500">No devices registered yet.</p>
            <p className="text-xs text-slate-600">Devices will appear here once the Android app is installed and registers itself.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-800">
            {recentDevices.map((device) => (
              <Link
                key={device.deviceId}
                to={`/devices/${encodeURIComponent(device.deviceId)}`}
                className="flex items-center gap-4 px-5 py-3.5 transition hover:bg-slate-800/50"
              >
                <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-slate-800">
                  <Smartphone className="h-4 w-4 text-slate-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-200 truncate">
                    {device.businessName || device.userName || device.deviceId}
                  </p>
                  <p className="text-xs text-slate-500 truncate">
                    {device.deviceId} · {device.phoneModel}
                  </p>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <StatusBadge status={device.computedStatus} />
                  <OnlineBadge isOnline={device.isOnline} />
                  <span className="hidden text-xs text-slate-600 sm:block">
                    {timeAgo(device.lastSeen)}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

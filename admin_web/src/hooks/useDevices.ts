// useDevices hook — real-time Firestore subscription to all devices

import { useEffect, useState } from 'react';
import type { NormalizedDevice, DashboardStats } from '../types';
import { subscribeToDevices } from '../services/devices';
import { DEFAULT_ONLINE_THRESHOLD_MS } from '../lib/dateUtils';

interface UseDevicesReturn {
  devices: NormalizedDevice[];
  loading: boolean;
  error: string | null;
}

export function useDevices(onlineThresholdMs: number = DEFAULT_ONLINE_THRESHOLD_MS): UseDevicesReturn {
  const [devices, setDevices] = useState<NormalizedDevice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = subscribeToDevices(
      (data) => {
        setDevices(data);
        setLoading(false);
        setError(null);
      },
      (err) => {
        const code = (err as Error & {code?: string}).code ?? 'unknown';
        console.error('[useDevices]', {code});
        setDevices([]);
        setError(code === 'permission-denied'
          ? 'Device list access denied. Check the signed-in admin role and Firestore device-read permissions.'
          : code === 'unavailable'
            ? 'Cannot reach the device database. Check your connection and retry.'
            : 'Unable to load device data. Retry or contact your administrator.');
        setLoading(false);
      },
      onlineThresholdMs
    );
    return unsubscribe;
  }, [onlineThresholdMs]);

  return { devices, loading, error };
}

// ---------------------------------------------------------------
// Derived dashboard stats from device list
// ---------------------------------------------------------------

export function useDashboardStats(
  devices: NormalizedDevice[]
): DashboardStats {
  return {
    totalDevices: devices.length,
    onlineDevices: devices.filter((d) => d.isOnline).length,
    offlineDevices: devices.filter((d) => !d.isOnline).length,
    activeSubscriptions: devices.filter((d) => d.computedStatus === 'Active').length,
    expiringSoon: devices.filter((d) => d.computedStatus === 'Expiring Soon').length,
    expired: devices.filter((d) => d.computedStatus === 'Expired').length,
    suspended: devices.filter((d) => d.computedStatus === 'Suspended').length,
  };
}

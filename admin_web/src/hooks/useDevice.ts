// useDevice hook — real-time listener for a single device

import { useEffect, useState } from 'react';
import type { NormalizedDevice } from '../types';
import { subscribeToDevice } from '../services/devices';
import { DEFAULT_ONLINE_THRESHOLD_MS } from '../lib/dateUtils';

interface UseDeviceReturn {
  device: NormalizedDevice | null;
  loading: boolean;
  error: string | null;
  notFound: boolean;
}

export function useDevice(
  deviceId: string | undefined,
  onlineThresholdMs: number = DEFAULT_ONLINE_THRESHOLD_MS
): UseDeviceReturn {
  const [device, setDevice] = useState<NormalizedDevice | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!deviceId) {
      setLoading(false);
      setNotFound(true);
      return;
    }

    const unsubscribe = subscribeToDevice(
      deviceId,
      (data) => {
        if (data === null) {
          setNotFound(true);
        } else {
          setDevice(data);
          setNotFound(false);
        }
        setLoading(false);
        setError(null);
      },
      (err) => {
        if (err.message?.includes('permission-denied')) {
          setError('Permission denied. You do not have access to this device.');
        } else {
          setError('Unable to load device data.');
        }
        setLoading(false);
      },
      onlineThresholdMs
    );

    return unsubscribe;
  }, [deviceId, onlineThresholdMs]);

  return { device, loading, error, notFound };
}

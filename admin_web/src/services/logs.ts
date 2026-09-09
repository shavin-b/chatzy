// Logs service — audit log reads from logs collection

import {
  collection,
  onSnapshot,
  query,
  orderBy,
  limit,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './firebase';
import type { LogEntry } from '../types';

function normalizeLog(data: Record<string, unknown>, id: string): LogEntry {
  return {
    logId: id,
    timestamp: (data.timestamp as number) ?? 0,
    title: (data.title as string) ?? '',
    description: (data.description as string) ?? '',
    category: (data.category as string) ?? '',
    performedBy: (data.performedBy as string) ?? '',
    deviceId: (data.deviceId as string | null) ?? null,
  };
}

export function subscribeToLogs(
  onData: (logs: LogEntry[]) => void,
  onError: (error: Error) => void,
  pageSize: number = 200
): Unsubscribe {
  const q = query(
    collection(db, 'logs'),
    orderBy('timestamp', 'desc'),
    limit(pageSize)
  );
  return onSnapshot(
    q,
    (snapshot) => {
      const logs = snapshot.docs.map((d) => normalizeLog(d.data(), d.id));
      onData(logs);
    },
    (error) => {
      console.error('[logs] Firestore error:', error.message);
      onError(error);
    }
  );
}

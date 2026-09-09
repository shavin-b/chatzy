// Commands service
// Handles LOCK/UNLOCK FCM command flow via Cloudflare Worker API
// NEVER sends FCM credentials or tokens from the browser

import {
  doc,
  onSnapshot,
  type Unsubscribe,
  collection,
  query,
  orderBy,
  limit,
  where,
} from 'firebase/firestore';
import { db, auth } from './firebase';
import type { DeviceCommand, CommandStatus } from '../types';
import { toEpochMs } from '../lib/dateUtils';

interface SendCommandResponse {
  commandId: string;
  status: CommandStatus;
  message: string;
}

// ---------------------------------------------------------------
// Send Device Command via Cloudflare Worker API
// Server-side Worker validates Firebase ID token, checks admin role,
// reads device FCM token, writes command record, and dispatches FCM.
// ---------------------------------------------------------------

export async function sendDeviceCommand(
  deviceId: string,
  command: 'LOCK' | 'UNLOCK' | 'SYNC'
): Promise<SendCommandResponse> {
  // 1. Verify VITE_COMMAND_API_BASE_URL configuration
  const rawBaseUrl = import.meta.env.VITE_COMMAND_API_BASE_URL;
  if (!rawBaseUrl || rawBaseUrl.trim() === '') {
    const err = new Error('VITE_COMMAND_API_BASE_URL is not configured');
    (err as unknown as { code: string }).code = 'CONFIG_ERROR';
    throw err;
  }

  // 2. Normalize Base URL safely
  const normalizedBaseUrl = rawBaseUrl.trim().replace(/\/+$/, '');
  const commandApiUrl = `${normalizedBaseUrl}/api/device-command`;

  // 3. Verify Authenticated User
  const currentUser = auth.currentUser;
  if (!currentUser) {
    const err = new Error('Authentication session expired. Please sign in again.');
    (err as unknown as { code: string }).code = 'UNAUTHENTICATED';
    throw err;
  }

  // 4. Retrieve Firebase ID Token (never logged!)
  const idToken = await currentUser.getIdToken();

  // 5. Safe Development Log (DO NOT log tokens, credentials, or keys!)
  console.log('[CommandAPI] URL:', commandApiUrl);
  console.log('[CommandAPI] Command:', command);
  console.log('[CommandAPI] Device:', deviceId);

  // 6. Execute fetch() call with Network Error Handling
  let response: Response;
  try {
    response = await fetch(commandApiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${idToken}`,
      },
      body: JSON.stringify({ deviceId, command }),
    });
  } catch (fetchErr: unknown) {
    console.error('[CommandAPI] fetch() Network Error:', fetchErr);
    const err = new Error(`Command API network error. Worker URL: ${commandApiUrl}`);
    (err as unknown as { code: string }).code = 'NETWORK_ERROR';
    throw err;
  }

  // 7. Parse Response
  const data = (await response.json().catch(() => ({}))) as {
    commandId?: string;
    status?: CommandStatus;
    message?: string;
    error?: string;
  };

  if (!response.ok) {
    const errorCode = data.error || `HTTP_${response.status}`;
    const errorMessage = data.message || `Failed to send command (${response.status})`;
    const err = new Error(errorMessage);
    (err as unknown as { code: string }).code = errorCode;
    throw err;
  }

  return {
    commandId: data.commandId || '',
    status: data.status || 'SENT',
    message: data.message || 'Command sent',
  };
}

// ---------------------------------------------------------------
// Normalize raw command document
// ---------------------------------------------------------------

function normalizeCommand(data: Record<string, unknown>, id: string): DeviceCommand {
  return {
    commandId: id,
    deviceId: (data.deviceId as string) ?? '',
    command: (data.command as DeviceCommand['command']) ?? 'LOCK',
    status: (data.status as CommandStatus) ?? 'PENDING',
    createdAt: toEpochMs(data.createdAt as DeviceCommand['createdAt']),
    sentAt: toEpochMs(data.sentAt as DeviceCommand['sentAt']),
    processedAt: toEpochMs(data.processedAt as DeviceCommand['processedAt']),
    errorMessage: (data.errorMessage as string | null) ?? null,
    adminUid: (data.adminUid as string) ?? undefined,
    adminEmail: (data.adminEmail as string) ?? undefined,
  };
}

// ---------------------------------------------------------------
// Real-time listener: single command (for acknowledgement tracking)
// ---------------------------------------------------------------

export function subscribeToCommand(
  commandId: string,
  onData: (command: DeviceCommand | null) => void,
  onError: (error: Error) => void
): Unsubscribe {
  return onSnapshot(
    doc(db, 'deviceCommands', commandId),
    (snap) => {
      if (!snap.exists()) {
        onData(null);
        return;
      }
      onData(normalizeCommand(snap.data(), snap.id));
    },
    (error) => {
      console.error('[command] Firestore error:', error.message);
      onError(error);
    }
  );
}

// ---------------------------------------------------------------
// Real-time listener: all commands (Activity Log)
// ---------------------------------------------------------------

export function subscribeToCommands(
  onData: (commands: DeviceCommand[]) => void,
  onError: (error: Error) => void,
  pageSize: number = 100
): Unsubscribe {
  const q = query(
    collection(db, 'deviceCommands'),
    orderBy('createdAt', 'desc'),
    limit(pageSize)
  );
  return onSnapshot(
    q,
    (snapshot) => {
      const commands = snapshot.docs.map((d) =>
        normalizeCommand(d.data(), d.id)
      );
      onData(commands);
    },
    (error) => {
      console.error('[commands] Firestore error:', error.message);
      onError(error);
    }
  );
}

// ---------------------------------------------------------------
// Real-time listener: commands for a specific device
// ---------------------------------------------------------------

export function subscribeToDeviceCommands(
  deviceId: string,
  onData: (commands: DeviceCommand[]) => void,
  onError: (error: Error) => void
): Unsubscribe {
  const q = query(
    collection(db, 'deviceCommands'),
    where('deviceId', '==', deviceId),
    orderBy('createdAt', 'desc'),
    limit(50)
  );
  return onSnapshot(
    q,
    (snapshot) => {
      const commands = snapshot.docs.map((d) =>
        normalizeCommand(d.data(), d.id)
      );
      onData(commands);
    },
    (error) => onError(error)
  );
}

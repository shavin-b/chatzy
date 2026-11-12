// useDeviceCommand hook
// Manages the full lifecycle of LOCK/UNLOCK commands
// idle → confirming → pending → sent → success/failed

import { useState, useEffect, useRef } from 'react';
import type { CommandType, CommandUIState, DeviceCommand } from '../types';
import { sendDeviceCommand, subscribeToCommand } from '../services/commands';
import { writeAuditLog } from '../services/subscriptions';
import { useAuth } from './useAuth';

interface UseDeviceCommandReturn {
  uiState: CommandUIState;
  lastCommand: DeviceCommand | null;
  errorMessage: string | null;
  issueCommand: (deviceId: string, command: CommandType) => Promise<void>;
  resetState: () => void;
}

const COMMAND_TIMEOUT_MS = 30_000; // 30 seconds

export function useDeviceCommand(): UseDeviceCommandReturn {
  const { user, admin } = useAuth();
  const [uiState, setUiState] = useState<CommandUIState>('idle');
  const [lastCommand, setLastCommand] = useState<DeviceCommand | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const unsubRef = useRef<(() => void) | null>(null);
  const inFlightRef = useRef(false);
  const generationRef = useRef(0);

  function cleanup() {
    generationRef.current += 1;
    inFlightRef.current = false;
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    if (unsubRef.current) unsubRef.current();
    timeoutRef.current = null;
    unsubRef.current = null;
  }

  useEffect(() => {
    return () => cleanup();
  }, []);

  async function issueCommand(deviceId: string, command: CommandType): Promise<void> {
    if (inFlightRef.current) return;
    if (!user || !admin) {
      setErrorMessage('Not authenticated');
      return;
    }

    cleanup();
    const generation = generationRef.current;
    inFlightRef.current = true;
    setErrorMessage(null);
    setUiState('pending');

    try {
      // Call Cloud Function (server-side FCM send)
      const result = await sendDeviceCommand(deviceId, command);
      if (generation !== generationRef.current) return;
      const commandId = result.commandId;

      setUiState('sent');

      // Write audit log
      await writeAuditLog({
        title: `Device ${command === 'LOCK' ? 'Locked' : command === 'UNLOCK' ? 'Unlocked' : 'Synced'}`,
        description: `Admin issued ${command} command to device ${deviceId}.`,
        category: 'Command',
        performedBy: admin.email,
        deviceId,
      });
      if (generation !== generationRef.current) return;

      // Terminal acknowledgements cancel this timer. Do not read render-captured uiState here.
      timeoutRef.current = setTimeout(() => {
        if (generation !== generationRef.current) return;
        setUiState('offline_queued');
        cleanup();
      }, COMMAND_TIMEOUT_MS);

      // Subscribe to command acknowledgement
      const unsubscribe = subscribeToCommand(
        commandId,
        (cmd) => {
          if (generation !== generationRef.current) return;
          if (!cmd) return;
          setLastCommand(cmd);
          if (cmd.status === 'SUCCESS') {
            setUiState('success');
            cleanup();
          } else if (cmd.status === 'FAILED') {
            setUiState('failed');
            setErrorMessage(cmd.errorMessage ?? 'Command failed on device');
            cleanup();
          }
        },
        (_err) => {
          if (generation !== generationRef.current) return;
          setUiState('failed');
          setErrorMessage('Failed to track command status');
          cleanup();
        }
      );
      // A cached terminal snapshot may arrive before subscribeToCommand returns.
      if (generation === generationRef.current) unsubRef.current = unsubscribe;
      else unsubscribe();
    } catch (err: unknown) {
      if (generation !== generationRef.current) return;
      cleanup();
      const errorObj = err as { code?: string; message?: string };
      const code = errorObj.code ?? '';
      const msg = errorObj.message ?? 'Unknown error';
      setUiState('failed');

      console.error('[useDeviceCommand] Command API call error:', { code, message: msg });

      if (code === 'NETWORK_ERROR' || msg.includes('Failed to fetch') || msg.includes('network error')) {
        setErrorMessage(msg.includes('Worker URL:') ? msg : `Command API network error. ${msg}`);
      } else if (code === 'CONFIG_ERROR') {
        setErrorMessage('VITE_COMMAND_API_BASE_URL is not configured');
      } else if (code === 'NO_FCM_TOKEN' || msg.includes('NO_FCM_TOKEN') || msg.includes('no registered FCM token')) {
        setErrorMessage('Unable to send command: this device has no registered FCM token.');
      } else if (code === 'ADMIN_DOC_MISSING') {
        setErrorMessage(`Permission denied: admin profile not found for your account (admins/<your-uid>). ${msg}`);
      } else if (code === 'ROLE_MISMATCH') {
        setErrorMessage(`Permission denied: account role not authorized. ${msg}`);
      } else if (code === 'ADMIN_AUTH_FAILED') {
        setErrorMessage(`Authorization check failed: ${msg}`);
      } else if (code === 'FORBIDDEN') {
        setErrorMessage(`Permission denied: ${msg}`);
      } else if (code === 'UNAUTHENTICATED' || msg.includes('UNAUTHENTICATED')) {
        setErrorMessage('Authentication session expired. Please sign in again.');
      } else if (code === 'DEVICE_NOT_FOUND' || msg.includes('DEVICE_NOT_FOUND')) {
        setErrorMessage('Device not found.');
      } else if (code === 'FCM_SEND_FAILED' || msg.includes('FCM_SEND_FAILED')) {
        setErrorMessage(`FCM Send Failed: ${msg}`);
      } else {
        setErrorMessage(`Command API error: ${code ? `${code} — ` : ''}${msg}`);
      }
    }
  }

  function resetState() {
    cleanup();
    setUiState('idle');
    setLastCommand(null);
    setErrorMessage(null);
  }

  return { uiState, lastCommand, errorMessage, issueCommand, resetState };
}

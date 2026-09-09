// Devices Firestore service
// Real-time listeners on devices collection. NO demo data.

import {
  collection,
  doc,
  onSnapshot,
  query,
  orderBy,
  updateDoc,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './firebase';
import type { Device, NormalizedDevice } from '../types';
import { toEpochMs, isOnline, daysRemaining, computeSubscriptionStatus, DEFAULT_ONLINE_THRESHOLD_MS } from '../lib/dateUtils';

// ---------------------------------------------------------------
// Normalize raw Firestore device document
// ---------------------------------------------------------------

export function normalizeDevice(
  raw: Record<string, unknown>,
  id: string,
  onlineThresholdMs: number = DEFAULT_ONLINE_THRESHOLD_MS
): NormalizedDevice {
  const lastSeenMs = toEpochMs(raw.lastSeen as Device['lastSeen']);
  const subscriptionEndMs = toEpochMs(raw.subscriptionEnd as Device['subscriptionEnd']);
  const subscriptionStartMs = toEpochMs(raw.subscriptionStart as Device['subscriptionStart']);
  const lastSyncMs = toEpochMs(raw.lastSync as Device['lastSync']);
  const firstSeenMs = toEpochMs(raw.firstSeen as Device['firstSeen']);
  const installationDate = typeof raw.installationDate === 'number' ? raw.installationDate : 0;
  const activationDate = typeof raw.activationDate === 'number' ? raw.activationDate : 0;
  
  const online = isOnline(lastSeenMs, onlineThresholdMs);
  const days = daysRemaining(subscriptionEndMs);
  const status = raw.status as string ?? 'Active';
  const computedStatus = computeSubscriptionStatus(subscriptionEndMs, status) as NormalizedDevice['computedStatus'];
  const adminLocked = Boolean(raw.adminLocked ?? raw.admin_locked ?? raw.isLocked ?? false);

  return {
    deviceId: id,
    phoneModel: (raw.phoneModel as string) ?? '',
    manufacturer: (raw.manufacturer as string) ?? '',
    androidVersion: (raw.androidVersion as string) ?? '',
    appVersion: (raw.appVersion as string) ?? '',
    appBuildNumber: raw.appBuildNumber as number | undefined,
    userName: (raw.userName as string) ?? '',
    businessName: (raw.businessName as string) ?? '',
    phoneNumber: (raw.phoneNumber as string) ?? '',
    installationDate,
    activationDate,
    subscriptionStart: subscriptionStartMs,
    subscriptionEnd: subscriptionEndMs,
    daysRemaining: days,
    status,
    lastSeen: lastSeenMs,
    lastSync: lastSyncMs,
    firstSeen: firstSeenMs,
    fcmToken: (raw.fcmToken as string) ?? '',
    isOnline: online,
    generatedUuid: (raw.generatedUuid as string) ?? '',
    computedStatus,
    hasFcmToken: !!(raw.fcmToken as string),
    adminLocked,
  };
}

// ---------------------------------------------------------------
// Real-time listener: all devices
// ---------------------------------------------------------------

export function subscribeToDevices(
  onData: (devices: NormalizedDevice[]) => void,
  onError: (error: Error) => void,
  onlineThresholdMs: number = DEFAULT_ONLINE_THRESHOLD_MS
): Unsubscribe {
  const q = query(collection(db, 'devices'), orderBy('lastSeen', 'desc'));
  
  return onSnapshot(
    q,
    (snapshot) => {
      if (snapshot.empty) {
        onData([]);
        return;
      }
      const devices = snapshot.docs.map((docSnap) =>
        normalizeDevice(docSnap.data(), docSnap.id, onlineThresholdMs)
      );
      onData(devices);
    },
    (error) => {
      console.error('[devices] Firestore error:', error.message);
      onError(error);
    }
  );
}

// ---------------------------------------------------------------
// Real-time listener: single device
// ---------------------------------------------------------------

export function subscribeToDevice(
  deviceId: string,
  onData: (device: NormalizedDevice | null) => void,
  onError: (error: Error) => void,
  onlineThresholdMs: number = DEFAULT_ONLINE_THRESHOLD_MS
): Unsubscribe {
  return onSnapshot(
    doc(db, 'devices', deviceId),
    (snap) => {
      if (!snap.exists()) {
        onData(null);
        return;
      }
      onData(normalizeDevice(snap.data(), snap.id, onlineThresholdMs));
    },
    (error) => {
      console.error('[device] Firestore error:', error.message);
      onError(error);
    }
  );
}

// ---------------------------------------------------------------
// Update device subscription fields (admin action)
// CRITICAL: Never touches admin_locked or lock-related fields
// ---------------------------------------------------------------

export async function updateDeviceSubscription(
  deviceId: string,
  newExpiryMs: number,
  newStatus: string
): Promise<void> {
  const docRef = doc(db, 'devices', deviceId);
  await updateDoc(docRef, {
    subscriptionEnd: newExpiryMs,
    status: newStatus,
    lastSync: Date.now(),
  });
}

// ---------------------------------------------------------------
// Update device status only (Activate/Suspend)
// CRITICAL: Never touches admin_locked or FCM-related fields
// ---------------------------------------------------------------

export async function updateDeviceStatus(
  deviceId: string,
  status: string
): Promise<void> {
  const docRef = doc(db, 'devices', deviceId);
  await updateDoc(docRef, {
    status,
    lastSync: Date.now(),
  });
}

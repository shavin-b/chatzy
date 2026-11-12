// Subscriptions service
// Reads subscription history and updates device subscription data

import {
  collection,
  doc,
  onSnapshot,
  query,
  orderBy,
  limit,
  setDoc,
  addDoc,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './firebase';
import type { SubscriptionRecord, LogEntry } from '../types';
import { updateDeviceSubscription, updateDeviceStatus } from './devices';

// ---------------------------------------------------------------
// Normalize subscription record
// ---------------------------------------------------------------

function normalizeSubscription(
  data: Record<string, unknown>,
  id: string
): SubscriptionRecord {
  return {
    subscriptionId: id,
    deviceId: (data.deviceId as string) ?? '',
    businessName: (data.businessName as string) ?? '',
    updatedBy: (data.updatedBy as string) ?? '',
    updatedAt: (data.updatedAt as number) ?? 0,
    oldExpiryDate: (data.oldExpiryDate as number) ?? 0,
    newExpiryDate: (data.newExpiryDate as number) ?? 0,
    daysAdded: (data.daysAdded as number) ?? 0,
    actionType: (data.actionType as string) ?? '',
  };
}

// ---------------------------------------------------------------
// Real-time listener: all subscription records
// ---------------------------------------------------------------

export function subscribeToSubscriptions(
  onData: (subs: SubscriptionRecord[]) => void,
  onError: (error: Error) => void,
  pageSize: number = 100
): Unsubscribe {
  const q = query(
    collection(db, 'subscriptions'),
    orderBy('updatedAt', 'desc'),
    limit(pageSize)
  );
  return onSnapshot(
    q,
    (snapshot) => {
      const subs = snapshot.docs.map((d) =>
        normalizeSubscription(d.data(), d.id)
      );
      onData(subs);
    },
    (error) => {
      console.error('[subscriptions] Firestore error:', error.message);
      onError(error);
    }
  );
}

// ---------------------------------------------------------------
// Extend subscription
// CRITICAL: Only modifies subscription fields. Never touches admin_locked.
// ---------------------------------------------------------------

export async function extendSubscription(params: {
  deviceId: string;
  businessName: string;
  currentExpiryMs: number | null;
  daysToAdd: number;
  adminEmail: string;
  adminUid: string;
  newStatus: string;
}): Promise<void> {
  const { deviceId, businessName, currentExpiryMs, daysToAdd, adminEmail, newStatus } = params;
  const base = (currentExpiryMs ?? 0) > Date.now() ? (currentExpiryMs ?? Date.now()) : Date.now();
  const newExpiryMs = base + daysToAdd * 24 * 60 * 60 * 1000;
  const now = Date.now();

  // 1. Update device subscription fields (subscription data only)
  await updateDeviceSubscription(deviceId, newExpiryMs, newStatus);

  // 2. Record subscription change log
  const subRecord: SubscriptionRecord = {
    subscriptionId: `sub_${deviceId}_${now}`,
    deviceId,
    businessName,
    updatedBy: adminEmail,
    updatedAt: now,
    oldExpiryDate: currentExpiryMs ?? 0,
    newExpiryDate: newExpiryMs,
    daysAdded: daysToAdd,
    actionType: 'Extend',
  };
  await setDoc(doc(db, 'subscriptions', subRecord.subscriptionId), subRecord);

  // 3. Audit log
  await writeAuditLog({
    title: 'Subscription Extended',
    description: `Extended subscription for ${businessName} by +${daysToAdd} days.`,
    category: 'Subscription',
    performedBy: adminEmail,
    deviceId,
  });
}

// ---------------------------------------------------------------
// Set subscription status (Activate / Suspend)
// ---------------------------------------------------------------

export async function setSubscriptionStatus(params: {
  deviceId: string;
  businessName: string;
  status: 'Active' | 'Suspended';
  adminEmail: string;
  currentExpiryMs: number | null;
}): Promise<void> {
  const { deviceId, businessName, status, adminEmail, currentExpiryMs } = params;
  const now = Date.now();

  await updateDeviceStatus(deviceId, status);

  const subRecord: SubscriptionRecord = {
    subscriptionId: `sub_${deviceId}_${now}`,
    deviceId,
    businessName,
    updatedBy: adminEmail,
    updatedAt: now,
    oldExpiryDate: currentExpiryMs ?? 0,
    newExpiryDate: currentExpiryMs ?? 0,
    daysAdded: 0,
    actionType: status === 'Suspended' ? 'Suspend' : 'Activate',
  };
  await setDoc(doc(db, 'subscriptions', subRecord.subscriptionId), subRecord);

  await writeAuditLog({
    title: `Device ${status === 'Suspended' ? 'Suspended' : 'Activated'}`,
    description: `${businessName} subscription status set to ${status}.`,
    category: 'Subscription',
    performedBy: adminEmail,
    deviceId,
  });
}

// ---------------------------------------------------------------
// Write audit log entry to logs collection
// ---------------------------------------------------------------

export async function writeAuditLog(entry: Omit<LogEntry, 'logId' | 'timestamp'>): Promise<void> {
  try {
    const logDoc: LogEntry = {
      logId: '',
      timestamp: Date.now(),
      ...entry,
    };
    const ref = await addDoc(collection(db, 'logs'), logDoc);
    // Update with the actual logId
    await setDoc(ref, { logId: ref.id }, { merge: true });
  } catch (err) {
    console.error('[logs] Failed to write audit log:', err);
  }
}

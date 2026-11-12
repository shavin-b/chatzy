import { Firestore } from 'firebase-admin/firestore';
import { HttpsError } from 'firebase-functions/v2/https';
import { canSendDeviceCommand } from './authorization';
export async function recordDeviceCommand(db: Firestore, commandId: string, record: Record<string, unknown>): Promise<void> {
  await db.runTransaction(async tx => {
    const deviceRef = db.doc(`devices/${record.deviceId}`);
    const [actor, device] = await Promise.all([tx.get(db.doc(`admins/${record.adminUid}`)), tx.get(deviceRef)]);
    if (!canSendDeviceCommand(actor.data()?.role)) throw new HttpsError('permission-denied', 'Management role required.');
    if (!device.exists) throw new HttpsError('not-found', 'Device not found.');
    tx.create(db.doc(`deviceCommands/${commandId}`), record);
    if (record.command === 'LOCK' || record.command === 'UNLOCK') {
      const locked = record.command === 'LOCK';
      tx.update(deviceRef, {adminLocked: locked, adminLock: locked, isLocked: locked, locked, lockStatus: locked ? 'LOCKED' : 'UNLOCKED', lockReason: locked ? 'ADMIN' : 'NONE', desiredCommandId: commandId});
    }
  });
}

export async function markDeviceCommandSent(db: Firestore, commandId: string, sentAt: unknown): Promise<void> {
  const ref = db.doc(`deviceCommands/${commandId}`);
  await db.runTransaction(async tx => {
    const current = await tx.get(ref);
    const patch: Record<string, unknown> = {sentAt};
    if (current.data()?.status === 'PENDING') patch.status = 'SENT';
    tx.update(ref, patch);
  });
}

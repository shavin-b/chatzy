import { randomUUID } from 'node:crypto';
import { FieldValue, Firestore } from 'firebase-admin/firestore';
import { Auth } from 'firebase-admin/auth';
import { HttpsError } from 'firebase-functions/v2/https';

export interface EnrollmentCaller { uid: string; anonymous: boolean; }
const lifetimeMs = 30 * 60 * 1000;
function validId(id: unknown): asserts id is string {
  if (typeof id !== 'string' || !/^OF-[A-Za-z0-9-]{1,80}$/.test(id)) throw new HttpsError('invalid-argument', 'Invalid existing device ID.');
}
function installation(caller: EnrollmentCaller): void {
  if (!caller.uid || !caller.anonymous) throw new HttpsError('permission-denied', 'Use a dedicated anonymous installation identity.');
}
function newDevice(deviceId: string): Record<string, unknown> {
  return {deviceId, generatedUuid: deviceId, firstSeen: Date.now(), subscriptionStatus: 'SUSPENDED', status: 'Suspended', subscriptionEnd: null, adminLocked: false, lockStatus: 'UNLOCKED', lockReason: 'NONE'};
}
export class DeviceEnrollmentService {
  constructor(private db: Firestore, private auth: Auth) {}
  async request(caller: EnrollmentCaller, deviceId: unknown): Promise<Record<string, unknown>> {
    installation(caller); validId(deviceId);
    const uidRef = this.db.doc(`deviceEnrollments/${caller.uid}`);
    const requestId = randomUUID();
    return this.db.runTransaction(async tx => {
      const current = await tx.get(uidRef);
      const data = current.data();
      if (data?.deviceId && data.deviceId !== deviceId) throw new HttpsError('permission-denied', 'Installation is already assigned to another device.');
      if (data?.state === 'APPROVED') return {requestId: data.requestId, state: 'APPROVED', deviceId};
      if (data?.requestId && data.expiresAt > Date.now()) return {requestId: data.requestId, state: 'PENDING', deviceId};
      const record = {requestId, authUid: caller.uid, deviceId, state: 'PENDING', createdAt: Date.now(), expiresAt: Date.now() + lifetimeMs};
      tx.create(this.db.doc(`deviceEnrollmentRequests/${requestId}`), record);
      tx.set(uidRef, record);
      return {requestId, state: 'PENDING', deviceId};
    });
  }
  async approve(adminUid: string, requestId: unknown): Promise<Record<string, unknown>> {
    if (typeof requestId !== 'string' || !/^[a-f0-9-]{36}$/.test(requestId)) throw new HttpsError('invalid-argument', 'Invalid pairing request.');
    return this.db.runTransaction(async tx => {
      const admin = await tx.get(this.db.doc(`admins/${adminUid}`));
      if (admin.data()?.role !== 'Super Admin') throw new HttpsError('permission-denied', 'Super Admin required.');
      const requestRef = this.db.doc(`deviceEnrollmentRequests/${requestId}`);
      const snapshot = await tx.get(requestRef); const request = snapshot.data();
      if (!request) throw new HttpsError('not-found', 'Pairing request not found.');
      if (request.state !== 'PENDING' || request.expiresAt <= Date.now()) throw new HttpsError('failed-precondition', 'Pairing request expired or already processed.');
      validId(request.deviceId);
      const bindingRef = this.db.doc(`deviceBindings/${request.deviceId}`);
      const uidRef = this.db.doc(`deviceEnrollments/${request.authUid}`);
      const deviceRef = this.db.doc(`devices/${request.deviceId}`);
      const [binding, uid, device] = await Promise.all([tx.get(bindingRef), tx.get(uidRef), tx.get(deviceRef)]);
      if (binding.exists && binding.data()?.authUid !== request.authUid) throw new HttpsError('already-exists', 'Device belongs to another installation. Supervised recovery required.');
      if (binding.exists && binding.data()?.active !== true) throw new HttpsError('failed-precondition', 'Revoked binding requires supervised recovery.');
      if (uid.data()?.requestId !== requestId || uid.data()?.deviceId !== request.deviceId) throw new HttpsError('failed-precondition', 'Superseded pairing request.');
      tx.set(bindingRef, {authUid: request.authUid, deviceId: request.deviceId, active: true, approvedBy: adminUid, approvedAt: Date.now()});
      tx.update(uidRef, {state: 'APPROVED'}); tx.update(requestRef, {state: 'APPROVED', approvedBy: adminUid});
      // Existing records are NEVER merged with defaults: subscription/lock/history survive.
      if (!device.exists) tx.create(deviceRef, newDevice(request.deviceId));
      return {state: 'APPROVED', deviceId: request.deviceId};
    });
  }
  async complete(caller: EnrollmentCaller, deviceId: unknown): Promise<Record<string, unknown>> {
    installation(caller); validId(deviceId);
    // Completion is the authenticated registration point. A provisioned record without
    // lastSeen is invisible to the console's orderBy(lastSeen) query until this commits.
    await this.db.runTransaction(async tx => {
      const deviceRef = this.db.doc(`devices/${deviceId}`);
      const [binding, enrollment, device] = await Promise.all([
        tx.get(this.db.doc(`deviceBindings/${deviceId}`)),
        tx.get(this.db.doc(`deviceEnrollments/${caller.uid}`)), tx.get(deviceRef)
      ]);
      if (binding.data()?.authUid !== caller.uid || binding.data()?.active !== true || enrollment.data()?.state !== 'APPROVED' || enrollment.data()?.deviceId !== deviceId) throw new HttpsError('failed-precondition', 'Physical-device approval required.');
      if (device.exists) tx.update(deviceRef, {lastSeen: FieldValue.serverTimestamp()});
      else tx.create(deviceRef, {...newDevice(deviceId), lastSeen: FieldValue.serverTimestamp()});
    });
    const user = await this.auth.getUser(caller.uid);
    await this.auth.setCustomUserClaims(caller.uid, {...user.customClaims, device_id: deviceId});
    // Rules ALSO check the live binding, so revocation during issuance cannot restore access.
    return {state: 'APPROVED', deviceId};
  }
}

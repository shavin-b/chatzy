/**
 * OrderFlow Admin — Firebase Cloud Functions
 *
 * SECURITY: This file runs server-side only.
 * FCM credentials, Admin SDK service account keys, and OAuth tokens
 * are NEVER exposed to browser code.
 *
 * Project: order-flow-34826
 */

import * as admin from 'firebase-admin';
import { onCall, HttpsError, CallableRequest } from 'firebase-functions/v2/https';
import { v4 as uuidv4 } from 'uuid';
import { canSendDeviceCommand } from './authorization';
import { DeviceEnrollmentService, EnrollmentCaller } from './enrollment';
import { recordDeviceCommand, markDeviceCommandSent } from './commandAuthority';

// ---------------------------------------------------------------
// Initialize Firebase Admin SDK
// When deployed to Firebase Functions, credentials are injected
// automatically via the runtime environment.
// ---------------------------------------------------------------

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ---------------------------------------------------------------
// Allowed commands
// ---------------------------------------------------------------

const ALLOWED_COMMANDS = ['LOCK', 'UNLOCK', 'SYNC'] as const;
type AllowedCommand = (typeof ALLOWED_COMMANDS)[number];

// ---------------------------------------------------------------
// Admin role check helper
// ---------------------------------------------------------------

async function verifyAdminRole(uid: string): Promise<{ name: string; email: string; role: string }> {
  console.log(`[sendDeviceCommand] Verifying admin role for UID: ${uid}`);
  const adminDoc = await db.collection('admins').doc(uid).get();
  if (!adminDoc.exists) {
    console.error(`[sendDeviceCommand] Admin document not found in admins/${uid}`);
    throw new HttpsError('permission-denied', 'User is not an authorized administrator.');
  }
  const data = adminDoc.data()!;
  const role: string = data.role ?? '';
  if (!canSendDeviceCommand(role)) {
    console.error(`[sendDeviceCommand] Unauthorized role "${role}" for UID: ${uid}`);
    throw new HttpsError('permission-denied', `Role "${role}" is not authorized to perform this action.`);
  }
  console.log(`[sendDeviceCommand] Admin role verified: ${role} for ${data.email ?? uid}`);
  return {
    name: data.name ?? 'Admin',
    email: data.email ?? '',
    role,
  };
}

// ---------------------------------------------------------------
// sendDeviceCommand — HTTPS Callable Function
//
// Called by: Frontend (after admin confirms action)
// Does NOT expose FCM credentials to browser.
// ---------------------------------------------------------------

interface SendCommandRequest {
  deviceId: string;
  command: AllowedCommand;
}

interface SendCommandResponse {
  commandId: string;
  status: string;
  message: string;
}

export const sendDeviceCommand = onCall(
  {
    region: 'us-central1',
    enforceAppCheck: false,
  },
  async (request: CallableRequest<SendCommandRequest>): Promise<SendCommandResponse> => {
    console.log('[sendDeviceCommand] Function invocation started');

    // 1. Authentication check
    if (!request.auth) {
      console.error('[sendDeviceCommand] Unauthenticated caller invocation rejected');
      throw new HttpsError('unauthenticated', 'You must be signed in to send commands.');
    }

    const callerUid = request.auth.uid;
    const callerEmail = request.auth.token.email ?? '';
    console.log(`[sendDeviceCommand] Caller authenticated: ${callerUid} (${callerEmail})`);

    // 2. Validate request payload
    const { deviceId, command } = request.data ?? {};

    if (!deviceId || typeof deviceId !== 'string' || deviceId.trim() === '') {
      console.error('[sendDeviceCommand] Invalid request payload: deviceId is missing or empty');
      throw new HttpsError('invalid-argument', 'deviceId is required.');
    }

    if (!ALLOWED_COMMANDS.includes(command)) {
      console.error(`[sendDeviceCommand] Invalid command requested: ${command}`);
      throw new HttpsError('invalid-argument', `command must be one of: ${ALLOWED_COMMANDS.join(', ')}`);
    }

    console.log(`[sendDeviceCommand] Processing command: ${command} for deviceId: ${deviceId.trim()}`);

    // 3. Verify admin role (server-side authorization)
    let adminProfile: Awaited<ReturnType<typeof verifyAdminRole>>;
    try {
      adminProfile = await verifyAdminRole(callerUid);
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      throw new HttpsError('permission-denied', 'Failed to verify admin authorization.');
    }

    // 4. Load device document
    console.log(`[sendDeviceCommand] Loading device document: devices/${deviceId.trim()}`);
    const deviceRef = db.collection('devices').doc(deviceId.trim());
    const deviceSnap = await deviceRef.get();

    if (!deviceSnap.exists) {
      console.error(`[sendDeviceCommand] Device document devices/${deviceId.trim()} not found`);
      throw new HttpsError('not-found', `Device "${deviceId}" not found.`);
    }

    const deviceData = deviceSnap.data()!;
    const fcmToken: string = deviceData.fcmToken ?? '';

    // 5. Validate FCM token (never log the actual token string!)
    if (!fcmToken || fcmToken.trim() === '') {
      console.error(`[sendDeviceCommand] Device ${deviceId.trim()} has no registered FCM token`);
      throw new HttpsError(
        'failed-precondition',
        'Unable to send lock command because this device has no registered FCM token.'
      );
    }
    console.log(`[sendDeviceCommand] FCM token present for deviceId: ${deviceId.trim()}`);

    // 6. Generate unique commandId
    const commandId = uuidv4();
    const now = admin.firestore.Timestamp.now();

    // 7. Create command record with PENDING status
    const commandRecord = {
      commandId,
      deviceId: deviceId.trim(),
      command,
      status: 'PENDING',
      createdAt: now,
      sentAt: null,
      processedAt: null,
      errorMessage: null,
      adminUid: callerUid,
      adminEmail: callerEmail,
    };

    console.log(`[sendDeviceCommand] Creating command document deviceCommands/${commandId}`);
    await recordDeviceCommand(db, commandId, commandRecord);

    // 8. Send FCM data message
    console.log(`[sendDeviceCommand] Attempting FCM message dispatch for commandId: ${commandId}`);
    try {
      await messaging.send({
        token: fcmToken,
        data: {
          command,
          deviceId: deviceId.trim(),
          commandId,
        },
        android: {
          priority: 'high',
          ttl: 86400 * 1000, // 24 hours TTL in milliseconds
        },
      });

      console.log(`[sendDeviceCommand] FCM message dispatch successful for commandId: ${commandId}`);

      // 9. Update status to SENT
      await markDeviceCommandSent(db, commandId, admin.firestore.Timestamp.now());

      // 10. Write audit log
      const logId = `log_${callerUid}_${Date.now()}`;
      await db.collection('logs').doc(logId).set({
        logId,
        timestamp: Date.now(),
        title: `Remote ${command === 'LOCK' ? 'Lock' : command === 'UNLOCK' ? 'Unlock' : 'Sync'} Command`,
        description: `Admin "${adminProfile.name}" sent ${command} command to device ${deviceId}.`,
        category: 'Command',
        performedBy: callerEmail,
        deviceId: deviceId.trim(),
        commandId,
        result: 'SENT',
      });

      return {
        commandId,
        status: 'SENT',
        message: `Command ${command} sent to device ${deviceId}. Awaiting acknowledgement.`,
      };
    } catch (fcmError: unknown) {
      const errMsg = (fcmError as { message?: string })?.message ?? 'FCM send failed';
      console.error(`[sendDeviceCommand] FCM send failed for commandId: ${commandId} — Error: ${errMsg}`);

      await db.collection('deviceCommands').doc(commandId).update({
        status: 'FAILED',
        errorMessage: `FCM delivery failed: ${errMsg}`,
        sentAt: admin.firestore.Timestamp.now(),
      });

      const fcmErrCode = (fcmError as { code?: string })?.code ?? '';
      if (
        fcmErrCode === 'messaging/registration-token-not-registered' ||
        fcmErrCode === 'messaging/invalid-registration-token'
      ) {
        throw new HttpsError(
          'failed-precondition',
          'The FCM token for this device is invalid or expired. The device may need to re-register.'
        );
      }

      throw new HttpsError('internal', `Failed to send FCM message: ${errMsg}`);
    }
  }
);

// ---------------------------------------------------------------
// getAdminRole — HTTPS Callable Function
// ---------------------------------------------------------------

export const getAdminRole = onCall(
  { region: 'us-central1' },
  async (request: CallableRequest): Promise<{ role: string | null; name: string | null }> => {
    if (!request.auth) {
      throw new HttpsError('unauthenticated', 'Authentication required.');
    }

    try {
      const adminDoc = await db.collection('admins').doc(request.auth.uid).get();
      if (!adminDoc.exists) {
        return { role: null, name: null };
      }
      const data = adminDoc.data()!;
      return { role: data.role ?? null, name: data.name ?? null };
    } catch {
      throw new HttpsError('internal', 'Failed to retrieve admin role.');
    }
  }
);

// Enrollment is owner-approved: a client-provided OF- ID never establishes ownership.
const enrollment = new DeviceEnrollmentService(db, admin.auth());
function installationCaller(request: CallableRequest): EnrollmentCaller {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Installation authentication required.');
  return {uid: request.auth.uid, anonymous: request.auth.token.firebase?.sign_in_provider === 'anonymous'};
}
export const requestDeviceEnrollment = onCall({region: 'us-central1'}, request => enrollment.request(installationCaller(request), request.data?.deviceId));
export const completeDeviceEnrollment = onCall({region: 'us-central1'}, request => enrollment.complete(installationCaller(request), request.data?.deviceId));
export const approveDeviceEnrollment = onCall({region: 'us-central1'}, request => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Admin authentication required.');
  return enrollment.approve(request.auth.uid, request.data?.requestId);
});
export const listDeviceEnrollmentRequests = onCall({region: 'us-central1'}, async request => {
  if (!request.auth || (await db.doc(`admins/${request.auth.uid}`).get()).data()?.role !== 'Super Admin') throw new HttpsError('permission-denied', 'Super Admin required.');
  const requests = await db.collection('deviceEnrollmentRequests').where('state', '==', 'PENDING').where('expiresAt', '>', Date.now()).orderBy('expiresAt').limit(100).get();
  return {requests: requests.docs.map(doc => doc.data()).filter(data => data.expiresAt > Date.now())};
});

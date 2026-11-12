import type { Env, DeviceCommandRequest, CommandType, ExecutionContext } from './types';
import { verifyFirebaseIdToken } from './auth';
import {
  getAdminProfileFromFirestore,
  getDeviceFromFirestore,
  createDeviceCommandInFirestore,
  updateDeviceCommandStatusInFirestore,
  createAuditLogInFirestore,
  updateDeviceLockStateInFirestore,
} from './firestore';
import { sendFcmMessage } from './fcm';
import { configurationError } from './config';

const ALLOWED_COMMANDS: CommandType[] = ['LOCK', 'UNLOCK', 'SYNC'];

function getCorsHeaders(request: Request, env: Env): HeadersInit {
  const origin = request.headers.get('Origin') || '';
  const allowedOrigins = (env.ALLOWED_ORIGINS || 'http://localhost:3001,http://localhost:5173,http://localhost:3000,http://127.0.0.1:3001,http://127.0.0.1:5173')
    .split(',')
    .map((o) => o.trim());

  const isLocalhost = /^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/.test(origin);
  const isAllowed = isLocalhost || allowedOrigins.includes(origin) || allowedOrigins.includes('*');
  const allowedOriginHeader = isAllowed ? origin : allowedOrigins[0] || 'http://localhost:3001';

  return {
    'Access-Control-Allow-Origin': allowedOriginHeader,
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Max-Age': '86400',
  };
}

function jsonResponse(data: unknown, status = 200, corsHeaders: HeadersInit = {}): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...corsHeaders,
    },
  });
}

export default {
  async fetch(request: Request, env: Env, _ctx: ExecutionContext): Promise<Response> {
    const corsHeaders = getCorsHeaders(request, env);
    const url = new URL(request.url);
    const configError = configurationError(env);
    if (configError) {
      return jsonResponse({ error: 'CONFIG_ERROR', message: configError }, 503, corsHeaders);
    }

    // 1. Handle CORS Options Preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    // 2. Health Endpoint
    if (request.method === 'GET' && url.pathname === '/health') {
      return jsonResponse(
        { status: 'ok', service: 'chatzy-device-command-api' },
        200,
        corsHeaders
      );
    }

    // 3. API Endpoint: POST /api/device-command
    if (request.method === 'POST' && url.pathname === '/api/device-command') {
      console.log('[Worker] Incoming POST /api/device-command request');

      // A. Authorization Header Check
      const authHeader = request.headers.get('Authorization');
      if (!authHeader || !authHeader.startsWith('Bearer ')) {
        console.error('[Worker] Missing or invalid Authorization header');
        return jsonResponse(
          { error: 'UNAUTHENTICATED', message: 'Authentication required.' },
          401,
          corsHeaders
        );
      }

      const idToken = authHeader.substring(7).trim();

      // B. Verify Firebase ID Token
      let verifiedToken: Awaited<ReturnType<typeof verifyFirebaseIdToken>>;
      try {
        const projectId = env.FIREBASE_PROJECT_ID;
        verifiedToken = await verifyFirebaseIdToken(idToken, projectId);
        console.log(`[Worker] Caller authenticated: ${verifiedToken.uid} (${verifiedToken.email})`);
      } catch (err: any) {
        console.error('[Worker] Token verification failed:', err?.message || err);
        return jsonResponse(
          { error: 'UNAUTHENTICATED', message: 'Authentication required.' },
          401,
          corsHeaders
        );
      }

      // C. Verify Admin Role in Firestore admins/{uid}
      let adminProfile: Awaited<ReturnType<typeof getAdminProfileFromFirestore>>;
      try {
        adminProfile = await getAdminProfileFromFirestore(env, verifiedToken.uid);
      } catch (err: any) {
        console.error('[Worker] Admin profile lookup error:', err?.message || err);
        return jsonResponse(
          { error: 'ADMIN_AUTH_FAILED', message: `Failed to verify administrative authorization: ${err?.message || 'Profile lookup failed'}` },
          403,
          corsHeaders
        );
      }

      if (!adminProfile) {
        console.error(`[Worker] Admin profile not found for UID: ${verifiedToken.uid}`);
        return jsonResponse(
          { error: 'ADMIN_DOC_MISSING', message: `Admin profile not found for UID: ${verifiedToken.uid}` },
          403,
          corsHeaders
        );
      }

      // Support role is READ ONLY — must not be allowed to send commands
      if (adminProfile.role !== 'Super Admin' && adminProfile.role !== 'Admin') {
        console.error(`[Worker] Unauthorized role "${adminProfile.role}" for UID: ${verifiedToken.uid}`);
        return jsonResponse(
          { error: 'ROLE_MISMATCH', message: `Account role not authorized: ${adminProfile.role}` },
          403,
          corsHeaders
        );
      }

      console.log(`[Worker] Admin role verified: ${adminProfile.role} for ${verifiedToken.email}`);

      // D. Parse & Validate Payload
      let body: DeviceCommandRequest;
      try {
        body = (await request.json()) as DeviceCommandRequest;
      } catch {
        return jsonResponse(
          { error: 'INVALID_COMMAND', message: 'Invalid JSON body.' },
          400,
          corsHeaders
        );
      }

      const { deviceId, command } = body || {};

      if (!deviceId || typeof deviceId !== 'string' || deviceId.trim() === '') {
        return jsonResponse(
          { error: 'INVALID_COMMAND', message: 'deviceId is required.' },
          400,
          corsHeaders
        );
      }

      if (!ALLOWED_COMMANDS.includes(command)) {
        console.error(`[Worker] Unsupported command requested: ${command}`);
        return jsonResponse(
          { error: 'INVALID_COMMAND', message: 'Unsupported command.' },
          400,
          corsHeaders
        );
      }

      const cleanDeviceId = deviceId.trim();
      console.log(`[Worker] Processing command: ${command} for deviceId: ${cleanDeviceId}`);

      // E. Load Device Document
      let device: Awaited<ReturnType<typeof getDeviceFromFirestore>>;
      try {
        device = await getDeviceFromFirestore(env, cleanDeviceId);
      } catch (err: any) {
        console.error(`[Worker] Device lookup failed for ${cleanDeviceId}:`, err?.message || err);
        return jsonResponse(
          { error: 'DEVICE_NOT_FOUND', message: 'Device not found.' },
          404,
          corsHeaders
        );
      }

      if (!device) {
        console.error(`[Worker] Device ${cleanDeviceId} not found in Firestore`);
        return jsonResponse(
          { error: 'DEVICE_NOT_FOUND', message: 'Device not found.' },
          404,
          corsHeaders
        );
      }

      // F. Validate FCM Token Presence
      if (!device.fcmToken || device.fcmToken.trim() === '') {
        console.error(`[Worker] Device ${cleanDeviceId} has no registered FCM token`);
        return jsonResponse(
          { error: 'NO_FCM_TOKEN', message: 'No FCM token registered for this device.' },
          409,
          corsHeaders
        );
      }

      console.log(`[Worker] FCM token present for deviceId: ${cleanDeviceId}`);

      // G. Generate Command Document
      const commandId = crypto.randomUUID();
      console.log(`[Worker] Created commandId: ${commandId}`);

      try {
        await createDeviceCommandInFirestore(env, {
          commandId,
          deviceId: cleanDeviceId,
          command,
          status: 'PENDING',
          adminUid: verifiedToken.uid,
          adminEmail: verifiedToken.email,
        });
      } catch (err: any) {
        console.error(`[Worker] Failed to write command document:`, err?.message || err);
        return jsonResponse(
          { error: 'INTERNAL_ERROR', message: 'Failed to record command request.' },
          500,
          corsHeaders
        );
      }

      // H. Dispatch FCM Message
      const fcmResult = await sendFcmMessage(
        env,
        device.fcmToken,
        command,
        cleanDeviceId,
        commandId
      );

      const logId = `log_${verifiedToken.uid}_${Date.now()}`;

      if (fcmResult.success) {
        await updateDeviceCommandStatusInFirestore(env, commandId, 'SENT');
        if (command === 'LOCK') {
          await updateDeviceLockStateInFirestore(env, cleanDeviceId, true);
        } else if (command === 'UNLOCK') {
          await updateDeviceLockStateInFirestore(env, cleanDeviceId, false);
        }
        await createAuditLogInFirestore(env, {
          logId,
          title: `Remote ${command === 'LOCK' ? 'Lock' : command === 'UNLOCK' ? 'Unlock' : 'Sync'} Command`,
          description: `Admin "${adminProfile.name}" sent ${command} command to device ${cleanDeviceId}.`,
          category: 'Command',
          performedBy: verifiedToken.email,
          deviceId: cleanDeviceId,
          commandId,
          result: 'SENT',
        });

        return jsonResponse(
          {
            commandId,
            status: 'SENT',
            message: `Command ${command} sent to device ${cleanDeviceId}. Awaiting acknowledgement.`,
          },
          200,
          corsHeaders
        );
      } else {
        await updateDeviceCommandStatusInFirestore(
          env,
          commandId,
          'FAILED',
          fcmResult.errorMessage || 'FCM delivery failed'
        );
        await createAuditLogInFirestore(env, {
          logId,
          title: `Remote ${command === 'LOCK' ? 'Lock' : command === 'UNLOCK' ? 'Unlock' : 'Sync'} Command`,
          description: `Failed to send ${command} command to device ${cleanDeviceId}.`,
          category: 'Command',
          performedBy: verifiedToken.email,
          deviceId: cleanDeviceId,
          commandId,
          result: 'FAILED',
        });

        return jsonResponse(
          { error: 'FCM_SEND_FAILED', message: 'Unable to send command to the device.' },
          502,
          corsHeaders
        );
      }
    }

    // Default 404 for unknown endpoints
    return jsonResponse({ error: 'NOT_FOUND', message: 'Endpoint not found.' }, 404, corsHeaders);
  },
};

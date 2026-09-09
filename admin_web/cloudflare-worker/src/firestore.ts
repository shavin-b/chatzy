import type { Env, AdminRoleDoc, FirestoreDevice } from './types';
import { getGoogleOAuthToken } from './firebase';

const FIRESTORE_BASE_URL = 'https://firestore.googleapis.com/v1/projects';

// Helper to extract string from Firestore REST field
function getStringField(fields: any, name: string): string | undefined {
  if (!fields || !fields[name]) return undefined;
  return fields[name].stringValue;
}

// ---------------------------------------------------------------
// Get Admin Role from admins/{uid}
// ---------------------------------------------------------------
export async function getAdminProfileFromFirestore(
  env: Env,
  uid: string
): Promise<AdminRoleDoc | null> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const url = `${FIRESTORE_BASE_URL}/${projectId}/databases/(default)/documents/admins/${uid}`;

  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    const errText = await res.text();
    console.error(`[getAdminProfile] Firestore REST error ${res.status}:`, errText);
    throw new Error(`Firestore REST API error: ${res.status}`);
  }

  const doc = (await res.json()) as any;
  const fields = doc.fields || {};

  return {
    name: getStringField(fields, 'name') || 'Admin',
    email: getStringField(fields, 'email') || '',
    role: getStringField(fields, 'role') || 'Support',
  };
}

// ---------------------------------------------------------------
// Get Device from devices/{deviceId}
// ---------------------------------------------------------------
export async function getDeviceFromFirestore(
  env: Env,
  deviceId: string
): Promise<FirestoreDevice | null> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const url = `${FIRESTORE_BASE_URL}/${projectId}/databases/(default)/documents/devices/${deviceId}`;

  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 404) {
    return null;
  }

  if (!res.ok) {
    const errText = await res.text();
    console.error(`[getDevice] Firestore REST error ${res.status}:`, errText);
    throw new Error(`Firestore REST API error: ${res.status}`);
  }

  const doc = (await res.json()) as any;
  const fields = doc.fields || {};

  return {
    deviceId: getStringField(fields, 'deviceId') || deviceId,
    fcmToken: getStringField(fields, 'fcmToken'),
    phoneModel: getStringField(fields, 'phoneModel'),
    businessName: getStringField(fields, 'businessName'),
    userName: getStringField(fields, 'userName'),
  };
}

// ---------------------------------------------------------------
// Create Command Document deviceCommands/{commandId}
// ---------------------------------------------------------------
export async function createDeviceCommandInFirestore(
  env: Env,
  commandRecord: {
    commandId: string;
    deviceId: string;
    command: string;
    status: string;
    adminUid: string;
    adminEmail: string;
  }
): Promise<void> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const url = `${FIRESTORE_BASE_URL}/${projectId}/databases/(default)/documents/deviceCommands?documentId=${commandRecord.commandId}`;

  const body = {
    fields: {
      commandId: { stringValue: commandRecord.commandId },
      deviceId: { stringValue: commandRecord.deviceId },
      command: { stringValue: commandRecord.command },
      status: { stringValue: commandRecord.status },
      createdAt: { timestampValue: new Date().toISOString() },
      sentAt: { nullValue: null },
      processedAt: { nullValue: null },
      errorMessage: { nullValue: null },
      adminUid: { stringValue: commandRecord.adminUid },
      adminEmail: { stringValue: commandRecord.adminEmail },
    },
  };

  const res = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const errText = await res.text();
    console.error(`[createDeviceCommand] Firestore REST error ${res.status}:`, errText);
    throw new Error(`Failed to create command record: ${res.status}`);
  }
}

// ---------------------------------------------------------------
// Update Command Document Status in deviceCommands/{commandId}
// ---------------------------------------------------------------
export async function updateDeviceCommandStatusInFirestore(
  env: Env,
  commandId: string,
  status: 'SENT' | 'FAILED',
  errorMessage?: string
): Promise<void> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const updateMask = status === 'SENT' ? 'updateMask.fieldPaths=status&updateMask.fieldPaths=sentAt' : 'updateMask.fieldPaths=status&updateMask.fieldPaths=sentAt&updateMask.fieldPaths=errorMessage';
  const url = `${FIRESTORE_BASE_URL}/${projectId}/databases/(default)/documents/deviceCommands/${commandId}?${updateMask}`;

  const fields: any = {
    status: { stringValue: status },
    sentAt: { timestampValue: new Date().toISOString() },
  };

  if (errorMessage) {
    fields.errorMessage = { stringValue: errorMessage };
  }

  const res = await fetch(url, {
    method: 'PATCH',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ fields }),
  });

  if (!res.ok) {
    const errText = await res.text();
    console.error(`[updateDeviceCommandStatus] Firestore REST error ${res.status}:`, errText);
  }
}

// ---------------------------------------------------------------
// Write Audit Log Entry to logs/{logId}
// ---------------------------------------------------------------
export async function createAuditLogInFirestore(
  env: Env,
  logRecord: {
    logId: string;
    title: string;
    description: string;
    category: string;
    performedBy: string;
    deviceId: string;
    commandId: string;
    result: string;
  }
): Promise<void> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const url = `${FIRESTORE_BASE_URL}/${projectId}/databases/(default)/documents/logs?documentId=${logRecord.logId}`;

  const body = {
    fields: {
      logId: { stringValue: logRecord.logId },
      timestamp: { integerValue: String(Date.now()) },
      title: { stringValue: logRecord.title },
      description: { stringValue: logRecord.description },
      category: { stringValue: logRecord.category },
      performedBy: { stringValue: logRecord.performedBy },
      deviceId: { stringValue: logRecord.deviceId },
      commandId: { stringValue: logRecord.commandId },
      result: { stringValue: logRecord.result },
    },
  };

  try {
    await fetch(url, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
  } catch (err) {
    console.error('[createAuditLog] Error writing log:', err);
  }
}

// ---------------------------------------------------------------
// Update adminLocked field on devices/{deviceId}
// ---------------------------------------------------------------
export async function updateDeviceLockStateInFirestore(
  env: Env,
  deviceId: string,
  adminLocked: boolean
): Promise<void> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const url = `${FIRESTORE_BASE_URL}/${projectId}/databases/(default)/documents/devices/${deviceId}?updateMask.fieldPaths=adminLocked`;

  const fields = {
    adminLocked: { booleanValue: adminLocked },
  };

  try {
    const res = await fetch(url, {
      method: 'PATCH',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ fields }),
    });

    if (!res.ok) {
      const errText = await res.text();
      console.error(`[updateDeviceLockState] Firestore REST error ${res.status}:`, errText);
    }
  } catch (err) {
    console.error('[updateDeviceLockState] Error updating device lock state:', err);
  }
}

import type { Env, CommandType } from './types';
import { getGoogleOAuthToken } from './firebase';

export async function sendFcmMessage(
  env: Env,
  fcmToken: string,
  command: CommandType,
  deviceId: string,
  commandId: string
): Promise<{ success: boolean; errorMessage?: string }> {
  const token = await getGoogleOAuthToken(env);
  const projectId = env.FIREBASE_PROJECT_ID;
  const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;

  const payload = {
    message: {
      token: fcmToken,
      data: {
        command,
        deviceId,
        commandId,
      },
      android: {
        priority: 'HIGH',
        ttl: '86400s',
      },
    },
  };

  console.log(`[sendFcmMessage] Attempting FCM HTTP v1 dispatch for commandId: ${commandId}`);

  const res = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (res.ok) {
    console.log(`[sendFcmMessage] FCM HTTP v1 dispatch SUCCESS for commandId: ${commandId}`);
    return { success: true };
  }

  console.error(`[sendFcmMessage] FCM dispatch failed; commandId: ${commandId}, status: ${res.status}`);
  return {
    success: false,
    errorMessage: `FCM HTTP v1 returned ${res.status}`,
  };
}

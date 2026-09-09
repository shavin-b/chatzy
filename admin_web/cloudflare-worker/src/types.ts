export interface Env {
  FIREBASE_PROJECT_ID: string;
  FIREBASE_SERVICE_ACCOUNT_EMAIL: string;
  FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY: string;
  ALLOWED_ORIGINS: string;
}

export interface ExecutionContext {
  waitUntil(promise: Promise<unknown>): void;
  passThroughOnException(): void;
}

export type CommandType = 'LOCK' | 'UNLOCK' | 'SYNC';

export interface DeviceCommandRequest {
  deviceId: string;
  command: CommandType;
}

export interface VerifiedFirebaseToken {
  uid: string;
  email: string;
  iss: string;
  aud: string;
  exp: number;
  iat: number;
  auth_time: number;
}

export interface AdminRoleDoc {
  name: string;
  email: string;
  role: 'Super Admin' | 'Admin' | 'Support' | string;
}

export interface FirestoreDevice {
  deviceId: string;
  fcmToken?: string;
  phoneModel?: string;
  businessName?: string;
  userName?: string;
}

export interface CommandApiResponse {
  commandId: string;
  status: 'SENT' | 'PENDING' | 'FAILED';
  message: string;
}

export interface ApiErrorResponse {
  error: string;
  message: string;
}

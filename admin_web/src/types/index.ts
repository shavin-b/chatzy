// ============================================================
// OrderFlow Admin Web Dashboard — TypeScript Types
// Mirrors exact Firestore field names from Android project
// ============================================================

import { Timestamp } from 'firebase/firestore';

// ------------------------------------------------------------------
// Admin / Auth
// ------------------------------------------------------------------

export type AdminRole = 'Super Admin' | 'Admin' | 'Support';

export interface AdminUser {
  adminId: string;
  email: string;
  name: string;
  role: AdminRole;
  lastLogin: number; // epoch ms
  avatarUrl?: string | null;
}

// ------------------------------------------------------------------
// Device
// ------------------------------------------------------------------

export type DeviceStatus =
  | 'Active'
  | 'Expiring Soon'
  | 'Expired'
  | 'Suspended'
  | 'Offline'
  | 'Uninstalled';

/** Exact Firestore field names from devices/{deviceId} */
export interface Device {
  deviceId: string;
  phoneModel: string;
  manufacturer: string;
  androidVersion: string;
  appVersion: string;
  appBuildNumber?: number;
  userName: string;
  businessName: string;
  phoneNumber: string;
  installationDate: number; // epoch ms
  activationDate: number;   // epoch ms
  subscriptionStart: number | Timestamp | null; // handles both types
  subscriptionEnd: number | Timestamp | null;   // handles both types
  daysRemaining: number;
  status: DeviceStatus | string;
  lastSeen: number | Timestamp | null;   // handles both types
  lastSync: number | Timestamp | null;   // handles both types
  firstSeen: number | Timestamp | null;  // handles both types
  fcmToken: string;
  isOnline?: boolean; // computed locally, also stored by Admin Android app
  generatedUuid: string;
  adminLocked?: boolean; // Set by remote LOCK / UNLOCK commands
}

/** Normalized device with all timestamps as epoch ms */
export interface NormalizedDevice extends Omit<Device, 'subscriptionStart' | 'subscriptionEnd' | 'lastSeen' | 'lastSync' | 'firstSeen'> {
  subscriptionStart: number | null;
  subscriptionEnd: number | null;
  lastSeen: number | null;
  lastSync: number | null;
  firstSeen: number | null;
  isOnline: boolean;
  daysRemaining: number;
  computedStatus: DeviceStatus;
  hasFcmToken: boolean;
  adminLocked: boolean;
}

// ------------------------------------------------------------------
// Device Command
// ------------------------------------------------------------------

export type CommandType = 'LOCK' | 'UNLOCK' | 'SYNC';
export type CommandStatus = 'PENDING' | 'SENT' | 'RECEIVED' | 'SUCCESS' | 'FAILED';

/** Exact fields from deviceCommands/{commandId} */
export interface DeviceCommand {
  commandId: string;
  deviceId: string;
  command: CommandType;
  status: CommandStatus;
  createdAt: number | Timestamp | null;  // NEW field (web dashboard adds this)
  sentAt?: number | Timestamp | null;    // NEW field (web dashboard adds this)
  processedAt?: number | Timestamp | null; // Written by Android
  errorMessage?: string | null;
  adminUid?: string;   // NEW field (web dashboard adds this)
  adminEmail?: string; // NEW field (web dashboard adds this)
}

// ------------------------------------------------------------------
// Subscription
// ------------------------------------------------------------------

/** From subscriptions/{subscriptionId} */
export interface SubscriptionRecord {
  subscriptionId: string;
  deviceId: string;
  businessName: string;
  updatedBy: string;
  updatedAt: number; // epoch ms
  oldExpiryDate: number;
  newExpiryDate: number;
  daysAdded: number;
  actionType: string; // 'Extend', 'Activate', 'Suspend'
}

// ------------------------------------------------------------------
// Log / Audit
// ------------------------------------------------------------------

/** From logs/{logId} */
export interface LogEntry {
  logId: string;
  timestamp: number; // epoch ms
  title: string;
  description: string;
  category: string;
  performedBy: string;
  deviceId?: string | null;
}

// ------------------------------------------------------------------
// Settings
// ------------------------------------------------------------------

/** From settings/global */
export interface AdminSettings {
  companyName: string;
  companyLogoUrl: string;
  supportEmail: string;
  supportPhone: string;
  defaultSubscriptionDays: number;
  notificationTemplates: string[];
  themeMode: string;
  autoBackupEnabled: boolean;
  onlineThresholdMinutes?: number; // NEW field added by web dashboard
}

// ------------------------------------------------------------------
// Dashboard Stats
// ------------------------------------------------------------------

export interface DashboardStats {
  totalDevices: number;
  onlineDevices: number;
  offlineDevices: number;
  activeSubscriptions: number;
  expiringSoon: number;
  expired: number;
  suspended: number;
}

// ------------------------------------------------------------------
// UI State
// ------------------------------------------------------------------

export type CommandUIState =
  | 'idle'
  | 'confirming'
  | 'pending'
  | 'sent'
  | 'success'
  | 'failed'
  | 'offline_queued';

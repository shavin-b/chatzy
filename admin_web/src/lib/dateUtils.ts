// Date and time utilities
// Online threshold: 17 minutes by default (configurable via settings)

import { Timestamp } from 'firebase/firestore';

export const DEFAULT_ONLINE_THRESHOLD_MS = 17 * 60 * 1000; // 17 minutes

// ---------------------------------------------------------------
// Convert Firestore value to epoch ms
// Handles: null, number (epoch ms), Firebase Timestamp
// ---------------------------------------------------------------

export function toEpochMs(value: number | Timestamp | null | undefined): number | null {
  if (value == null) return null;
  if (typeof value === 'number') return value;
  if (value instanceof Timestamp) return value.toMillis();
  // Handle plain object with seconds/nanoseconds (from Firestore snapshots)
  if (typeof value === 'object' && 'seconds' in value) {
    return (value as { seconds: number }).seconds * 1000;
  }
  return null;
}

// ---------------------------------------------------------------
// Online/Offline detection
// ---------------------------------------------------------------

export function isOnline(
  lastSeenMs: number | null,
  thresholdMs: number = DEFAULT_ONLINE_THRESHOLD_MS
): boolean {
  if (lastSeenMs == null) return false;
  return Date.now() - lastSeenMs < thresholdMs;
}

// ---------------------------------------------------------------
// Human-readable relative time
// ---------------------------------------------------------------

export function timeAgo(ms: number | null): string {
  if (ms == null) return 'Never';
  const diff = Date.now() - ms;
  const seconds = Math.floor(diff / 1000);
  if (seconds < 5) return 'Just now';
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return 'Yesterday';
  if (days < 7) return `${days}d ago`;
  return formatDate(ms);
}

// ---------------------------------------------------------------
// Format date as "Aug 13, 2026"
// ---------------------------------------------------------------

export function formatDate(ms: number | null): string {
  if (ms == null) return 'N/A';
  return new Date(ms).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

// ---------------------------------------------------------------
// Format date + time
// ---------------------------------------------------------------

export function formatDateTime(ms: number | null): string {
  if (ms == null) return 'N/A';
  return new Date(ms).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ---------------------------------------------------------------
// Format time only (12:35 PM)
// ---------------------------------------------------------------

export function formatTime(ms: number | null): string {
  if (ms == null) return 'N/A';
  return new Date(ms).toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

// ---------------------------------------------------------------
// Days remaining from epoch ms expiry
// ---------------------------------------------------------------

export function daysRemaining(expiryMs: number | null): number {
  if (expiryMs == null || expiryMs === 0) return 0;
  const diff = expiryMs - Date.now();
  if (diff <= 0) return 0;
  return Math.ceil(diff / (1000 * 60 * 60 * 24));
}

// ---------------------------------------------------------------
// Add N days to epoch ms (or now if base is in the past)
// ---------------------------------------------------------------

export function addDays(baseMs: number, days: number): number {
  const base = baseMs > Date.now() ? baseMs : Date.now();
  return base + days * 24 * 60 * 60 * 1000;
}

// ---------------------------------------------------------------
// Compute device subscription status label
// ---------------------------------------------------------------

export function computeSubscriptionStatus(
  expiryMs: number | null,
  currentStatus: string
): string {
  if (currentStatus === 'Suspended') return 'Suspended';
  if (expiryMs == null || expiryMs === 0) return 'Active';
  const days = daysRemaining(expiryMs);
  if (days <= 0) return 'Expired';
  if (days <= 7) return 'Expiring Soon';
  return 'Active';
}

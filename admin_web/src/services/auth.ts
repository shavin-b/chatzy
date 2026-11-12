// Authentication service
// Handles Firebase Auth + admin role verification from Firestore admins/{uid}

import {
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  onAuthStateChanged,
  type User,
} from 'firebase/auth';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { auth, db } from './firebase';
import type { AdminUser, AdminRole } from '../types';

// ---------------------------------------------------------------
// Login
// ---------------------------------------------------------------

export async function signIn(email: string, password: string): Promise<void> {
  await signInWithEmailAndPassword(auth, email, password);
}

// ---------------------------------------------------------------
// Logout
// ---------------------------------------------------------------

export async function signOut(): Promise<void> {
  await firebaseSignOut(auth);
}

// ---------------------------------------------------------------
// Get admin profile + role from Firestore
// Returns null if user is not in admins collection
// ---------------------------------------------------------------

export async function getAdminProfile(uid: string): Promise<AdminUser | null> {
  try {
    const snap = await getDoc(doc(db, 'admins', uid));
    if (!snap.exists()) return null;
    const data = snap.data();
    // Existence of a profile alone does not confer an administrator role.
    if (!['Super Admin', 'Admin', 'Support'].includes(data.role)) return null;
    return {
      adminId: snap.id,
      email: data.email ?? '',
      name: data.name ?? 'Admin',
      role: data.role as AdminRole,
      lastLogin: data.lastLogin ?? 0,
      avatarUrl: data.avatarUrl ?? null,
    };
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------
// Update last login timestamp
// ---------------------------------------------------------------

export async function updateLastLogin(uid: string): Promise<void> {
  try {
    await setDoc(
      doc(db, 'admins', uid),
      { lastLogin: Date.now() },
      { merge: true }
    );
  } catch {
    // Non-critical — don't throw
  }
}

// ---------------------------------------------------------------
// Auth state listener
// ---------------------------------------------------------------

export function onAuthChanged(callback: (user: User | null) => void): () => void {
  return onAuthStateChanged(auth, callback);
}

// ---------------------------------------------------------------
// Role helpers
// ---------------------------------------------------------------

export function canWrite(role: AdminRole | undefined): boolean {
  return role === 'Super Admin' || role === 'Admin';
}

export function isSuperAdmin(role: AdminRole | undefined): boolean {
  return role === 'Super Admin';
}

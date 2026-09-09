// Admins service — reads/writes admins collection

import {
  collection,
  doc,
  onSnapshot,
  query,
  orderBy,
  updateDoc,
  type Unsubscribe,
} from 'firebase/firestore';
import { db } from './firebase';
import type { AdminUser } from '../types';

function normalizeAdmin(data: Record<string, unknown>, id: string): AdminUser {
  return {
    adminId: id,
    email: (data.email as string) ?? '',
    name: (data.name as string) ?? 'Admin',
    role: (data.role as AdminUser['role']) ?? 'Support',
    lastLogin: (data.lastLogin as number) ?? 0,
    avatarUrl: (data.avatarUrl as string | null) ?? null,
  };
}

export function subscribeToAdmins(
  onData: (admins: AdminUser[]) => void,
  onError: (error: Error) => void
): Unsubscribe {
  const q = query(collection(db, 'admins'), orderBy('name', 'asc'));
  return onSnapshot(
    q,
    (snapshot) => {
      const admins = snapshot.docs.map((d) => normalizeAdmin(d.data(), d.id));
      onData(admins);
    },
    (error) => {
      console.error('[admins] Firestore error:', error.message);
      onError(error);
    }
  );
}

export async function updateAdminProfile(
  adminId: string,
  updates: Partial<Pick<AdminUser, 'name' | 'role'>>
): Promise<void> {
  await updateDoc(doc(db, 'admins', adminId), updates);
}

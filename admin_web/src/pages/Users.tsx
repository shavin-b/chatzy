// Users page — admin user management (reads from admins collection)

import { useState, useEffect } from 'react';
import { Users, Shield, Clock, Edit2, XCircle } from 'lucide-react';
import { subscribeToAdmins, updateAdminProfile } from '../services/admins';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../components/Toast';
import { formatDateTime } from '../lib/dateUtils';
import { isSuperAdmin } from '../services/auth';
import type { AdminUser } from '../types';
import { cn } from '../lib/cn';

const ROLE_OPTIONS: AdminUser['role'][] = ['Super Admin', 'Admin', 'Support'];

export function UsersPage() {
  const { admin } = useAuth();
  const { toast } = useToast();
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editRole, setEditRole] = useState<AdminUser['role']>('Support');
  const [savingId, setSavingId] = useState<string | null>(null);

  const canEditRoles = isSuperAdmin(admin?.role);

  useEffect(() => {
    const unsub = subscribeToAdmins(
      (data) => { setAdmins(data); setLoading(false); },
      (_err) => { setError('Unable to load users.'); setLoading(false); }
    );
    return unsub;
  }, []);

  async function saveRole(adminId: string) {
    setSavingId(adminId);
    try {
      await updateAdminProfile(adminId, { role: editRole });
      toast({ type: 'success', title: 'Role updated successfully.' });
      setEditingId(null);
    } catch {
      toast({ type: 'error', title: 'Failed to update role.' });
    } finally {
      setSavingId(null);
    }
  }

  const roleColors: Record<string, string> = {
    'Super Admin': 'text-amber-400 bg-amber-500/10 ring-amber-500/30',
    'Admin': 'text-teal-400 bg-teal-500/10 ring-teal-500/30',
    'Support': 'text-slate-400 bg-slate-500/10 ring-slate-500/30',
  };

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Users</h1>
          <p className="text-sm text-slate-500">Admin console user accounts</p>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden">
        {/* Header */}
        <div className="hidden grid-cols-[2fr_1fr_1fr_1fr_auto] gap-4 border-b border-slate-800 px-5 py-3 text-xs font-medium uppercase tracking-wider text-slate-500 lg:grid">
          <span>Admin</span>
          <span>Role</span>
          <span>Last Login</span>
          <span>Status</span>
          <span />
        </div>

        {loading ? (
          <div className="p-4 space-y-3">
            {Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-14 skeleton rounded-xl" />)}
          </div>
        ) : error ? (
          <div className="flex items-center gap-3 p-4 text-sm text-red-300">
            <XCircle className="h-4 w-4 text-red-400" />
            {error}
          </div>
        ) : admins.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-12 text-center">
            <Users className="h-10 w-10 text-slate-700" />
            <p className="text-sm text-slate-500">No admin accounts found.</p>
            <p className="text-xs text-slate-600">Add admin accounts in the Firebase Console → Authentication.</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-800">
            {admins.map((a) => (
              <div
                key={a.adminId}
                className="grid grid-cols-1 gap-3 px-5 py-4 lg:grid-cols-[2fr_1fr_1fr_1fr_auto] lg:items-center lg:gap-4"
              >
                {/* User info */}
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-slate-800 text-sm font-bold text-slate-400">
                    {a.name.charAt(0).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-200 truncate">{a.name}</p>
                    <p className="text-xs text-slate-500 truncate">{a.email}</p>
                  </div>
                  {a.adminId === admin?.adminId && (
                    <span className="rounded-full bg-teal-500/10 px-2 py-0.5 text-xs text-teal-400 ring-1 ring-teal-500/30">
                      You
                    </span>
                  )}
                </div>

                {/* Role */}
                {editingId === a.adminId && canEditRoles ? (
                  <div className="flex items-center gap-2">
                    <select
                      value={editRole}
                      onChange={(e) => setEditRole(e.target.value as AdminUser['role'])}
                      className="rounded-lg border border-slate-700 bg-slate-800 px-2 py-1 text-xs text-slate-300 outline-none"
                    >
                      {ROLE_OPTIONS.map((r) => <option key={r} value={r}>{r}</option>)}
                    </select>
                    <button
                      onClick={() => saveRole(a.adminId)}
                      disabled={savingId === a.adminId}
                      className="rounded-lg bg-teal-600 px-2 py-1 text-xs font-semibold text-white hover:bg-teal-500 disabled:opacity-50"
                    >
                      Save
                    </button>
                    <button
                      onClick={() => setEditingId(null)}
                      className="text-xs text-slate-500 hover:text-slate-300"
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <span className={cn('inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ring-1', roleColors[a.role] ?? roleColors['Support'])}>
                    <Shield className="h-3 w-3" />
                    {a.role}
                  </span>
                )}

                {/* Last login */}
                <span className="flex items-center gap-1.5 text-xs text-slate-500">
                  <Clock className="h-3.5 w-3.5" />
                  {a.lastLogin ? formatDateTime(a.lastLogin) : 'Never'}
                </span>

                {/* Status */}
                <span className="text-xs text-emerald-400">Active</span>

                {/* Actions */}
                {canEditRoles && editingId !== a.adminId && a.adminId !== admin?.adminId && (
                  <button
                    onClick={() => { setEditingId(a.adminId); setEditRole(a.role); }}
                    className="flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-2.5 py-1.5 text-xs text-slate-400 transition hover:bg-slate-700 hover:text-slate-200"
                  >
                    <Edit2 className="h-3 w-3" />
                    Edit Role
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Note about user management */}
      <div className="rounded-xl border border-slate-800 bg-slate-900/50 p-4 text-xs text-slate-500">
        <p className="font-medium text-slate-400 mb-1">About Admin Accounts</p>
        <p>Admin accounts are managed in Firebase Authentication + the <code className="font-mono bg-slate-800 px-1 rounded">admins</code> Firestore collection.
        To add a new admin, create the user in Firebase Authentication and add a document to <code className="font-mono bg-slate-800 px-1 rounded">admins/{'{uid}'}</code> with their name, email, and role.</p>
      </div>
    </div>
  );
}

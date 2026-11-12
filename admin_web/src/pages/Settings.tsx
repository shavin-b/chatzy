import { DeviceEnrollmentApproval } from '../components/DeviceEnrollmentApproval';
// Settings page — admin profile, online threshold, Firebase status

import { useState, useEffect } from 'react';
import { doc, onSnapshot, setDoc } from 'firebase/firestore';
import { db, firebaseProjectId } from '../services/firebase';
import { brand } from '../config/brand';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../components/Toast';
import { Settings, Wifi, CheckCircle, AlertCircle, Save } from 'lucide-react';
import { isSuperAdmin } from '../services/auth';
import type { AdminSettings } from '../types';

const DEFAULT_SETTINGS: AdminSettings = {
  companyName: brand.company,
  companyLogoUrl: '',
  supportEmail: brand.supportEmail,
  supportPhone: brand.supportPhone,
  defaultSubscriptionDays: 30,
  notificationTemplates: [],
  themeMode: 'System',
  autoBackupEnabled: true,
  onlineThresholdMinutes: 17,
};

export function SettingsPage() {
  const { admin } = useAuth();
  const { toast } = useToast();
  const canEdit = isSuperAdmin(admin?.role);

  const [settings, setSettings] = useState<AdminSettings>(DEFAULT_SETTINGS);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [fbConnected, setFbConnected] = useState<boolean | null>(null);

  useEffect(() => {
    const unsub = onSnapshot(
      doc(db, 'settings', 'global'),
      (snap) => {
        if (snap.exists()) {
          setSettings({ ...DEFAULT_SETTINGS, ...(snap.data() as AdminSettings) });
        }
        setLoading(false);
        setFbConnected(true);
      },
      () => {
        setFbConnected(false);
        setLoading(false);
      }
    );
    return unsub;
  }, []);

  async function handleSave() {
    if (!canEdit) return;
    setSaving(true);
    try {
      await setDoc(doc(db, 'settings', 'global'), settings, { merge: true });
      toast({ type: 'success', title: 'Settings saved.' });
    } catch {
      toast({ type: 'error', title: 'Failed to save settings.' });
    } finally {
      setSaving(false);
    }
  }

  function Field({
    label,
    id,
    value,
    onChange,
    type = 'text',
    disabled,
  }: {
    label: string;
    id: string;
    value: string | number;
    onChange: (v: string) => void;
    type?: string;
    disabled?: boolean;
  }) {
    return (
      <div>
        <label htmlFor={id} className="mb-1.5 block text-xs font-medium text-slate-400">{label}</label>
        <input
          id={id}
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled || !canEdit}
          className="w-full rounded-xl border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm text-slate-300 outline-none transition focus:border-teal-500/50 disabled:opacity-60 disabled:cursor-not-allowed"
        />
      </div>
    );
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Settings</h1>
          <p className="text-sm text-slate-500">Admin console configuration</p>
        </div>
        {canEdit && (
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-2 rounded-xl bg-teal-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-teal-500 disabled:opacity-50"
          >
            {saving ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            Save Settings
          </button>
        )}
      </div>

      <DeviceEnrollmentApproval />
      {/* Firebase connection status */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
        <h3 className="mb-4 text-xs font-semibold uppercase tracking-wider text-slate-500">Firebase Status</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="flex items-center gap-3 rounded-xl bg-slate-800 p-3">
            <Wifi className="h-5 w-5 text-teal-400" />
            <div>
              <p className="text-xs text-slate-500">Project</p>
              <p className="font-mono text-xs font-semibold text-slate-300">{firebaseProjectId}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-xl bg-slate-800 p-3">
            {fbConnected === null ? (
              <span className="h-4 w-4 animate-spin rounded-full border-2 border-teal-500/30 border-t-teal-500" />
            ) : fbConnected ? (
              <CheckCircle className="h-5 w-5 text-emerald-400" />
            ) : (
              <AlertCircle className="h-5 w-5 text-red-400" />
            )}
            <div>
              <p className="text-xs text-slate-500">Connection</p>
              <p className="text-xs font-semibold text-slate-300">
                {fbConnected === null ? 'Checking…' : fbConnected ? 'Connected' : 'Disconnected'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-xl bg-slate-800 p-3">
            <Settings className="h-5 w-5 text-slate-400" />
            <div>
              <p className="text-xs text-slate-500">Admin Console</p>
              <p className="text-xs font-semibold text-slate-300">v{brand.version}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Admin profile */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
        <h3 className="mb-4 text-xs font-semibold uppercase tracking-wider text-slate-500">Your Profile</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <p className="text-xs text-slate-500 mb-1">Name</p>
            <p className="text-sm font-medium text-slate-200">{admin?.name ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500 mb-1">Email</p>
            <p className="text-sm text-slate-200">{admin?.email ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500 mb-1">Role</p>
            <p className="text-sm font-semibold text-amber-400">{admin?.role ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-slate-500 mb-1">Admin ID</p>
            <p className="font-mono text-xs text-slate-500">{admin?.adminId ?? '—'}</p>
          </div>
        </div>
      </div>

      {/* Global settings */}
      {loading ? (
        <div className="h-64 skeleton rounded-2xl" />
      ) : (
        <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
          <h3 className="mb-4 text-xs font-semibold uppercase tracking-wider text-slate-500">
            Application Settings
            {!canEdit && <span className="ml-2 text-slate-600">(Read Only — Super Admin only)</span>}
          </h3>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field
              label="Company Name"
              id="setting-company-name"
              value={settings.companyName}
              onChange={(v) => setSettings((s) => ({ ...s, companyName: v }))}
            />
            <Field
              label="Support Email"
              id="setting-support-email"
              value={settings.supportEmail}
              onChange={(v) => setSettings((s) => ({ ...s, supportEmail: v }))}
              type="email"
            />
            <Field
              label="Support Phone"
              id="setting-support-phone"
              value={settings.supportPhone}
              onChange={(v) => setSettings((s) => ({ ...s, supportPhone: v }))}
            />
            <Field
              label="Default Subscription Days"
              id="setting-default-sub-days"
              value={settings.defaultSubscriptionDays}
              onChange={(v) => setSettings((s) => ({ ...s, defaultSubscriptionDays: parseInt(v) || 30 }))}
              type="number"
            />
            <div>
              <label htmlFor="setting-online-threshold" className="mb-1.5 block text-xs font-medium text-slate-400">
                Online Threshold (minutes)
              </label>
              <input
                id="setting-online-threshold"
                type="number"
                min={1}
                max={60}
                value={settings.onlineThresholdMinutes ?? 17}
                onChange={(e) => setSettings((s) => ({ ...s, onlineThresholdMinutes: parseInt(e.target.value) || 17 }))}
                disabled={!canEdit}
                className="w-full rounded-xl border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm text-slate-300 outline-none transition focus:border-teal-500/50 disabled:opacity-60"
              />
              <p className="mt-1 text-xs text-slate-600">
                Devices are shown as Online if lastSeen is within this many minutes. Current: {settings.onlineThresholdMinutes ?? 17} min.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Security note */}
      <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs text-amber-300/80">
        <p className="font-medium text-amber-300 mb-1">Security Note</p>
        <p>Firebase credentials and service account keys are never exposed in this browser application. FCM commands are sent server-side via Firebase Cloud Functions only.</p>
      </div>
    </div>
  );
}

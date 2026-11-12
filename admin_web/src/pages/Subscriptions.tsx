// Subscriptions page — subscription history + management actions

import { useState, useEffect, useMemo } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CreditCard, Plus } from 'lucide-react';
import { useDevices } from '../hooks/useDevices';
import { subscribeToSubscriptions, extendSubscription, setSubscriptionStatus } from '../services/subscriptions';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../components/Toast';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { StatusBadge } from '../components/StatusBadge';
import { formatDate, formatDateTime, addDays } from '../lib/dateUtils';
import type { SubscriptionRecord, NormalizedDevice } from '../types';
import { canWrite } from '../services/auth';

const EXTEND_OPTIONS = [
  { label: '+7 Days', days: 7 },
  { label: '+30 Days', days: 30 },
  { label: '+90 Days', days: 90 },
  { label: '+180 Days', days: 180 },
  { label: '+365 Days', days: 365 },
];

export function SubscriptionsPage() {
  const { admin } = useAuth();
  const { toast } = useToast();
  const [searchParams] = useSearchParams();
  const preselectedDevice = searchParams.get('device');

  const { devices } = useDevices();
  const [subscriptions, setSubscriptions] = useState<SubscriptionRecord[]>([]);
  const [subLoading, setSubLoading] = useState(true);
  const [subError, setSubError] = useState<string | null>(null);

  const [selectedDevice, setSelectedDevice] = useState<NormalizedDevice | null>(null);
  const [confirmAction, setConfirmAction] = useState<{
    type: 'extend' | 'activate' | 'suspend';
    days?: number;
    device: NormalizedDevice;
  } | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    const unsub = subscribeToSubscriptions(
      (data) => { setSubscriptions(data); setSubLoading(false); },
      (_err) => { setSubError('Unable to load subscription records.'); setSubLoading(false); }
    );
    return unsub;
  }, []);

  // Preselect device from URL param
  useEffect(() => {
    if (preselectedDevice && devices.length > 0) {
      const found = devices.find((d) => d.deviceId === preselectedDevice);
      if (found) setSelectedDevice(found);
    }
  }, [preselectedDevice, devices]);

  const userCanWrite = canWrite(admin?.role);

  async function handleAction() {
    if (!confirmAction || !admin) return;
    setActionLoading(true);
    try {
      const { type, days, device } = confirmAction;
      if (type === 'extend' && days) {
        await extendSubscription({
          deviceId: device.deviceId,
          businessName: device.businessName || device.userName,
          currentExpiryMs: device.subscriptionEnd,
          daysToAdd: days,
          adminEmail: admin.email,
          adminUid: admin.adminId,
          newStatus: 'Active',
        });
        toast({ type: 'success', title: `Subscription extended by +${days} days.` });
      } else if (type === 'activate') {
        await setSubscriptionStatus({ deviceId: device.deviceId, businessName: device.businessName || device.userName, status: 'Active', adminEmail: admin.email, currentExpiryMs: device.subscriptionEnd });
        toast({ type: 'success', title: 'Device subscription activated.' });
      } else if (type === 'suspend') {
        await setSubscriptionStatus({ deviceId: device.deviceId, businessName: device.businessName || device.userName, status: 'Suspended', adminEmail: admin.email, currentExpiryMs: device.subscriptionEnd });
        toast({ type: 'warning', title: 'Device subscription suspended.' });
      }
      setConfirmAction(null);
    } catch (err: unknown) {
      toast({ type: 'error', title: 'Action failed', description: (err as Error)?.message });
    } finally {
      setActionLoading(false);
    }
  }

  const deviceSubRecords = useMemo(() => {
    if (!selectedDevice) return subscriptions;
    return subscriptions.filter((s) => s.deviceId === selectedDevice.deviceId);
  }, [subscriptions, selectedDevice]);

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-white">Subscriptions</h1>
        <p className="text-sm text-slate-500">Manage device subscriptions and view change history</p>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        {/* Device picker */}
        <div className="lg:col-span-1">
          <div className="rounded-2xl border border-slate-800 bg-slate-900">
            <div className="border-b border-slate-800 px-4 py-3">
              <h3 className="text-sm font-semibold text-white">Select Device</h3>
            </div>
            <div className="max-h-96 overflow-y-auto divide-y divide-slate-800">
              {devices.length === 0 ? (
                <p className="p-4 text-sm text-slate-500">No devices registered.</p>
              ) : (
                devices.map((d) => (
                  <button
                    key={d.deviceId}
                    onClick={() => setSelectedDevice(d)}
                    className={`w-full px-4 py-3 text-left transition hover:bg-slate-800/50 ${selectedDevice?.deviceId === d.deviceId ? 'bg-teal-500/10' : ''}`}
                  >
                    <p className="text-sm font-medium text-slate-200 truncate">{d.businessName || d.userName || d.deviceId}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="font-mono text-xs text-slate-500 truncate">{d.deviceId}</span>
                      <StatusBadge status={d.computedStatus} size="sm" />
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Actions + history */}
        <div className="lg:col-span-2 space-y-4">
          {/* Quick actions */}
          {selectedDevice && userCanWrite && (
            <div className="rounded-2xl border border-slate-800 bg-slate-900 p-4">
              <h3 className="mb-3 text-sm font-semibold text-white">
                Actions for{' '}
                <span className="text-teal-400">{selectedDevice.businessName || selectedDevice.deviceId}</span>
              </h3>

              <div className="mb-2 flex items-center gap-2 flex-wrap">
                <StatusBadge status={selectedDevice.computedStatus} size="md" />
                <span className="text-xs text-slate-500">
                  Expires: {formatDate(selectedDevice.subscriptionEnd)} · {selectedDevice.daysRemaining} days remaining
                </span>
              </div>

              {/* Extend buttons */}
              <div className="flex flex-wrap gap-2 mb-3">
                {EXTEND_OPTIONS.map(({ label, days }) => (
                  <button
                    key={days}
                    onClick={() => setConfirmAction({ type: 'extend', days, device: selectedDevice })}
                    className="flex items-center gap-1.5 rounded-xl border border-teal-500/30 bg-teal-500/10 px-3 py-1.5 text-xs font-semibold text-teal-400 transition hover:bg-teal-500/20"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    {label}
                  </button>
                ))}
              </div>

              {/* Status buttons */}
              <div className="flex gap-2">
                {selectedDevice.computedStatus !== 'Active' && (
                  <button
                    onClick={() => setConfirmAction({ type: 'activate', device: selectedDevice })}
                    className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-xs font-semibold text-emerald-400 transition hover:bg-emerald-500/20"
                  >
                    Activate
                  </button>
                )}
                {selectedDevice.computedStatus !== 'Suspended' && (
                  <button
                    onClick={() => setConfirmAction({ type: 'suspend', device: selectedDevice })}
                    className="rounded-xl border border-orange-500/30 bg-orange-500/10 px-3 py-1.5 text-xs font-semibold text-orange-400 transition hover:bg-orange-500/20"
                  >
                    Suspend
                  </button>
                )}
              </div>
            </div>
          )}

          {/* History */}
          <div className="rounded-2xl border border-slate-800 bg-slate-900">
            <div className="border-b border-slate-800 px-4 py-3 flex items-center justify-between">
              <h3 className="text-sm font-semibold text-white">Change History</h3>
              {selectedDevice && (
                <button onClick={() => setSelectedDevice(null)} className="text-xs text-slate-500 hover:text-slate-300">Clear filter</button>
              )}
            </div>

            {subLoading ? (
              <div className="p-4 space-y-2">
                {Array.from({ length: 5 }).map((_, i) => <div key={i} className="h-12 skeleton rounded-xl" />)}
              </div>
            ) : subError ? (
              <div className="p-4 text-sm text-red-300">{subError}</div>
            ) : deviceSubRecords.length === 0 ? (
              <div className="flex flex-col items-center gap-3 py-10 text-center">
                <CreditCard className="h-10 w-10 text-slate-700" />
                <p className="text-sm text-slate-500">No subscription changes recorded yet.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-800">
                {deviceSubRecords.map((sub) => (
                  <div key={sub.subscriptionId} className="px-4 py-3">
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-medium text-slate-200">{sub.actionType} — {sub.businessName}</p>
                        <p className="text-xs text-slate-500">
                          By {sub.updatedBy} · {formatDateTime(sub.updatedAt)}
                        </p>
                      </div>
                      {sub.daysAdded > 0 && (
                        <span className="rounded-lg bg-teal-500/10 px-2 py-1 text-xs font-semibold text-teal-400">
                          +{sub.daysAdded}d
                        </span>
                      )}
                    </div>
                    <div className="mt-1.5 flex items-center gap-2 text-xs text-slate-600">
                      <Link to={`/devices/${encodeURIComponent(sub.deviceId)}`} className="font-mono text-slate-500 hover:text-teal-400 transition">
                        {sub.deviceId}
                      </Link>
                      {sub.newExpiryDate > 0 && (
                        <>
                          <span>·</span>
                          <span>New expiry: {formatDate(sub.newExpiryDate)}</span>
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Confirm dialog */}
      <ConfirmDialog
        isOpen={!!confirmAction}
        onClose={() => setConfirmAction(null)}
        onConfirm={handleAction}
        title={
          confirmAction?.type === 'extend'
            ? `Extend Subscription +${confirmAction.days} Days`
            : confirmAction?.type === 'activate'
            ? 'Activate Subscription'
            : 'Suspend Subscription'
        }
        description={
          confirmAction?.type === 'suspend'
            ? 'This will suspend the subscription. The Android app will disable the AutoResponder.'
            : `This will update the subscription for ${confirmAction?.device.businessName || confirmAction?.device.deviceId}.`
        }
        confirmLabel={
          confirmAction?.type === 'extend' ? `Extend +${confirmAction.days} Days`
          : confirmAction?.type === 'activate' ? 'Activate'
          : 'Suspend'
        }
        confirmVariant={confirmAction?.type === 'suspend' ? 'warning' : 'default'}
        details={confirmAction ? [
          { label: 'Device', value: confirmAction.device.deviceId },
          { label: 'Customer', value: confirmAction.device.businessName || confirmAction.device.userName || '—' },
          { label: 'Current Status', value: confirmAction.device.computedStatus },
          ...(confirmAction.type === 'extend' && confirmAction.days
            ? [{
                label: 'New Expiry',
                value: formatDate(addDays(confirmAction.device.subscriptionEnd ?? 0, confirmAction.days)),
              }]
            : []),
        ] : []}
        loading={actionLoading}
      />
    </div>
  );
}

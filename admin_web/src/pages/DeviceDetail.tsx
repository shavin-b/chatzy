// DeviceDetail page — full device info + LOCK/UNLOCK actions

import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Smartphone,
  Lock,
  Unlock,
  CheckCircle,
  Clock,
  AlertCircle,
  Loader2,
  Copy,
  Check,
} from 'lucide-react';
import { useDevice } from '../hooks/useDevice';
import { useDeviceCommand } from '../hooks/useDeviceCommand';
import { useAuth } from '../hooks/useAuth';
import { StatusBadge, OnlineBadge } from '../components/StatusBadge';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { useToast } from '../components/Toast';
import { timeAgo, formatDate, formatDateTime, toEpochMs } from '../lib/dateUtils';
import { cn } from '../lib/cn';
import { canWrite } from '../services/auth';
import { subscribeToDeviceCommands } from '../services/commands';
import type { CommandType, DeviceCommand } from '../types';

function InfoRow({ label, value, mono = false }: { label: string; value: string | number | null | undefined; mono?: boolean }) {
  return (
    <div className="flex justify-between gap-4 py-2.5 border-b border-slate-800 last:border-0">
      <dt className="text-xs font-medium text-slate-500 flex-shrink-0">{label}</dt>
      <dd className={cn('text-xs text-right', mono ? 'font-mono text-teal-400' : 'text-slate-300')}>
        {value ?? '—'}
      </dd>
    </div>
  );
}

function InfoSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900 p-5">
      <h3 className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-500">{title}</h3>
      <dl>{children}</dl>
    </div>
  );
}

export function DeviceDetailPage() {
  const { deviceId } = useParams<{ deviceId: string }>();
  const navigate = useNavigate();
  const { admin } = useAuth();
  const { toast } = useToast();

  const decodedId = deviceId ? decodeURIComponent(deviceId) : undefined;
  const { device, loading, notFound } = useDevice(decodedId);
  const { uiState, errorMessage: cmdError, issueCommand, resetState } = useDeviceCommand();

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingCommand, setPendingCommand] = useState<CommandType | null>(null);
  const [copied, setCopied] = useState(false);
  const [deviceCommands, setDeviceCommands] = useState<DeviceCommand[]>([]);
  const [commandsLoading, setCommandsLoading] = useState(true);

  useEffect(() => {
    if (!decodedId) return;
    const unsub = subscribeToDeviceCommands(
      decodedId,
      (cmds) => {
        setDeviceCommands(cmds);
        setCommandsLoading(false);
      },
      () => setCommandsLoading(false)
    );
    return unsub;
  }, [decodedId]);

  const canSendCommands = canWrite(admin?.role);
  const commandInFlight = uiState === 'pending' || uiState === 'sent';

  // Show toast when command completes
  useEffect(() => {
    if (uiState === 'success') {
      toast({ type: 'success', title: `Device ${pendingCommand === 'LOCK' ? 'locked' : 'unlocked'} successfully.` });
    } else if (uiState === 'failed') {
      toast({ type: 'error', title: 'Command failed', description: cmdError ?? undefined });
    } else if (uiState === 'offline_queued') {
      toast({
        type: 'warning',
        title: 'Device is offline',
        description: 'The command will be processed when the device reconnects.',
      });
    }
  }, [uiState]);

  function openConfirm(cmd: CommandType) {
    resetState();
    setPendingCommand(cmd);
    setConfirmOpen(true);
  }

  async function handleConfirm() {
    if (!pendingCommand || !decodedId) return;
    setConfirmOpen(false);
    await issueCommand(decodedId, pendingCommand);
  }

  function copyDeviceId() {
    if (decodedId) {
      navigator.clipboard.writeText(decodedId);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-teal-400" />
      </div>
    );
  }

  if (notFound || !device) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-16 text-center">
        <Smartphone className="h-12 w-12 text-slate-700" />
        <h2 className="text-lg font-semibold text-white">Device Not Found</h2>
        <p className="text-sm text-slate-500">
          The requested device <code className="font-mono text-teal-400">{decodedId}</code> does not exist.
        </p>
        <button
          onClick={() => navigate('/devices')}
          className="rounded-xl border border-slate-700 bg-slate-800 px-4 py-2 text-sm font-medium text-slate-300 transition hover:bg-slate-700 hover:text-white"
        >
          Back to Devices
        </button>
      </div>
    );
  }

  const commandStatusLabel: Record<string, string> = {
    idle: '',
    confirming: '',
    pending: 'Sending command request...',
    sent: 'Command sent to device. Awaiting device acknowledgement...',
    success: `Device ${pendingCommand === 'LOCK' ? 'locked' : 'unlocked'} successfully.`,
    failed: cmdError || 'Command failed.',
    offline_queued: 'Device is offline. Command queued.',
  };

  return (
    <div className="p-6 space-y-6">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-xs">
        <button
          onClick={() => navigate('/devices')}
          className="flex items-center gap-1.5 text-sm text-slate-500 transition hover:text-slate-300"
        >
          <ArrowLeft className="h-4 w-4" />
          Devices
        </button>
        <span className="text-slate-700">/</span>
        <span className="font-mono text-sm text-teal-400">{device.deviceId}</span>
      </div>

      {/* Header */}
      <div className="flex flex-wrap items-start gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-800">
          <Smartphone className="h-6 w-6 text-slate-400" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-xl font-bold text-white truncate">
              {device.businessName || device.userName || device.deviceId}
            </h1>
            <OnlineBadge isOnline={device.isOnline} />
            <StatusBadge status={device.computedStatus} size="md" />
            {device.adminLocked && (
              <span className="inline-flex items-center gap-1 rounded-full bg-red-500/15 border border-red-500/30 px-3 py-1 text-xs font-semibold text-red-400">
                <Lock className="h-3 w-3" />
                Admin Locked
              </span>
            )}
          </div>
          <div className="mt-1 flex items-center gap-2">
            <button
              onClick={copyDeviceId}
              className="flex items-center gap-1 font-mono text-xs text-slate-500 transition hover:text-teal-400"
            >
              {device.deviceId}
              {copied ? <Check className="h-3 w-3 text-teal-400" /> : <Copy className="h-3 w-3" />}
            </button>
            <span className="text-slate-700">·</span>
            <span className="text-xs text-slate-500">Last seen {timeAgo(device.lastSeen)}</span>
          </div>
        </div>

        {/* Lock / Unlock buttons */}
        {canSendCommands && (
          <div className="flex gap-2 flex-shrink-0">
            <button
              id="btn-unlock-device"
              onClick={() => openConfirm('UNLOCK')}
              disabled={commandInFlight}
              className="flex items-center gap-2 rounded-xl border border-teal-500/30 bg-teal-500/10 px-4 py-2 text-sm font-semibold text-teal-400 transition hover:bg-teal-500/20 disabled:opacity-50"
            >
              <Unlock className="h-4 w-4" />
              Unlock
            </button>
            <button
              id="btn-lock-device"
              onClick={() => openConfirm('LOCK')}
              disabled={commandInFlight}
              className="flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-500 disabled:opacity-50"
            >
              <Lock className="h-4 w-4" />
              Lock Device
            </button>
          </div>
        )}
      </div>

      {/* Command status bar */}
      {uiState !== 'idle' && uiState !== 'confirming' && (
        <div
          className={cn(
            'flex items-center gap-3 rounded-xl border p-4 text-sm',
            uiState === 'success'
              ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300'
              : uiState === 'failed'
              ? 'border-red-500/30 bg-red-500/10 text-red-300'
              : uiState === 'offline_queued'
              ? 'border-amber-500/30 bg-amber-500/10 text-amber-300'
              : 'border-blue-500/30 bg-blue-500/10 text-blue-300'
          )}
        >
          {(uiState === 'pending' || uiState === 'sent') && (
            <Loader2 className="h-4 w-4 animate-spin flex-shrink-0" />
          )}
          {uiState === 'success' && <CheckCircle className="h-4 w-4 flex-shrink-0" />}
          {uiState === 'failed' && <AlertCircle className="h-4 w-4 flex-shrink-0" />}
          {uiState === 'offline_queued' && <Clock className="h-4 w-4 flex-shrink-0" />}
          {commandStatusLabel[uiState]}
        </div>
      )}

      {/* Info grid */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">
        <InfoSection title="Device Identity">
          <InfoRow label="Device ID" value={device.deviceId} mono />
          <InfoRow label="Generated UUID" value={device.generatedUuid || 'Not set'} mono />
          <InfoRow label="Phone Model" value={`${device.manufacturer} ${device.phoneModel}`} />
          <InfoRow label="Android Version" value={device.androidVersion} />
          <InfoRow label="App Version" value={device.appVersion} />
          {device.appBuildNumber != null && (
            <InfoRow label="Build Number" value={String(device.appBuildNumber)} />
          )}
        </InfoSection>

        <InfoSection title="Subscription">
          <InfoRow label="Status" value={device.computedStatus} />
          <InfoRow label="Start Date" value={formatDate(device.subscriptionStart)} />
          <InfoRow label="Expiry Date" value={formatDate(device.subscriptionEnd)} />
          <InfoRow
            label="Days Remaining"
            value={device.subscriptionEnd ? `${device.daysRemaining} days` : 'N/A'}
          />
          <div className="mt-3">
            <Link
              to={`/subscriptions?device=${encodeURIComponent(device.deviceId)}`}
              className="text-xs font-medium text-teal-400 hover:text-teal-300 transition"
            >
              Manage subscription →
            </Link>
          </div>
        </InfoSection>

        <InfoSection title="Customer">
          <InfoRow label="Business Name" value={device.businessName || '—'} />
          <InfoRow label="Contact Name" value={device.userName || '—'} />
          <InfoRow label="Phone Number" value={device.phoneNumber || '—'} />
          <InfoRow label="Activation Date" value={formatDate(device.activationDate)} />
          <InfoRow label="Installation Date" value={formatDate(device.installationDate)} />
          <InfoRow label="First Seen" value={formatDateTime(device.firstSeen)} />
        </InfoSection>

        <InfoSection title="Connectivity">
          <InfoRow label="Status" value={device.isOnline ? 'Online' : 'Offline'} />
          <InfoRow label="Last Seen" value={`${timeAgo(device.lastSeen)} (${formatDateTime(device.lastSeen)})`} />
          <InfoRow label="Last Sync" value={formatDateTime(device.lastSync)} />
          <InfoRow label="FCM Registration" value={device.hasFcmToken ? 'Registered' : 'Not Registered'} />
          <InfoRow label="Lock State" value={device.adminLocked ? '🔒 Locked (Admin)' : 'Unlocked'} />
        </InfoSection>

        <InfoSection title="Recent Commands & Lock History">
          {commandsLoading ? (
            <div className="text-xs text-slate-500 py-2">Loading command history...</div>
          ) : deviceCommands.length === 0 ? (
            <div className="text-xs text-slate-500 py-2">No commands issued to this device yet.</div>
          ) : (
            <div className="space-y-2.5 max-h-52 overflow-y-auto pr-1">
              {deviceCommands.map((cmd) => (
                <div key={cmd.commandId} className="flex items-center justify-between border-b border-slate-800 pb-2 last:border-0">
                  <div className="flex items-center gap-2">
                    <span className={cn(
                      'px-2 py-0.5 rounded font-mono font-bold text-[10px]',
                      cmd.command === 'LOCK' ? 'bg-red-500/20 text-red-400 border border-red-500/30' :
                      cmd.command === 'UNLOCK' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-300'
                    )}>
                      {cmd.command}
                    </span>
                    <span className="text-[11px] text-slate-400 truncate max-w-[120px]">{cmd.adminEmail || 'Admin'}</span>
                  </div>
                  <div className="flex items-center gap-2 text-right">
                    <span className={cn(
                      'font-semibold text-xs',
                      cmd.status === 'SUCCESS' ? 'text-emerald-400' :
                      cmd.status === 'FAILED' ? 'text-red-400' :
                      cmd.status === 'SENT' ? 'text-amber-400' : 'text-slate-500'
                    )}>
                      {cmd.status}
                    </span>
                    <span className="text-[10px] text-slate-500">{timeAgo(toEpochMs(cmd.processedAt || cmd.sentAt || cmd.createdAt))}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </InfoSection>
      </div>

      {/* Confirm dialog */}
      <ConfirmDialog
        isOpen={confirmOpen}
        onClose={() => { setConfirmOpen(false); resetState(); }}
        onConfirm={handleConfirm}
        title={pendingCommand === 'LOCK' ? 'Lock Device' : 'Unlock Device'}
        description={
          pendingCommand === 'LOCK'
            ? 'This will remotely lock the device and disable the AutoResponder. The device will remain locked until explicitly unlocked.'
            : 'This will remotely unlock the device and re-enable the AutoResponder.'
        }
        confirmLabel={pendingCommand === 'LOCK' ? 'Lock Device' : 'Unlock Device'}
        confirmVariant={pendingCommand === 'LOCK' ? 'danger' : 'default'}
        details={[
          { label: 'Device ID', value: device.deviceId },
          { label: 'Customer', value: device.businessName || device.userName || '—' },
          { label: 'Current Status', value: device.computedStatus },
          { label: 'Device Online', value: device.isOnline ? 'Yes' : 'No (command will queue)' },
          { label: 'FCM', value: device.hasFcmToken ? 'Registered' : 'Not registered' },
        ]}
        loading={commandInFlight}
      />
    </div>
  );
}

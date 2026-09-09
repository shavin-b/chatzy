// Activity page — command log + audit log

import { useState, useEffect, useMemo } from 'react';
import { Activity } from 'lucide-react';
import { subscribeToCommands } from '../services/commands';
import { subscribeToLogs } from '../services/logs';
import { StatusBadge } from '../components/StatusBadge';
import { formatDateTime } from '../lib/dateUtils';
import type { DeviceCommand, LogEntry } from '../types';
import { Link } from 'react-router-dom';
import { cn } from '../lib/cn';

type Tab = 'commands' | 'audit';

export function ActivityPage() {
  const [tab, setTab] = useState<Tab>('commands');
  const [commands, setCommands] = useState<DeviceCommand[]>([]);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [cmdLoading, setCmdLoading] = useState(true);
  const [logLoading, setLogLoading] = useState(true);
  const [cmdError, setCmdError] = useState<string | null>(null);
  const [logError, setLogError] = useState<string | null>(null);

  // Filters
  const [filterDevice, setFilterDevice] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterCommand, setFilterCommand] = useState('');

  useEffect(() => {
    const unsub = subscribeToCommands(
      (data) => { setCommands(data); setCmdLoading(false); },
      (_err) => { setCmdError('Unable to load command log.'); setCmdLoading(false); }
    );
    return unsub;
  }, []);

  useEffect(() => {
    const unsub = subscribeToLogs(
      (data) => { setLogs(data); setLogLoading(false); },
      (_err) => { setLogError('Unable to load audit log.'); setLogLoading(false); }
    );
    return unsub;
  }, []);

  const filteredCommands = useMemo(() => {
    return commands.filter((c) => {
      if (filterDevice && !c.deviceId.toLowerCase().includes(filterDevice.toLowerCase())) return false;
      if (filterStatus && c.status !== filterStatus) return false;
      if (filterCommand && c.command !== filterCommand) return false;
      return true;
    });
  }, [commands, filterDevice, filterStatus, filterCommand]);

  return (
    <div className="p-6 space-y-5">
      <div>
        <h1 className="text-xl font-bold text-white">Activity</h1>
        <p className="text-sm text-slate-500">Command log and audit trail</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 border-b border-slate-800">
        {(['commands', 'audit'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              'px-4 py-2.5 text-sm font-medium border-b-2 transition -mb-px capitalize',
              tab === t
                ? 'border-teal-400 text-teal-400'
                : 'border-transparent text-slate-500 hover:text-slate-300'
            )}
          >
            {t === 'commands' ? 'FCM Commands' : 'Audit Log'}
          </button>
        ))}
      </div>

      {/* Commands tab */}
      {tab === 'commands' && (
        <div className="space-y-4">
          {/* Filters */}
          <div className="flex flex-wrap gap-3">
            <input
              type="text"
              placeholder="Filter by device ID…"
              value={filterDevice}
              onChange={(e) => setFilterDevice(e.target.value)}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-300 placeholder-slate-600 outline-none focus:border-teal-500/50 w-48"
            />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-300 outline-none focus:border-teal-500/50"
            >
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="SENT">Sent</option>
              <option value="SUCCESS">Success</option>
              <option value="FAILED">Failed</option>
            </select>
            <select
              value={filterCommand}
              onChange={(e) => setFilterCommand(e.target.value)}
              className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-300 outline-none focus:border-teal-500/50"
            >
              <option value="">All Commands</option>
              <option value="LOCK">LOCK</option>
              <option value="UNLOCK">UNLOCK</option>
              <option value="SYNC">SYNC</option>
            </select>
          </div>

          {/* Table */}
          <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden">
            <div className="hidden grid-cols-[1fr_1.5fr_1fr_1fr_1.5fr_1.5fr] gap-4 border-b border-slate-800 px-5 py-3 text-xs font-medium uppercase tracking-wider text-slate-500 lg:grid">
              <span>Time</span>
              <span>Admin</span>
              <span>Device</span>
              <span>Command</span>
              <span>Status</span>
              <span>Error</span>
            </div>

            {cmdLoading ? (
              <div className="p-4 space-y-2">
                {Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-10 skeleton rounded-xl" />)}
              </div>
            ) : cmdError ? (
              <div className="p-4 text-sm text-red-300">{cmdError}</div>
            ) : filteredCommands.length === 0 ? (
              <div className="flex flex-col items-center gap-3 py-12 text-center">
                <Activity className="h-10 w-10 text-slate-700" />
                <p className="text-sm text-slate-500">No commands recorded yet.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-800">
                {filteredCommands.map((cmd) => (
                  <div
                    key={cmd.commandId}
                    className="grid grid-cols-1 gap-2 px-5 py-3 lg:grid-cols-[1fr_1.5fr_1fr_1fr_1.5fr_1.5fr] lg:items-center lg:gap-4"
                  >
                    <span className="text-xs text-slate-500 whitespace-nowrap">
                      {formatDateTime(cmd.createdAt as number)}
                    </span>
                    <span className="text-xs text-slate-400 truncate">{cmd.adminEmail ?? '—'}</span>
                    <Link
                      to={`/devices/${encodeURIComponent(cmd.deviceId)}`}
                      className="font-mono text-xs text-teal-400 hover:underline truncate"
                    >
                      {cmd.deviceId}
                    </Link>
                    <span className={cn(
                      'font-mono text-xs font-semibold',
                      cmd.command === 'LOCK' ? 'text-red-400' : 'text-emerald-400'
                    )}>
                      {cmd.command}
                    </span>
                    <StatusBadge status={cmd.status} />
                    <span className="text-xs text-slate-500 truncate">{cmd.errorMessage ?? '—'}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Audit log tab */}
      {tab === 'audit' && (
        <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden">
          <div className="hidden grid-cols-[1.5fr_1fr_1.5fr_2fr_1fr] gap-4 border-b border-slate-800 px-5 py-3 text-xs font-medium uppercase tracking-wider text-slate-500 lg:grid">
            <span>Time</span>
            <span>Category</span>
            <span>Title</span>
            <span>Description</span>
            <span>Performed By</span>
          </div>

          {logLoading ? (
            <div className="p-4 space-y-2">
              {Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-10 skeleton rounded-xl" />)}
            </div>
          ) : logError ? (
            <div className="p-4 text-sm text-red-300">{logError}</div>
          ) : logs.length === 0 ? (
            <div className="flex flex-col items-center gap-3 py-12 text-center">
              <Activity className="h-10 w-10 text-slate-700" />
              <p className="text-sm text-slate-500">No audit entries yet.</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-800">
              {logs.map((log) => (
                <div key={log.logId} className="grid grid-cols-1 gap-2 px-5 py-3 lg:grid-cols-[1.5fr_1fr_1.5fr_2fr_1fr] lg:items-center lg:gap-4">
                  <span className="text-xs text-slate-500">{formatDateTime(log.timestamp)}</span>
                  <span className="rounded-lg bg-slate-800 px-2 py-0.5 text-xs text-slate-400 w-fit">{log.category}</span>
                  <span className="text-sm font-medium text-slate-200 truncate">{log.title}</span>
                  <span className="text-xs text-slate-500 truncate">{log.description}</span>
                  <span className="text-xs text-slate-400 truncate">{log.performedBy}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

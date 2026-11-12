// Devices page — searchable, filterable device list

import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  Search,
  Smartphone,
  ChevronUp,
  ChevronDown,
  XCircle,
  Lock,
} from 'lucide-react';
import { useDevices } from '../hooks/useDevices';
import { StatusBadge, OnlineBadge } from '../components/StatusBadge';
import { timeAgo, formatDate } from '../lib/dateUtils';
import { cn } from '../lib/cn';
import type { NormalizedDevice } from '../types';

type SortKey = 'lastSeen' | 'subscriptionEnd' | 'deviceId' | 'businessName';
type SortDir = 'asc' | 'desc';
type StatusFilter = 'all' | 'online' | 'offline' | 'active' | 'expiring' | 'expired' | 'suspended';

const ITEMS_PER_PAGE = 20;

export function DevicesPage() {
  const { devices, loading, error } = useDevices();

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [sortKey, setSortKey] = useState<SortKey>('lastSeen');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [page, setPage] = useState(1);

  function toggleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('desc');
    }
    setPage(1);
  }

  const filtered = useMemo(() => {
    let list = [...devices];

    // Search
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (d) =>
          d.deviceId.toLowerCase().includes(q) ||
          d.businessName.toLowerCase().includes(q) ||
          d.userName.toLowerCase().includes(q) ||
          d.phoneModel.toLowerCase().includes(q) ||
          d.phoneNumber.toLowerCase().includes(q)
      );
    }

    // Status filter
    switch (statusFilter) {
      case 'online':   list = list.filter((d) => d.isOnline); break;
      case 'offline':  list = list.filter((d) => !d.isOnline); break;
      case 'active':   list = list.filter((d) => d.computedStatus === 'Active'); break;
      case 'expiring': list = list.filter((d) => d.computedStatus === 'Expiring Soon'); break;
      case 'expired':  list = list.filter((d) => d.computedStatus === 'Expired'); break;
      case 'suspended': list = list.filter((d) => d.computedStatus === 'Suspended'); break;
    }

    // Sort
    list.sort((a, b) => {
      let valA: number | string = 0;
      let valB: number | string = 0;
      switch (sortKey) {
        case 'lastSeen': valA = a.lastSeen ?? 0; valB = b.lastSeen ?? 0; break;
        case 'subscriptionEnd': valA = a.subscriptionEnd ?? 0; valB = b.subscriptionEnd ?? 0; break;
        case 'deviceId': valA = a.deviceId; valB = b.deviceId; break;
        case 'businessName': valA = a.businessName; valB = b.businessName; break;
      }
      if (typeof valA === 'string') {
        return sortDir === 'asc'
          ? valA.localeCompare(valB as string)
          : (valB as string).localeCompare(valA);
      }
      return sortDir === 'asc' ? (valA as number) - (valB as number) : (valB as number) - (valA as number);
    });

    return list;
  }, [devices, search, statusFilter, sortKey, sortDir]);

  const totalPages = Math.ceil(filtered.length / ITEMS_PER_PAGE);
  const paginated = filtered.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE);

  function SortButton({ colKey, label }: { colKey: SortKey; label: string }) {
    const active = sortKey === colKey;
    return (
      <button
        onClick={() => toggleSort(colKey)}
        className={cn(
          'flex items-center gap-1 text-xs font-medium uppercase tracking-wider transition',
          active ? 'text-teal-400' : 'text-slate-500 hover:text-slate-300'
        )}
      >
        {label}
        <span className="flex flex-col">
          <ChevronUp className={cn('h-2.5 w-2.5', active && sortDir === 'asc' ? 'text-teal-400' : 'text-slate-700')} />
          <ChevronDown className={cn('h-2.5 w-2.5', active && sortDir === 'desc' ? 'text-teal-400' : 'text-slate-700')} />
        </span>
      </button>
    );
  }

  const filterButtons: { key: StatusFilter; label: string }[] = [
    { key: 'all', label: 'All' },
    { key: 'online', label: '🟢 Online' },
    { key: 'offline', label: '⚫ Offline' },
    { key: 'active', label: 'Active' },
    { key: 'expiring', label: 'Expiring' },
    { key: 'expired', label: 'Expired' },
    { key: 'suspended', label: 'Suspended' },
  ];

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">Devices</h1>
          <p className="text-sm text-slate-500">
            {loading ? 'Loading…' : error ? 'Device count unavailable' : `${devices.length} device${devices.length !== 1 ? 's' : ''} registered`}
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        {/* Search */}
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            id="device-search"
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by device ID, business name, model, phone…"
            className="w-full rounded-xl border border-slate-700 bg-slate-900 py-2.5 pl-9 pr-4 text-sm text-slate-200 placeholder-slate-600 outline-none transition focus:border-teal-500/50 focus:ring-1 focus:ring-teal-500/20"
          />
          {search && (
            <button
              onClick={() => { setSearch(''); setPage(1); }}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
            >
              <XCircle className="h-4 w-4" />
            </button>
          )}
        </div>

        {/* Status filters */}
        <div className="flex flex-wrap gap-2">
          {filterButtons.map(({ key, label }) => (
            <button
              key={key}
              onClick={() => { setStatusFilter(key); setPage(1); }}
              className={cn(
                'rounded-xl px-3 py-1.5 text-xs font-medium transition',
                statusFilter === key
                  ? 'bg-teal-500/20 text-teal-400 ring-1 ring-teal-500/30'
                  : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200'
              )}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900 overflow-hidden">
        {/* Error */}
        {error && (
          <div className="flex items-center gap-3 border-b border-slate-800 p-4 text-sm text-red-300">
            <XCircle className="h-4 w-4 flex-shrink-0 text-red-400" />
            {error}
          </div>
        )}

        {/* Table header */}
        <div className="hidden grid-cols-[minmax(0,1.5fr)_minmax(0,1.5fr)_1fr_1fr_auto_auto_auto_auto] gap-4 border-b border-slate-800 px-5 py-3 lg:grid">
          <SortButton colKey="deviceId" label="Device ID" />
          <SortButton colKey="businessName" label="Customer" />
          <span className="text-xs font-medium uppercase tracking-wider text-slate-500">Model</span>
          <span className="text-xs font-medium uppercase tracking-wider text-slate-500">Version</span>
          <span className="text-xs font-medium uppercase tracking-wider text-slate-500">Status</span>
          <span className="text-xs font-medium uppercase tracking-wider text-slate-500">Online</span>
          <SortButton colKey="subscriptionEnd" label="Subscription" />
          <SortButton colKey="lastSeen" label="Last Seen" />
        </div>

        {/* Skeleton */}
        {loading ? (
          <div className="p-4 space-y-2">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="h-12 skeleton rounded-xl" />
            ))}
          </div>
        ) : error ? null : paginated.length === 0 ? (
          <div className="flex flex-col items-center gap-3 py-16 text-center">
            <Smartphone className="h-12 w-12 text-slate-800" />
            {search || statusFilter !== 'all' ? (
              <>
                <p className="text-sm font-medium text-slate-500">No devices match your filters.</p>
                <button
                  onClick={() => { setSearch(''); setStatusFilter('all'); }}
                  className="text-xs text-teal-400 hover:text-teal-300 transition"
                >
                  Clear filters
                </button>
              </>
            ) : (
              <>
                <p className="text-sm font-medium text-slate-500">No devices registered yet.</p>
                <p className="text-xs text-slate-600">Devices appear automatically when the Android client registers.</p>
              </>
            )}
          </div>
        ) : (
          <div className="divide-y divide-slate-800">
            {paginated.map((device) => (
              <DeviceRow key={device.deviceId} device={device} />
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-800 px-5 py-3">
            <p className="text-xs text-slate-500">
              Showing {(page - 1) * ITEMS_PER_PAGE + 1}–{Math.min(page * ITEMS_PER_PAGE, filtered.length)} of {filtered.length}
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(page - 1)}
                disabled={page === 1}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1 text-xs text-slate-400 transition hover:bg-slate-700 disabled:opacity-40"
              >
                Previous
              </button>
              <button
                onClick={() => setPage(page + 1)}
                disabled={page === totalPages}
                className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1 text-xs text-slate-400 transition hover:bg-slate-700 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function DeviceRow({ device }: { device: NormalizedDevice }) {
  return (
    <Link
      to={`/devices/${encodeURIComponent(device.deviceId)}`}
      className="grid grid-cols-1 gap-2 px-5 py-4 transition hover:bg-slate-800/50 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1.5fr)_1fr_1fr_auto_auto_auto_auto] lg:items-center lg:gap-4"
    >
      {/* Device ID */}
      <div className="font-mono text-xs font-semibold text-teal-400 truncate">{device.deviceId}</div>

      {/* Customer */}
      <div className="min-w-0">
        <p className="text-sm font-medium text-slate-200 truncate">
          {device.businessName || device.userName || '—'}
        </p>
        {device.businessName && device.userName && (
          <p className="text-xs text-slate-500 truncate">{device.userName}</p>
        )}
      </div>

      {/* Model */}
      <div className="text-xs text-slate-400 truncate">
        {device.manufacturer} {device.phoneModel}
      </div>

      {/* App Version */}
      <div className="text-xs text-slate-500">
        <span className="font-mono">{device.appVersion || '—'}</span>
        <span className="ml-1 text-slate-600">Android {device.androidVersion}</span>
      </div>

      {/* Subscription status & lock indicator */}
      <div className="flex items-center gap-1.5 flex-wrap">
        <StatusBadge status={device.computedStatus} />
        {device.adminLocked && (
          <span className="inline-flex items-center gap-1 rounded-full bg-red-500/15 border border-red-500/30 px-2 py-0.5 text-[10px] font-semibold text-red-400">
            <Lock className="h-2.5 w-2.5" />
            Locked
          </span>
        )}
      </div>

      {/* Online badge */}
      <OnlineBadge isOnline={device.isOnline} />

      {/* Subscription expiry */}
      <div className="text-xs text-slate-500">
        {device.subscriptionEnd ? formatDate(device.subscriptionEnd) : 'N/A'}
      </div>

      {/* Last seen */}
      <div className="text-xs text-slate-500 whitespace-nowrap">{timeAgo(device.lastSeen)}</div>
    </Link>
  );
}

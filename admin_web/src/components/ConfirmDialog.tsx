// ConfirmDialog — modal for confirming destructive actions (LOCK/UNLOCK)

import { AlertTriangle, X } from 'lucide-react';
import { cn } from '../lib/cn';

interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  confirmLabel?: string;
  confirmVariant?: 'danger' | 'warning' | 'default';
  details?: Array<{ label: string; value: string }>;
  loading?: boolean;
}

export function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = 'Confirm',
  confirmVariant = 'danger',
  details,
  loading = false,
}: ConfirmDialogProps) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/70 backdrop-blur-sm"
        onClick={onClose}
      />
      {/* Dialog */}
      <div className="relative z-10 w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl">
        {/* Header */}
        <div className="flex items-start gap-4">
          <div
            className={cn(
              'flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full',
              confirmVariant === 'danger'
                ? 'bg-red-500/15'
                : confirmVariant === 'warning'
                ? 'bg-amber-500/15'
                : 'bg-teal-500/15'
            )}
          >
            <AlertTriangle
              className={cn(
                'h-5 w-5',
                confirmVariant === 'danger'
                  ? 'text-red-400'
                  : confirmVariant === 'warning'
                  ? 'text-amber-400'
                  : 'text-teal-400'
              )}
            />
          </div>
          <div className="flex-1">
            <h3 className="text-base font-semibold text-white">{title}</h3>
            <p className="mt-1 text-sm text-slate-400">{description}</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-lg p-1 text-slate-500 transition hover:bg-slate-800 hover:text-slate-300"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Details */}
        {details && details.length > 0 && (
          <div className="mt-4 rounded-xl border border-slate-700 bg-slate-800/50 p-4">
            <dl className="space-y-2">
              {details.map(({ label, value }) => (
                <div key={label} className="flex justify-between gap-4">
                  <dt className="text-xs font-medium text-slate-500">{label}</dt>
                  <dd className="text-xs font-medium text-slate-300 text-right">{value}</dd>
                </div>
              ))}
            </dl>
          </div>
        )}

        {/* Actions */}
        <div className="mt-6 flex gap-3">
          <button
            onClick={onClose}
            disabled={loading}
            className="flex-1 rounded-xl border border-slate-700 bg-slate-800 px-4 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-slate-700 hover:text-white disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className={cn(
              'flex-1 rounded-xl px-4 py-2.5 text-sm font-semibold text-white transition disabled:opacity-50',
              confirmVariant === 'danger'
                ? 'bg-red-600 hover:bg-red-500'
                : confirmVariant === 'warning'
                ? 'bg-amber-600 hover:bg-amber-500'
                : 'bg-teal-600 hover:bg-teal-500'
            )}
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                Processing...
              </span>
            ) : (
              confirmLabel
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

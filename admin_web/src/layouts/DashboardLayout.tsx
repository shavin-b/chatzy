import { brand } from '../config/brand';
// DashboardLayout — sidebar + top header shell

import { useState, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Smartphone,
  Users,
  CreditCard,
  Activity,
  Settings,
  LogOut,
  Menu,
  Bell,
  ChevronDown,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { cn } from '../lib/cn';

const navItems = [
  { label: 'Dashboard', href: '/', icon: LayoutDashboard },
  { label: 'Devices', href: '/devices', icon: Smartphone },
  { label: 'Users', href: '/users', icon: Users },
  { label: 'Subscriptions', href: '/subscriptions', icon: CreditCard },
  { label: 'Activity', href: '/activity', icon: Activity },
  { label: 'Settings', href: '/settings', icon: Settings },
];

function NavItem({
  item,
  collapsed,
  onClick,
}: {
  item: (typeof navItems)[number];
  collapsed: boolean;
  onClick?: () => void;
}) {
  const location = useLocation();
  const isActive =
    item.href === '/'
      ? location.pathname === '/'
      : location.pathname.startsWith(item.href);

  return (
    <Link
      to={item.href}
      onClick={onClick}
      title={collapsed ? item.label : undefined}
      className={cn(
        'group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-150',
        isActive
          ? 'bg-teal-500/15 text-teal-400'
          : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
      )}
    >
      <item.icon
        className={cn(
          'h-5 w-5 flex-shrink-0 transition-colors',
          isActive ? 'text-teal-400' : 'text-slate-500 group-hover:text-slate-300'
        )}
      />
      {!collapsed && <span>{item.label}</span>}
      {!collapsed && isActive && (
        <span className="ml-auto h-1.5 w-1.5 rounded-full bg-teal-400" />
      )}
    </Link>
  );
}

interface DashboardLayoutProps {
  children: ReactNode;
}

export function DashboardLayout({ children }: DashboardLayoutProps) {
  const { admin, signOut } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  async function handleSignOut() {
    await signOut();
    navigate('/login', { replace: true });
  }

  const roleColor =
    admin?.role === 'Super Admin'
      ? 'text-amber-400'
      : admin?.role === 'Admin'
      ? 'text-teal-400'
      : 'text-slate-400';

  return (
    <div className="flex h-screen overflow-hidden bg-slate-950 text-slate-100">
      {/* ── Mobile Sidebar Overlay ── */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* ── Sidebar ── */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex flex-col border-r border-slate-800 bg-slate-900 transition-all duration-300 lg:relative lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
          sidebarCollapsed ? 'w-16' : 'w-64'
        )}
      >
        {/* Logo */}
        <div
          className={cn(
            'flex h-16 items-center border-b border-slate-800 px-4',
            sidebarCollapsed ? 'justify-center' : 'gap-3'
          )}
        >
          <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-teal-500/20">
            <img src="/favicon.svg" alt="Chatzy" className="h-8 w-8" />
          </div>
          {!sidebarCollapsed && (
            <div className="flex-1 min-w-0">
              <p className="text-sm font-bold text-white truncate">{brand.product}</p>
              <p className="text-xs text-slate-500 truncate">Admin Console</p>
            </div>
          )}
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto p-3 space-y-1">
          {navItems.map((item) => (
            <NavItem
              key={item.href}
              item={item}
              collapsed={sidebarCollapsed}
              onClick={() => setSidebarOpen(false)}
            />
          ))}
        </nav>

        {/* User profile */}
        <div className="border-t border-slate-800 p-3">
          {!sidebarCollapsed && (
            <div className="mb-2 rounded-xl bg-slate-800/50 px-3 py-2">
              <p className="truncate text-xs font-semibold text-slate-200">
                {admin?.name ?? '—'}
              </p>
              <p className={cn('text-xs font-medium', roleColor)}>
                {admin?.role ?? '—'}
              </p>
            </div>
          )}
          <button
            onClick={handleSignOut}
            title={sidebarCollapsed ? 'Sign Out' : undefined}
            className="flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium text-slate-400 transition hover:bg-slate-800 hover:text-red-400"
          >
            <LogOut className="h-4 w-4 flex-shrink-0" />
            {!sidebarCollapsed && 'Sign Out'}
          </button>
        </div>

        {/* Collapse toggle (desktop) */}
        <button
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          className="hidden lg:flex absolute -right-3 top-20 h-6 w-6 items-center justify-center rounded-full border border-slate-700 bg-slate-900 text-slate-500 hover:text-teal-400 transition"
        >
          <ChevronDown
            className={cn(
              'h-3 w-3 transition-transform',
              sidebarCollapsed ? 'rotate-90' : '-rotate-90'
            )}
          />
        </button>
      </aside>

      {/* ── Main content ── */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top header */}
        <header className="flex h-16 flex-shrink-0 items-center gap-4 border-b border-slate-800 bg-slate-900/50 px-4 backdrop-blur-sm">
          {/* Mobile menu toggle */}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="lg:hidden rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-slate-200"
          >
            <Menu className="h-5 w-5" />
          </button>

          {/* Breadcrumb / page title placeholder */}
          <div className="flex-1" />

          {/* Actions */}
          <div className="flex items-center gap-2">
            <button className="relative rounded-lg p-2 text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition">
              <Bell className="h-4 w-4" />
            </button>

            {/* Profile menu */}
            <div className="relative">
              <button
                onClick={() => setProfileMenuOpen(!profileMenuOpen)}
                className="flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-3 py-1.5 text-sm transition hover:border-slate-600"
              >
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-teal-500/20 text-xs font-bold text-teal-400">
                  {admin?.name?.charAt(0)?.toUpperCase() ?? 'A'}
                </div>
                <span className="hidden text-slate-300 sm:block">{admin?.name ?? 'Admin'}</span>
                <ChevronDown className="h-3 w-3 text-slate-500" />
              </button>

              {profileMenuOpen && (
                <>
                  <div className="fixed inset-0 z-10" onClick={() => setProfileMenuOpen(false)} />
                  <div className="absolute right-0 top-full z-20 mt-2 w-48 rounded-xl border border-slate-700 bg-slate-900 p-1 shadow-2xl">
                    <div className="px-3 py-2 border-b border-slate-800 mb-1">
                      <p className="text-xs font-semibold text-white truncate">{admin?.email ?? '—'}</p>
                      <p className={cn('text-xs', roleColor)}>{admin?.role ?? '—'}</p>
                    </div>
                    <Link
                      to="/settings"
                      onClick={() => setProfileMenuOpen(false)}
                      className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition"
                    >
                      <Settings className="h-4 w-4" />
                      Settings
                    </Link>
                    <button
                      onClick={() => { setProfileMenuOpen(false); handleSignOut(); }}
                      className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-red-400 hover:bg-red-500/10 transition"
                    >
                      <LogOut className="h-4 w-4" />
                      Sign Out
                    </button>
                  </div>
                </>
              )}
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
}

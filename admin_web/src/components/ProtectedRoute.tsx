// ProtectedRoute — redirects unauthenticated and unauthorized users

import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: 'Super Admin' | 'Admin' | 'Support';
}

export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { user, admin, loading, signOut } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-950">
        <div className="flex flex-col items-center gap-4">
          <div className="h-10 w-10 animate-spin rounded-full border-2 border-teal-500/30 border-t-teal-500" />
          <p className="text-sm text-slate-500">Verifying session…</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!admin) {
    // Authenticated but NOT in admins collection
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-slate-950 p-8 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-500/10">
          <svg className="h-8 w-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
          </svg>
        </div>
        <h2 className="text-xl font-bold text-white">Access Denied</h2>
        <p className="max-w-sm text-sm text-slate-400">
          Your account is not authorized to access the Chatzy Admin Console. Contact a Super Admin to grant you access.
        </p>
        <button
          onClick={() => signOut()}
          className="rounded-xl bg-slate-800 px-6 py-2 text-sm font-medium text-slate-300 hover:bg-slate-700"
        >
          Sign Out
        </button>
      </div>
    );
  }

  // Role-based check
  if (requiredRole) {
    const roleOrder = { 'Super Admin': 3, 'Admin': 2, 'Support': 1 };
    const userLevel = roleOrder[admin.role] ?? 0;
    const requiredLevel = roleOrder[requiredRole] ?? 0;
    if (userLevel < requiredLevel) {
      return (
        <div className="flex h-full flex-col items-center justify-center gap-3 p-8 text-center">
          <p className="text-sm text-slate-400">
            You need <strong className="text-white">{requiredRole}</strong> access to view this page.
          </p>
        </div>
      );
    }
  }

  return <>{children}</>;
}

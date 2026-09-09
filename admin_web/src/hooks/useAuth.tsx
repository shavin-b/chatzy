// useAuth hook — provides auth state and admin profile throughout the app

import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { User } from 'firebase/auth';
import type { AdminUser } from '../types';
import {
  onAuthChanged,
  getAdminProfile,
  updateLastLogin,
  signIn as authSignIn,
  signOut as authSignOut,
} from '../services/auth';

interface AuthContextValue {
  user: User | null;
  admin: AdminUser | null;
  loading: boolean;
  authError: string | null;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [admin, setAdmin] = useState<AdminUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthChanged(async (firebaseUser) => {
      setUser(firebaseUser);
      if (firebaseUser) {
        const profile = await getAdminProfile(firebaseUser.uid);
        setAdmin(profile);
        if (profile) {
          await updateLastLogin(firebaseUser.uid);
        }
      } else {
        setAdmin(null);
      }
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  async function signIn(email: string, password: string): Promise<void> {
    setAuthError(null);
    try {
      await authSignIn(email, password);
    } catch (err: unknown) {
      const msg = (err as { message?: string })?.message ?? 'Login failed';
      // Map Firebase error codes to user-friendly messages
      if (msg.includes('invalid-credential') || msg.includes('wrong-password') || msg.includes('user-not-found')) {
        setAuthError('Invalid email or password.');
      } else if (msg.includes('too-many-requests')) {
        setAuthError('Too many login attempts. Please try again later.');
      } else {
        setAuthError('Login failed. Please try again.');
      }
      throw err;
    }
  }

  async function signOut(): Promise<void> {
    await authSignOut();
    setUser(null);
    setAdmin(null);
  }

  return (
    <AuthContext.Provider value={{ user, admin, loading, authError, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

export { AuthContext };

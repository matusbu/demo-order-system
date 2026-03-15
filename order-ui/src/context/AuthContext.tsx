import { createContext, useContext, useState, type ReactNode } from 'react';

interface AuthState {
  customerName: string | null;
  login: (customerName: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [customerName, setCustomerName] = useState<string | null>(null);

  const login = (name: string) => setCustomerName(name);
  const logout = () => setCustomerName(null);

  return (
    <AuthContext.Provider value={{ customerName, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

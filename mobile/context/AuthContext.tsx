import React, { createContext, useContext, useState, useEffect } from 'react';
import { storage } from '../utils/storage';

interface AuthContextType {
  anonymousId: string | null;
  token: string | null;
  isLoading: boolean;
  enroll: (id: string, token: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [anonymousId, setAnonymousId] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadStorage();
  }, []);

  const loadStorage = async () => {
    try {
      const id = await storage.getItem('circleguard_anon_id');
      const savedToken = await storage.getItem('circleguard_token');
      console.log('AuthContext: Loaded from storage', { id, savedToken });
      setAnonymousId(id);
      setToken(savedToken);
    } catch (e) {
      console.error('AuthContext: Failed to load storage', e);
    } finally {
      setIsLoading(false);
    }
  };

  const enroll = async (id: string, newToken: string) => {
    console.log('AuthContext: Enrolling', { id });
    await storage.setItem('circleguard_anon_id', id);
    await storage.setItem('circleguard_token', newToken);
    setAnonymousId(id);
    setToken(newToken);
  };

  const logout = async () => {
    console.log('AuthContext: Logging out and clearing storage');
    await storage.deleteItem('circleguard_anon_id');
    await storage.deleteItem('circleguard_token');
    setAnonymousId(null);
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ anonymousId, token, isLoading, enroll, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

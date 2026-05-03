import { useState, useEffect } from 'react';
import { storage } from '@/utils/storage';

/**
 * Hook to manage CircleGuard Identity Handshake.
 * Implements Story 1.1 & 2.1: Anonymous ID persistence.
 */
export const useAuth = () => {
  const [anonymousId, setAnonymousId] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadIdentity();
  }, []);

  const loadIdentity = async () => {
    try {
      const id = await storage.getItem('circleguard_anon_id');
      const storedToken = await storage.getItem('circleguard_token');
      console.log('useAuth: Loaded from storage', { id, storedToken });
      setAnonymousId(id);
      setToken(storedToken);
    } catch (e) {
      console.error('Failed to load identity', e);
    } finally {
      setIsLoading(false);
    }
  };

  const enroll = async (id: string, newToken: string) => {
    console.log('useAuth: Enrolling', { id });
    await storage.setItem('circleguard_anon_id', id);
    await storage.setItem('circleguard_token', newToken);
    setAnonymousId(id);
    setToken(newToken);
  };

  const logout = async () => {
    console.log('useAuth: Logging out and clearing storage');
    await storage.deleteItem('circleguard_anon_id');
    await storage.deleteItem('circleguard_token');
    setAnonymousId(null);
    setToken(null);
  };

  return { anonymousId, token, isLoading, enroll, logout };
};

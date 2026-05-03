import { useState, useEffect } from 'react';
import axios from 'axios';
import { AUTH_BASE_URL } from '@/constants/Config';

/**
 * Hook to fetch and rotate short-lived Campus Entry QR tokens.
 * Implements Story 2.2: Rotating Token logic.
 */
export const useQrToken = (anonymousId: string | null, authToken: string | null) => {
  const [token, setToken] = useState<string | null>(null);
  const [timeLeft, setTimeLeft] = useState(60);

  useEffect(() => {
    if (!anonymousId || !authToken) return;

    fetchToken();
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          fetchToken();
          return 60;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [anonymousId, authToken]);

  const fetchToken = async () => {
    try {
      const response = await axios.get(`${AUTH_BASE_URL}/api/v1/auth/qr/generate`, {
        headers: {
          Authorization: `Bearer ${authToken}`
        }
      });
      
      if (response.data && response.data.qrToken) {
        setToken(response.data.qrToken);
        const expires = parseInt(response.data.expiresIn || '60', 10);
        setTimeLeft(expires);
      }
    } catch (e) {
      console.error('QR Fetch Failed', e);
      // Fallback to a recognizable error token if needed, or keep last token
    }
  };

  return { token, timeLeft };
};

import { useState, useCallback } from 'react';
import axios from 'axios';
import { PROMOTION_BASE_URL } from '../constants/Config';

export interface Circle {
  id: string;
  name: string;
  inviteCode: string;
  locationId?: string;
  isActive: boolean;
}

export const useCircles = (anonymousId: string) => {
  const [circles, setCircles] = useState<Circle[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchCircles = useCallback(async () => {
    if (!anonymousId) return;
    setLoading(true);
    try {
      const response = await axios.get(`${PROMOTION_BASE_URL}/api/v1/circles/user/${anonymousId}`);
      setCircles(response.data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch circles');
    } finally {
      setLoading(false);
    }
  }, [anonymousId]);

  const createCircle = async (name: string, locationId: string = 'building-1') => {
    setLoading(true);
    try {
      const response = await axios.post(`${PROMOTION_BASE_URL}/api/v1/circles`, {
        name,
        locationId
      });
      await fetchCircles(); // Refresh list
      return response.data;
    } catch (err: any) {
      setError(err.message || 'Failed to create circle');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const joinCircle = async (inviteCode: string) => {
    setLoading(true);
    try {
      const response = await axios.post(`${PROMOTION_BASE_URL}/api/v1/circles/join/${inviteCode}/user/${anonymousId}`);
      await fetchCircles(); // Refresh list
      return response.data;
    } catch (err: any) {
      setError(err.message || 'Failed to join circle');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const addMember = async (circleId: string, memberAnonymousId: string) => {
    setLoading(true);
    try {
      const response = await axios.post(`${PROMOTION_BASE_URL}/api/v1/circles/${circleId}/members/${memberAnonymousId}`);
      await fetchCircles();
      return response.data;
    } catch (err: any) {
      setError(err.message || 'Failed to add member to circle');
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return {
    circles,
    loading,
    error,
    fetchCircles,
    createCircle,
    joinCircle,
    addMember
  };
};

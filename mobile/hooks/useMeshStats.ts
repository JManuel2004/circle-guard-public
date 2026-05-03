import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { PROMOTION_BASE_URL } from '../constants/Config';

export interface MeshStats {
  confirmedCount: number;
  unconfirmedCount: number;
}

/**
 * Hook to fetch and poll connection stats for "The Mesh" visualization.
 */
export const useMeshStats = (anonymousId: string | null) => {
  const [stats, setStats] = useState<MeshStats>({ confirmedCount: 0, unconfirmedCount: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = useCallback(async () => {
    if (!anonymousId) return;
    setLoading(true);
    try {
      const response = await axios.get(`${PROMOTION_BASE_URL}/api/v1/mesh/stats/${anonymousId}`);
      setStats(response.data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch mesh stats');
    } finally {
      setLoading(false);
    }
  }, [anonymousId]);

  useEffect(() => {
    fetchStats();
    // Poll every 30 seconds to keep visualization dynamic
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, [fetchStats]);

  return { stats, loading, error, refresh: fetchStats };
};

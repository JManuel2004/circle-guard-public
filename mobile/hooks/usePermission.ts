import { useMemo } from 'react';
import { useAuth } from './useAuth';

/**
 * Hook to handle permission-based UI gating.
 * Implements Story 1.6.
 */
export const usePermission = () => {
    const { token } = useAuth();

    const permissions = useMemo(() => {
        if (!token) return [];
        try {
            // Basic JWT decoding (payload is the second part)
            const parts = token.split('.');
            if (parts.length !== 3) {
                // Not a valid JWT (could be a handoff token or other non-auth string)
                return [];
            }
            const base64Url = parts[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            const payload = JSON.parse(jsonPayload);
            return payload.permissions || [];
        } catch (e) {
            console.warn('Malformed JWT token detected', e);
            return [];
        }
    }, [token]);

    const hasPermission = (permission: string): boolean => {
        return permissions.includes(permission);
    };

    return { hasPermission, permissions };
};

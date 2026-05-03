import { useState } from 'react';
import { API_BASE_URL } from '@/constants/Config';
import { useAuth } from './useAuth';

export type ValidationStatus = 'idle' | 'validating' | 'success' | 'error';

export interface ValidationResult {
    valid: boolean;
    status: 'GREEN' | 'RED';
    message: string;
}

/**
 * Hook to validate a scanned QR token against the Gateway Service.
 * Implements Story 2.6 logic.
 */
export const useGateValidation = () => {
    const { token: authToken } = useAuth();
    const [status, setStatus] = useState<ValidationStatus>('idle');
    const [result, setResult] = useState<ValidationResult | null>(null);

    const validateToken = async (qrToken: string) => {
        if (!authToken) {
            setStatus('error');
            setResult({ valid: false, status: 'RED', message: 'Staff authentication required' });
            return;
        }

        setStatus('validating');
        try {
            const response = await fetch(`${API_BASE_URL}/api/v1/gate/validate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authToken}`,
                },
                body: JSON.stringify({ token: qrToken }),
            });

            if (!response.ok) {
                throw new Error('Gate validation failed');
            }

            const data: ValidationResult = await response.json();
            setResult(data);
            setStatus('success');
        } catch (error) {
            console.error('Validation Error', error);
            setStatus('error');
            setResult({ valid: false, status: 'RED', message: 'Network or system error' });
        }
    };

    const reset = () => {
        setStatus('idle');
        setResult(null);
    };

    return { validateToken, status, result, reset };
};

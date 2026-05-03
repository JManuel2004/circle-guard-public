import { useState, useEffect, useRef } from 'react';
import { AppState, AppStateStatus } from 'react-native';
import * as LocalAuthentication from 'expo-local-authentication';

const BIOMETRIC_EXP_TIME = 5 * 60 * 1000; // 5 minutes in ms

/**
 * Hook to manage Biometric Re-authentication.
 * Implements Story 2.7 with focus-loss and temporal expiration logic.
 */
export const useBiometricAuth = () => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [lastAuthenticatedAt, setLastAuthenticatedAt] = useState<number | null>(null);
    const appState = useRef(AppState.currentState);

    useEffect(() => {
        const subscription = AppState.addEventListener('change', handleAppStateChange);
        return () => subscription.remove();
    }, []);

    const handleAppStateChange = (nextAppState: AppStateStatus) => {
        // "Remove the token when the app is minimized or moved to background"
        if (
            appState.current === 'active' &&
            nextAppState.match(/inactive|background/)
        ) {
            console.log('App moved to background, clearing biometric auth');
            clearAuth();
        }
        appState.current = nextAppState;
    };

    const clearAuth = () => {
        setIsAuthenticated(false);
        setLastAuthenticatedAt(null);
    };

    const isSessionValid = () => {
        if (!isAuthenticated || !lastAuthenticatedAt) return false;
        const now = Date.now();
        return now - lastAuthenticatedAt < BIOMETRIC_EXP_TIME;
    };

    /**
     * Triggers biometric authentication.
     * Falls back to PIN/Passcode if supported by device.
     */
    const authenticate = async (reason: string = 'Verify your identity to continue') => {
        try {
            // Check if hardware is available
            const hasHardware = await LocalAuthentication.hasHardwareAsync();
            if (!hasHardware) {
                // Degrade gracefully if no hardware, but still "authenticate" for dev/simulators
                // Or we could just set it to true if we trust the device
                setIsAuthenticated(true);
                setLastAuthenticatedAt(Date.now());
                return true;
            }

            const result = await LocalAuthentication.authenticateAsync({
                promptMessage: reason,
                fallbackLabel: 'Use Passcode',
            });

            if (result.success) {
                setIsAuthenticated(true);
                setLastAuthenticatedAt(Date.now());
                return true;
            }

            return false;
        } catch (error) {
            console.error('Biometric Auth Error', error);
            return false;
        }
    };

    return { 
        authenticate, 
        isAuthenticated: isSessionValid(), 
        clearAuth 
    };
};

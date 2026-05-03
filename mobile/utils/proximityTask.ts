import { Platform } from 'react-native';
import * as TaskManager from 'expo-task-manager';
import * as BackgroundFetch from 'expo-background-fetch';
import * as SecureStore from 'expo-secure-store';
import { ProximityScanner } from './proximityScanner';

export const PROXIMITY_SYNC_TASK = 'PROXIMITY_SYNC_TASK';
const PROMOTION_BASE_URL = 'http://localhost:8088'; // Promotion Service

/**
 * Registry for the periodic background encounter sync.
 * This runs even when the app is in the background or closed.
 */
TaskManager.defineTask(PROXIMITY_SYNC_TASK, async () => {
    try {
        console.log('[Task] PROXIMITY_SYNC_TASK started...');
        
        // 1. Get current user's anonymousId
        const myAnonymousId = await SecureStore.getItemAsync('user_anonymous_id');
        if (!myAnonymousId) {
            console.log('[Task] No anonymousId found. Skipping sync.');
            return BackgroundFetch.BackgroundFetchResult.NoData;
        }

        // 2. Scan for peers
        const peers = await ProximityScanner.scanForPeers(15000); // 15s scan
        if (peers.length === 0) {
            console.log('[Task] No peers detected.');
            return BackgroundFetch.BackgroundFetchResult.NoData;
        }

        // 3. Report encounters to backend
        for (const peer of peers) {
            try {
                await fetch(`${PROMOTION_BASE_URL}/api/v1/encounters/report`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        sourceId: myAnonymousId,
                        targetId: peer.anonymousId,
                        locationId: 'mobile_ble_bg'
                    })
                });
                console.log(`[Task] Reported encounter with ${peer.anonymousId}`);
            } catch (err) {
                console.error(`[Task] Failed to report encounter with ${peer.anonymousId}:`, err);
            }
        }

        return BackgroundFetch.BackgroundFetchResult.NewData;
    } catch (error) {
        console.error('[Task] PROXIMITY_SYNC_TASK failed:', error);
        return BackgroundFetch.BackgroundFetchResult.Failed;
    }
});

/**
 * Registers the proximity task with the OS.
 */
export const registerProximityTask = async () => {
    if (Platform.OS === 'web') {
        console.log('[Task] BackgroundFetch not supported on web. Skipping registration.');
        return;
    }
    try {
        console.log('[Task] Registering PROXIMITY_SYNC_TASK...');
        await BackgroundFetch.registerTaskAsync(PROXIMITY_SYNC_TASK, {
            minimumInterval: 60 * 15, // 15 minutes
            stopOnTerminate: false,    // Stay alive after app close
            startOnBoot: true,        // Restart on device reboot
        });
        console.log('[Task] Registration successful.');
    } catch (err) {
        console.log('[Task] Registration failed:', err);
    }
};

/**
 * Unregisters the proximity task.
 */
export const unregisterProximityTask = async () => {
    if (Platform.OS === 'web') return;
    return BackgroundFetch.unregisterTaskAsync(PROXIMITY_SYNC_TASK);
};

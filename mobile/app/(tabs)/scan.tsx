import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Dimensions } from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { ScanResultOverlay } from '@/components/ScanResultOverlay';
import { useGateValidation } from '@/hooks/useGateValidation';
import { useBiometricAuth } from '@/hooks/useBiometricAuth';
import { MeshBackground } from '@/components/MeshBackground';
import { Camera, Lock, Fingerprint } from 'lucide-react-native';

const { width } = Dimensions.get('window');
const SCAN_AREA_SIZE = width * 0.7;

export default function ScannerScreen() {
    const [permission, requestPermission] = useCameraPermissions();
    const { validateToken, status, result, reset } = useGateValidation();
    const { authenticate, isAuthenticated } = useBiometricAuth();
    const [scanned, setScanned] = useState(false);

    useEffect(() => {
        if (!permission) {
            requestPermission();
        }
    }, []);

    const handleBarCodeScanned = ({ data }: { data: string }) => {
        if (scanned || status === 'validating') return;
        setScanned(true);
        validateToken(data);
    };

    const handleDismiss = () => {
        reset();
        setScanned(false);
    };

    if (!permission) {
        return <View style={styles.container}><MeshBackground /></View>;
    }

    if (!permission.granted) {
        return (
            <View style={styles.container}>
                <MeshBackground />
                <View style={styles.centerContainer}>
                    <Camera size={64} color="#52525b" strokeWidth={1} />
                    <Text style={styles.mainText}>CAMERA PERMISSION REQUIRED</Text>
                    <TouchableOpacity style={styles.button} onPress={requestPermission}>
                        <Text style={styles.buttonText}>GRANT ACCESS</Text>
                    </TouchableOpacity>
                </View>
            </View>
        );
    }

    if (!isAuthenticated) {
        return (
            <View style={styles.container}>
                <MeshBackground />
                <View style={styles.centerContainer}>
                    <View style={styles.lockIconContainer}>
                        <Lock size={48} color="#0891B2" strokeWidth={1.5} />
                    </View>
                    <Text style={styles.mainText}>BIOMETRIC RE-AUTH REQUIRED</Text>
                    <Text style={styles.subText}>VERIFY IDENTITY TO ACCESS GATE SCANNER</Text>
                    <TouchableOpacity 
                        style={[styles.button, styles.primaryButton]} 
                        onPress={() => authenticate('Access Gate Scanner')}
                    >
                        <Fingerprint size={20} color="#000" style={{ marginRight: 8 }} />
                        <Text style={styles.primaryButtonText}>UNLOCK SCANNER</Text>
                    </TouchableOpacity>
                </View>
            </View>
        );
    }

    return (
        <View style={styles.container}>
            <CameraView
                style={StyleSheet.absoluteFillObject}
                facing="back"
                onBarcodeScanned={scanned ? undefined : handleBarCodeScanned}
                barcodeScannerSettings={{
                    barcodeTypes: ['qr'],
                }}
            >
                <View style={styles.overlay}>
                    <View style={styles.unfocusedContainer} />
                    <View style={styles.middleContainer}>
                        <View style={styles.unfocusedContainer} />
                        <View style={styles.scanArea}>
                            {/* Decorative corners */}
                            <View style={[styles.corner, styles.topLeft]} />
                            <View style={[styles.corner, styles.topRight]} />
                            <View style={[styles.corner, styles.bottomLeft]} />
                            <View style={[styles.corner, styles.bottomRight]} />
                            
                            <View style={styles.scanLine} />
                        </View>
                        <View style={styles.unfocusedContainer} />
                    </View>
                    <View style={styles.unfocusedContainer}>
                        <Text style={styles.label}>ALIGN QR CODE WITHIN FRAME</Text>
                    </View>
                </View>
            </CameraView>

            <ScanResultOverlay 
                status={status} 
                result={result} 
                onDismiss={handleDismiss} 
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#000',
    },
    centerContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        padding: 40,
    },
    lockIconContainer: {
        width: 100,
        height: 100,
        borderRadius: 50,
        backgroundColor: 'rgba(8, 145, 178, 0.1)',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 32,
        borderWidth: 1,
        borderColor: 'rgba(8, 145, 178, 0.2)',
    },
    mainText: {
        color: '#f4f4f5',
        fontSize: 14,
        fontWeight: '800',
        letterSpacing: 2,
        marginBottom: 12,
        textAlign: 'center',
    },
    subText: {
        color: '#71717a',
        fontSize: 10,
        fontWeight: '600',
        letterSpacing: 1,
        marginBottom: 40,
        textAlign: 'center',
    },
    button: {
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        paddingVertical: 16,
        paddingHorizontal: 32,
        borderRadius: 16,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
        flexDirection: 'row',
        alignItems: 'center',
    },
    primaryButton: {
        backgroundColor: '#0891B2',
        borderColor: '#0891B2',
    },
    buttonText: {
        color: '#f4f4f5',
        fontWeight: '700',
        letterSpacing: 1,
    },
    primaryButtonText: {
        color: '#000',
        fontWeight: '800',
        letterSpacing: 1,
    },
    overlay: {
        flex: 1,
        backgroundColor: 'transparent',
    },
    unfocusedContainer: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.6)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    middleContainer: {
        flexDirection: 'row',
        height: SCAN_AREA_SIZE,
    },
    scanArea: {
        width: SCAN_AREA_SIZE,
        height: SCAN_AREA_SIZE,
        backgroundColor: 'transparent',
        position: 'relative',
    },
    label: {
        color: 'rgba(255, 255, 255, 0.4)',
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 3,
        marginTop: 20,
    },
    corner: {
        position: 'absolute',
        width: 40,
        height: 40,
        borderColor: '#0891B2',
    },
    topLeft: {
        top: 0,
        left: 0,
        borderTopWidth: 4,
        borderLeftWidth: 4,
        borderTopLeftRadius: 16,
    },
    topRight: {
        top: 0,
        right: 0,
        borderTopWidth: 4,
        borderRightWidth: 4,
        borderTopRightRadius: 16,
    },
    bottomLeft: {
        bottom: 0,
        left: 0,
        borderBottomWidth: 4,
        borderLeftWidth: 4,
        borderBottomLeftRadius: 16,
    },
    bottomRight: {
        bottom: 0,
        right: 0,
        borderBottomWidth: 4,
        borderRightWidth: 4,
        borderBottomRightRadius: 16,
    },
    scanLine: {
        position: 'absolute',
        top: '50%',
        left: '10%',
        right: '10%',
        height: 2,
        backgroundColor: 'rgba(8, 145, 178, 0.5)',
        shadowColor: '#0891B2',
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 1,
        shadowRadius: 10,
    }
});

import React, { useEffect } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Animated } from 'react-native';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react-native';
import { ValidationResult, ValidationStatus } from '@/hooks/useGateValidation';

interface ScanResultOverlayProps {
    status: ValidationStatus;
    result: ValidationResult | null;
    onDismiss: () => void;
}

export const ScanResultOverlay: React.FC<ScanResultOverlayProps> = ({ status, result, onDismiss }) => {
    const fadeAnim = React.useRef(new Animated.Value(0)).current;

    useEffect(() => {
        if (status !== 'idle') {
            Animated.timing(fadeAnim, {
                toValue: 1,
                duration: 300,
                useNativeDriver: true,
            }).start();

            if (status === 'success' || status === 'error') {
                const timer = setTimeout(() => {
                    handleDismiss();
                }, 3000);
                return () => clearTimeout(timer);
            }
        }
    }, [status]);

    const handleDismiss = () => {
        Animated.timing(fadeAnim, {
            toValue: 0,
            duration: 200,
            useNativeDriver: true,
        }).start(() => onDismiss());
    };

    if (status === 'idle') return null;

    const isSuccess = result?.status === 'GREEN';
    const Icon = status === 'validating' ? Loader2 : isSuccess ? CheckCircle2 : XCircle;
    const color = isSuccess ? '#22c55e' : '#ef4444';
    const bgColor = isSuccess ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)';

    return (
        <Animated.View style={[styles.container, { opacity: fadeAnim }]}>
            <TouchableOpacity 
                activeOpacity={1} 
                style={styles.backdrop} 
                onPress={handleDismiss} 
            >
                <View style={[styles.content, { backgroundColor: 'rgba(24, 24, 27, 0.95)', borderColor: color }]}>
                    <View style={[styles.iconContainer, { backgroundColor: bgColor }]}>
                        <Icon size={64} color={color} strokeWidth={1.5} />
                    </View>
                    
                    <Text style={[styles.title, { color: color }]}>
                        {status === 'validating' ? 'VALIDATING...' : isSuccess ? 'ACCESS GRANTED' : 'ACCESS DENIED'}
                    </Text>
                    
                    {result && (
                        <Text style={styles.message}>
                            {result.message.toUpperCase()}
                        </Text>
                    )}

                    {status !== 'validating' && (
                        <Text style={styles.hint}>TAP TO RESUME</Text>
                    )}
                </View>
            </TouchableOpacity>
        </Animated.View>
    );
};

const styles = StyleSheet.create({
    container: {
        ...StyleSheet.absoluteFillObject,
        zIndex: 1000,
    },
    backdrop: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        justifyContent: 'center',
        padding: 40,
    },
    content: {
        borderRadius: 32,
        padding: 40,
        alignItems: 'center',
        borderWidth: 1,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.5,
        shadowRadius: 20,
        elevation: 10,
    },
    iconContainer: {
        width: 120,
        height: 120,
        borderRadius: 60,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 24,
    },
    title: {
        fontSize: 24,
        fontWeight: '900',
        letterSpacing: 2,
        marginBottom: 8,
    },
    message: {
        color: '#a1a1aa',
        fontSize: 14,
        textAlign: 'center',
        fontWeight: '600',
        letterSpacing: 1,
    },
    hint: {
        marginTop: 32,
        color: '#52525b',
        fontSize: 10,
        fontWeight: '800',
        letterSpacing: 2,
    }
});

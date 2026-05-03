import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput, Alert, ActivityIndicator } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { ShieldAlert, ChevronLeft, CheckCircle2 } from 'lucide-react-native';
import { useRouter } from 'expo-router';
import { PROMOTION_BASE_URL } from '@/constants/Config';

/**
 * Story 4.2: Confirmed Positive Intake
 * Screen for Health Center staff to register positive lab results.
 */
export default function PositiveIntakeScreen() {
  const router = useRouter();
  const [anonymousId, setAnonymousId] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleConfirm = async () => {
    if (!anonymousId || anonymousId.length < 8) {
      Alert.alert('Invalid Entry', 'Please enter a valid Anonymous ID.');
      return;
    }

    Alert.alert(
      'Confirm Action',
      `Are you sure you want to promote ID ${anonymousId} to CONFIRMED status? This will trigger a campus-wide containment cascade.`,
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'CONFIRM POSITIVE', 
          style: 'destructive',
          onPress: executePromotion
        },
      ]
    );
  };

  const executePromotion = async () => {
    setLoading(true);
    try {
      // In production, this would use a secure base URL from config
      const response = await fetch(`${PROMOTION_BASE_URL}/api/v1/health/confirmed`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // Authentication headers would normally be injected by service-layer
        },
        body: JSON.stringify({ anonymousId }),
      });

      if (response.ok) {
        setSuccess(true);
      } else {
        const errorText = await response.text();
        throw new Error(errorText || 'Promotion failed');
      }
    } catch (error) {
      Alert.alert('System Error', 'Failed to communicate with promotion-service. Ensure your Health Center credentials are active.');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <View style={styles.container}>
        <MeshBackground confirmedCount={1} unconfirmedCount={0} />
        <View style={styles.successContent}>
          <CheckCircle2 color="#22c55e" size={80} />
          <Text style={styles.successTitle}>INTAKE COMPLETE</Text>
          <Text style={styles.successSubtitle}>
            User {anonymousId.substring(0, 8)}... has been promoted to CONFIRMED.
            Containment graphs are being updated and notifications dispatched.
          </Text>
          <TouchableOpacity 
            style={styles.primaryButton}
            onPress={() => router.replace('/admin')}
          >
            <Text style={styles.buttonText}>BACK TO DASHBOARD</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <MeshBackground confirmedCount={0} unconfirmedCount={0} />
      
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <ChevronLeft color="#f4f4f5" size={24} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Positive Intake</Text>
        <View style={{ width: 24 }} />
      </View>

      <View style={styles.content}>
        <View style={styles.instructionBox}>
          <ShieldAlert color="#ef4444" size={24} />
          <Text style={styles.instructionText}>
            Enter the student's Anonymous ID from their health profile or scanned QR code.
          </Text>
        </View>

        <View style={styles.inputSection}>
          <Text style={styles.inputLabel}>ANONYMOUS ID</Text>
          <TextInput
            style={styles.input}
            placeholder="e.g. 550e8400-e29b-41d4..."
            placeholderTextColor="#3f3f46"
            value={anonymousId}
            onChangeText={setAnonymousId}
            autoCapitalize="none"
            autoCorrect={false}
          />
        </View>

        <TouchableOpacity 
          style={[styles.primaryButton, loading && styles.buttonDisabled]}
          onPress={handleConfirm}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="#ffffff" />
          ) : (
            <Text style={styles.buttonText}>CONFIRM POSITIVE RESULT</Text>
          )}
        </TouchableOpacity>

        <Text style={styles.disclaimer}>
          ALL ACTIONS IN THIS PORTAL ARE AUDITED. 
          Unauthorized status promotion is a security violation.
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#09090b',
  },
  header: {
    paddingTop: 60,
    paddingHorizontal: 24,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 32,
  },
  backButton: {
    padding: 8,
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 12,
  },
  headerTitle: {
    color: '#f4f4f5',
    fontSize: 18,
    fontWeight: '700',
    letterSpacing: 1,
  },
  content: {
    padding: 24,
    flex: 1,
  },
  instructionBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.03)',
    padding: 20,
    borderRadius: 20,
    marginBottom: 40,
    gap: 16,
  },
  instructionText: {
    color: '#a1a1aa',
    fontSize: 14,
    flex: 1,
    lineHeight: 20,
  },
  inputSection: {
    marginBottom: 40,
  },
  inputLabel: {
    color: '#71717a',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 2,
    marginBottom: 12,
  },
  input: {
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 16,
    padding: 20,
    color: '#f4f4f5',
    fontSize: 16,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  primaryButton: {
    backgroundColor: '#ef4444',
    paddingVertical: 20,
    borderRadius: 20,
    alignItems: 'center',
    marginBottom: 24,
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 1,
  },
  disclaimer: {
    color: '#3f3f46',
    fontSize: 11,
    textAlign: 'center',
    lineHeight: 16,
    letterSpacing: 1,
  },
  successContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  successTitle: {
    color: '#22c55e',
    fontSize: 28,
    fontWeight: '900',
    letterSpacing: -0.5,
    marginTop: 24,
    marginBottom: 16,
  },
  successSubtitle: {
    color: '#a1a1aa',
    fontSize: 15,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 48,
  },
});

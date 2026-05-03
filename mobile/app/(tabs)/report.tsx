import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert, useWindowDimensions } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { Thermometer, Wind, Activity, CheckCircle2, ChevronLeft, ArrowRight } from 'lucide-react-native';
import { useAuth } from '@/context/AuthContext';
import { useRouter, Link } from 'expo-router';
import { FORM_BASE_URL } from '@/constants/Config';

/**
 * Story 4.1 & 8.2: Symptom Self-Reporting Flow.
 * Allows users to report symptoms and receive immediate instructions.
 * Responsive layout for Web/Desktop.
 */
export default function SymptomReportScreen() {
  const { anonymousId } = useAuth();
  const { width } = useWindowDimensions();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  
  const isDesktop = width > 768;

  const [symptoms, setSymptoms] = useState({
    hasFever: false,
    hasCough: false,
    hasShortnessOfBreath: false,
  });

  const toggleSymptom = (key: keyof typeof symptoms) => {
    setSymptoms(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const handleSubmit = async () => {
    if (!anonymousId) {
      Alert.alert('Error', 'No authenticated identity found.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${FORM_BASE_URL}/api/v1/surveys`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          anonymousId,
          ...symptoms,
          timestamp: new Date().toISOString(),
        }),
      });

      if (response.ok) {
        setSubmitted(true);
      } else {
        throw new Error('Submission failed');
      }
    } catch (error) {
      Alert.alert('Submission Failed', 'Could not reach the health gateway. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <View style={styles.container}>
        <MeshBackground confirmedCount={0} unconfirmedCount={0} />
        <View style={styles.successContent}>
          <View style={styles.successIcon}>
            <CheckCircle2 size={64} color="#22c55e" />
          </View>
          <Text style={styles.successTitle}>STATUS: SUSPECT</Text>
          <Text style={styles.successSubtitle}>
            Based on your report, your campus access has been restricted to protect the community.
          </Text>
          
          <View style={[styles.instructionBox, isDesktop && { maxWidth: 600 }]}>
            <Text style={styles.instructionTitle}>IMMEDIATE ACTIONS:</Text>
            <Text style={styles.instructionItem}>• Relocate to your remote learning/work site immediately.</Text>
            <Text style={styles.instructionItem}>• Schedule a COVID-19 PCR test through the Health Portal.</Text>
            <Text style={styles.instructionItem}>• Maintain strict isolation until test results are verified.</Text>
          </View>

          <TouchableOpacity 
            style={[styles.primaryButton, isDesktop && { width: 400 }]}
            onPress={() => router.replace('/(tabs)')}
          >
            <Text style={styles.buttonText}>RETURN TO DASHBOARD</Text>
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
        <Text style={styles.headerTitle}>Symptom Report</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={[styles.scrollContent, isDesktop && { paddingHorizontal: width * 0.15 }]}>
        <Text style={styles.prompt}>Select all that apply to you currently:</Text>

        <View style={isDesktop ? styles.symptomGrid : styles.symptomList}>
          <TouchableOpacity 
            style={[styles.symptomCard, symptoms.hasFever && styles.symptomCardActive, isDesktop && styles.symptomCardDesktop]} 
            onPress={() => toggleSymptom('hasFever')}
          >
            <Thermometer color={symptoms.hasFever ? '#f4f4f5' : '#0891B2'} size={32} />
            <View style={styles.symptomTextContainer}>
              <Text style={styles.symptomLabel}>Fever / Chills</Text>
              <Text style={styles.symptomDesc}>Temperature above 100.4°F (38°C)</Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.symptomCard, symptoms.hasCough && styles.symptomCardActive, isDesktop && styles.symptomCardDesktop]} 
            onPress={() => toggleSymptom('hasCough')}
          >
            <Activity color={symptoms.hasCough ? '#f4f4f5' : '#0891B2'} size={32} />
            <View style={styles.symptomTextContainer}>
              <Text style={styles.symptomLabel}>Persistent Cough</Text>
              <Text style={styles.symptomDesc}>New or worsening cough</Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[styles.symptomCard, symptoms.hasShortnessOfBreath && styles.symptomCardActive, isDesktop && styles.symptomCardDesktop]} 
            onPress={() => toggleSymptom('hasShortnessOfBreath')}
          >
            <Wind color={symptoms.hasShortnessOfBreath ? '#f4f4f5' : '#0891B2'} size={32} />
            <View style={styles.symptomTextContainer}>
              <Text style={styles.symptomLabel}>Difficult Breathing</Text>
              <Text style={styles.symptomDesc}>Shortness of breath or chest tightness</Text>
            </View>
          </TouchableOpacity>
        </View>

        <View style={[styles.disclaimerBox, isDesktop && { marginTop: 40, padding: 24 }]}>
          <Text style={styles.disclaimerText}>
            Submitting this report will impact your campus access status immediately. 
            Knowingly providing false information is a violation of University conduct code.
          </Text>
        </View>
      </ScrollView>

      <View style={[styles.footer, isDesktop && { paddingHorizontal: width * 0.15 }]}>
        <TouchableOpacity 
          style={[styles.primaryButton, loading && { opacity: 0.5 }, isDesktop && { width: 300, alignSelf: 'flex-end' }]} 
          onPress={handleSubmit}
          disabled={loading}
        >
          <Text style={styles.buttonText}>{loading ? 'SUBMITTING...' : 'CONFIRM REPORT'}</Text>
          <ArrowRight color="#ffffff" size={20} />
        </TouchableOpacity>
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
    marginBottom: 20,
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
    letterSpacing: 0.5,
  },
  scrollContent: {
    padding: 24,
  },
  prompt: {
    color: '#a1a1aa',
    fontSize: 16,
    marginBottom: 24,
  },
  symptomCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    padding: 20,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    marginBottom: 16,
    gap: 20,
  },
  symptomCardActive: {
    backgroundColor: '#0891B2',
    borderColor: '#0891B2',
  },
  symptomTextContainer: {
    flex: 1,
  },
  symptomGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 20,
    justifyContent: 'center',
  },
  symptomList: {
    flexDirection: 'column',
    gap: 16,
  },
  symptomCardDesktop: {
    width: '45%',
    minWidth: 300,
    marginBottom: 0,
  },
  symptomLabel: {
    color: '#f4f4f5',
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 4,
  },
  symptomDesc: {
    color: '#71717a',
    fontSize: 13,
  },
  disclaimerBox: {
    marginTop: 24,
    padding: 16,
    backgroundColor: 'rgba(239, 68, 68, 0.05)',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(239, 68, 68, 0.1)',
  },
  disclaimerText: {
    color: '#ef4444',
    fontSize: 12,
    lineHeight: 18,
    textAlign: 'center',
  },
  footer: {
    padding: 24,
    paddingBottom: 40,
  },
  primaryButton: {
    backgroundColor: '#0891B2',
    paddingVertical: 18,
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 1,
  },
  successContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  successIcon: {
    marginBottom: 32,
    padding: 24,
    backgroundColor: 'rgba(34, 197, 94, 0.1)',
    borderRadius: 48,
  },
  successTitle: {
    color: '#ef4444', // SUSPECT is high priority
    fontSize: 32,
    fontWeight: '900',
    letterSpacing: -1,
    marginBottom: 16,
  },
  successSubtitle: {
    color: '#a1a1aa',
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 40,
  },
  instructionBox: {
    width: '100%',
    backgroundColor: 'rgba(255,255,255,0.03)',
    padding: 24,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
    marginBottom: 48,
  },
  instructionTitle: {
    color: '#f4f4f5',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 16,
  },
  instructionItem: {
    color: '#d4d4d8',
    fontSize: 14,
    lineHeight: 22,
    marginBottom: 8,
  },
});

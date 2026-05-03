import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ActivityIndicator, Alert } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { DynamicForm } from '@/components/DynamicForm';
import { ChevronLeft, ArrowRight, ClipboardCheck, Paperclip, FileCheck } from 'lucide-react-native';
import { useRouter } from 'expo-router';
import * as DocumentPicker from 'expo-document-picker';
import { useAuth } from '@/context/AuthContext';
import { FORM_BASE_URL } from '@/constants/Config';

export default function QuestionnaireScreen() {
  const router = useRouter();
  const { anonymousId } = useAuth();
  const [questionnaire, setQuestionnaire] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [responses, setResponses] = useState<Record<string, any>>({});
  const [attachment, setAttachment] = useState<any>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchActiveQuestionnaire();
  }, []);

  const fetchActiveQuestionnaire = async () => {
    try {
      const response = await fetch(`${FORM_BASE_URL}/api/v1/questionnaires/active`);
      if (response.ok) {
        const data = await response.json();
        setQuestionnaire(data);
      } else {
        throw new Error('No active questionnaire found');
      }
    } catch (error) {
      console.error('Failed to fetch questionnaire:', error);
      Alert.alert('Error', 'Could not load health survey. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handlePickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['application/pdf', 'image/*'],
        copyToCacheDirectory: true
      });

      if (!result.canceled) {
        setAttachment(result.assets[0]);
      }
    } catch (err) {
      console.error('Pick document error:', err);
    }
  };

  const uploadAttachment = async () => {
    if (!attachment) return null;
    
    setUploading(true);
    try {
      const formData = new FormData();
      // @ts-ignore
      formData.append('file', {
        uri: attachment.uri,
        name: attachment.name,
        type: attachment.mimeType || 'application/octet-stream'
      });

      const response = await fetch(`${FORM_BASE_URL}/api/v1/attachments`, {
        method: 'POST',
        body: formData,
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      if (response.ok) {
        const data = await response.json();
        return data.filename;
      }
      throw new Error('Upload failed');
    } catch (error) {
      console.error('Upload error:', error);
      Alert.alert('Upload Error', 'Failed to upload certificate.');
      return null;
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async () => {
    if (!anonymousId) return;
    
    setSubmitting(true);
    try {
      let attachmentPath = null;
      if (attachment) {
        attachmentPath = await uploadAttachment();
        if (!attachmentPath) {
          setSubmitting(false);
          return;
        }
      }

      const response = await fetch(`${FORM_BASE_URL}/api/v1/surveys`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          anonymousId,
          responses,
          attachmentPath,
          timestamp: new Date().toISOString()
        })
      });

      if (response.ok) {
        router.replace('/(tabs)');
        Alert.alert('Submitted', 'Thank you for your report. Your status has been updated.');
      } else {
        throw new Error('Submission failed');
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to submit survey. Please check your connection.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#0891B2" />
      </View>
    );
  }

  if (!questionnaire) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>No active health survey available.</Text>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <Text style={styles.backButtonText}>GO BACK</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <MeshBackground confirmedCount={0} unconfirmedCount={0} />
      
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.iconButton}>
          <ChevronLeft color="#f4f4f5" size={24} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Health Screening</Text>
        <View style={{ width: 40 }} />
      </View>

      <View style={styles.content}>
        <View style={styles.introBox}>
          <ClipboardCheck color="#0891B2" size={24} />
          <View>
            <Text style={styles.introTitle}>{questionnaire.title}</Text>
            <Text style={styles.introDesc}>{questionnaire.description}</Text>
          </View>
        </View>

        <DynamicForm 
          questions={questionnaire.questions} 
          onSubmit={() => {}} // Controlled by external button
          // Pass setResponses to capture state
        />
        
        <View style={styles.attachmentBox}>
          <Text style={styles.sectionTitle}>Medical Certificate (Optional)</Text>
          <TouchableOpacity 
            style={[styles.attachmentButton, attachment && styles.attachmentActive]} 
            onPress={handlePickDocument}
          >
            {attachment ? (
              <FileCheck color="#0891B2" size={20} />
            ) : (
              <Paperclip color="#71717a" size={20} />
            )}
            <Text style={[styles.attachmentText, attachment && styles.attachmentTextActive]}>
              {attachment ? attachment.name : 'Attach test results or certificate'}
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.footer}>
        <TouchableOpacity 
          style={[styles.submitButton, submitting && { opacity: 0.5 }]} 
          onPress={handleSubmit}
          disabled={submitting}
        >
          <Text style={styles.submitButtonText}>
            {submitting ? 'SUBMITTING...' : 'SUBMIT REPORT'}
          </Text>
          <ArrowRight color="#fff" size={20} />
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
  loadingContainer: {
    flex: 1,
    backgroundColor: '#09090b',
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorContainer: {
    flex: 1,
    backgroundColor: '#09090b',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 40,
  },
  errorText: {
    color: '#a1a1aa',
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 24,
  },
  header: {
    paddingTop: 60,
    paddingHorizontal: 24,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  iconButton: {
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
  content: {
    flex: 1,
    paddingHorizontal: 24,
  },
  introBox: {
    flexDirection: 'row',
    gap: 16,
    backgroundColor: 'rgba(8, 145, 178, 0.05)',
    padding: 20,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: 'rgba(8, 145, 178, 0.1)',
    marginBottom: 32,
  },
  introTitle: {
    color: '#f4f4f5',
    fontSize: 18,
    fontWeight: '800',
    marginBottom: 4,
  },
  introDesc: {
    color: '#71717a',
    fontSize: 14,
    lineHeight: 20,
  },
  footer: {
    padding: 24,
    paddingBottom: 40,
  },
  submitButton: {
    backgroundColor: '#0891B2',
    paddingVertical: 18,
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 1,
  },
  backButton: {
    paddingVertical: 12,
    paddingHorizontal: 24,
    backgroundColor: '#27272a',
    borderRadius: 12,
  },
  backButtonText: {
    color: '#fff',
    fontWeight: '700',
  },
  attachmentBox: {
    marginTop: 32,
    gap: 12,
  },
  sectionTitle: {
    color: '#f4f4f5',
    fontSize: 16,
    fontWeight: '700',
  },
  attachmentButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: 'rgba(255,255,255,0.03)',
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: '#3f3f46',
  },
  attachmentActive: {
    backgroundColor: 'rgba(8, 145, 178, 0.05)',
    borderColor: '#0891B2',
    borderStyle: 'solid',
  },
  attachmentText: {
    color: '#71717a',
    fontSize: 14,
  },
  attachmentTextActive: {
    color: '#f4f4f5',
    fontWeight: '600',
  }
});

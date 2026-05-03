import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, Platform, useWindowDimensions, ScrollView, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import QRCode from 'react-native-qrcode-svg';
import axios from 'axios';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Shield } from 'lucide-react-native';

import { IDENTITY_BASE_URL, AUTH_BASE_URL } from '../constants/Config';
const IDENTITY_URL = `${IDENTITY_BASE_URL}/api/v1/identities/visitor`;
const AUTH_URL = `${AUTH_BASE_URL}/api/v1/auth/visitor/handoff`;

export default function VisitorRegistrationScreen() {
  const { width } = useWindowDimensions();
  const isDesktop = width >= 768;
  
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [reason, setReason] = useState('');
  
  const [isLoading, setIsLoading] = useState(false);
  const [handoffToken, setHandoffToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleRegister = async () => {
    if (!name || !email || !reason) {
      setError('Please fill in all fields.');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      // 1. Register visitor with Identity Service
      const idResponse = await axios.post(IDENTITY_URL, {
        name,
        email,
        reason_for_visit: reason
      });
      const anonymousId = idResponse.data.anonymousId;

      // 2. Request Handoff Token from Auth Service
      const authResponse = await axios.post(AUTH_URL, {
        anonymousId
      });
      
      setHandoffToken(authResponse.data.handoffPayload);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Failed to register visitor.');
      console.error(e);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-slate-50 dark:bg-slate-900">
      <ScrollView contentContainerStyle={{ flexGrow: 1, justifyContent: 'center', alignItems: 'center', padding: 20 }}>
        <View className={`w-full ${isDesktop ? 'max-w-md' : 'max-w-sm'} bg-white dark:bg-slate-800 rounded-3xl p-8 shadow-sm border border-slate-100 dark:border-slate-700`} accessibilityLabel="Visitor Registration Form">
          <View className="items-center mb-8">
            <View className="w-16 h-16 bg-blue-100 dark:bg-blue-900/30 rounded-2xl items-center justify-center mb-4" accessibilityRole="image">
              <Shield size={32} color="#3b82f6" />
            </View>
            <Text className="text-2xl font-bold text-slate-900 dark:text-white text-center" accessibilityRole="header">
              Visitor Registration
            </Text>
            <Text className="text-slate-500 dark:text-slate-400 text-center mt-2">
              Register for temporary campus access
            </Text>
          </View>

          {error && (
            <View className="bg-red-50 dark:bg-red-900/20 p-4 rounded-xl mb-6 border border-red-100 dark:border-red-900/30" accessibilityRole="alert">
              <Text className="text-red-600 dark:text-red-400 text-sm text-center">{error}</Text>
            </View>
          )}

          {!handoffToken ? (
            <View className="space-y-4 gap-4">
              <View>
                <Text className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">Full Name</Text>
                <TextInput
                  value={name}
                  onChangeText={setName}
                  placeholder="Jane Doe"
                  className="w-full bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl px-4 py-3.5 text-slate-900 dark:text-white"
                  placeholderTextColor="#94a3b8"
                />
              </View>

              <View>
                <Text className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">Email Address</Text>
                <TextInput
                  value={email}
                  onChangeText={setEmail}
                  placeholder="jane@example.com"
                  keyboardType="email-address"
                  autoCapitalize="none"
                  className="w-full bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl px-4 py-3.5 text-slate-900 dark:text-white"
                  placeholderTextColor="#94a3b8"
                />
              </View>

              <View>
                <Text className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">Reason for Visit</Text>
                <TextInput
                  value={reason}
                  onChangeText={setReason}
                  placeholder="Guest Lecturer"
                  className="w-full bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl px-4 py-3.5 text-slate-900 dark:text-white"
                  placeholderTextColor="#94a3b8"
                />
              </View>

              <Pressable 
                onPress={handleRegister}
                disabled={isLoading}
                className={`w-full rounded-xl py-4 items-center justify-center mt-4 ${isLoading ? 'bg-blue-400' : 'bg-blue-600 active:bg-blue-700'}`}
              >
                {isLoading ? (
                  <ActivityIndicator color="#ffffff" />
                ) : (
                  <Text className="text-white font-semibold text-lg">Generate Pass</Text>
                )}
              </Pressable>
            </View>
          ) : (
            <View className="items-center space-y-6 gap-6">
              <View className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100">
                <QRCode
                  value={handoffToken}
                  size={200}
                  color="black"
                  backgroundColor="white"
                />
              </View>
              
              <View className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-xl w-full border border-blue-100 dark:border-blue-900/30">
                <Text className="text-blue-800 dark:text-blue-300 text-center font-medium">
                  Scan this QR code with the CircleGuard mobile app upon arrival.
                </Text>
              </View>
              
              <Pressable 
                onPress={() => {
                  setHandoffToken(null);
                  setName('');
                  setEmail('');
                  setReason('');
                }}
                className="w-full bg-slate-100 dark:bg-slate-700 active:bg-slate-200 dark:active:bg-slate-600 rounded-xl py-4 items-center justify-center"
              >
                <Text className="text-slate-700 dark:text-slate-300 font-semibold">Register Another</Text>
              </Pressable>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

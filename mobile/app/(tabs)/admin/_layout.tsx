import React from 'react';
import { Stack, useRouter } from 'expo-router';
import { usePermission } from '@/hooks/usePermission';
import { View, Text, TouchableOpacity } from 'react-native';

export default function AdminLayout() {
  const { hasPermission } = usePermission();
  const router = useRouter();

  if (!hasPermission('dashboard:view')) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#000', padding: 20 }}>
        <Text style={{ color: '#ef4444', fontSize: 20, fontWeight: 'bold', marginBottom: 10 }}>Access Denied</Text>
        <Text style={{ color: '#a1a1aa', textAlign: 'center', marginBottom: 20 }}>
          You do not have the required permissions to view the admin portal.
        </Text>
        <TouchableOpacity 
          style={{ backgroundColor: '#27272a', padding: 12, borderRadius: 8 }}
          onPress={() => router.replace('/')}
        >
          <Text style={{ color: '#fff' }}>Return Home</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="index" options={{ title: 'Admin Dashboard' }} />
      <Stack.Screen name="analytics" options={{ title: 'Department Analytics' }} />
    </Stack>
  );
}

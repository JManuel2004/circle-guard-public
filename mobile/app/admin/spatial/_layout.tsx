import React from 'react';
import { Stack } from 'expo-router';

export default function AdminLayout() {
  return (
    <Stack screenOptions={{ 
      headerStyle: { backgroundColor: '#111' },
      headerTintColor: '#06b6d4', // Cyan 500
      headerTitleStyle: { fontWeight: 'bold' },
    }}>
      <Stack.Screen name="index" options={{ title: 'Buildings' }} />
      <Stack.Screen name="[buildingId]/floors/[floorId]" options={{ title: 'Map Editor' }} />
    </Stack>
  );
}

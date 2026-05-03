import React from 'react';
import { LayoutDashboard, Info, ScanLine, Users, Shield } from 'lucide-react-native';
import { Link, Tabs, usePathname, useRouter } from 'expo-router';
import { Platform, Pressable, View, StyleSheet, useWindowDimensions, Text, TouchableOpacity } from 'react-native';
import { useSharedValue } from 'react-native-reanimated';

import Colors from '@/constants/Colors';
import { useColorScheme } from '@/components/useColorScheme';
import { useClientOnlyValue } from '@/components/useClientOnlyValue';
import { usePermission } from '@/hooks/usePermission';
import { useAuth } from '@/context/AuthContext';
import { useMeshStats } from '@/hooks/useMeshStats';
import { MeshBackground } from '@/components/MeshBackground';

export default function TabLayout() {
  const colorScheme = useColorScheme();
  const { hasPermission } = usePermission();
  const { anonymousId, logout } = useAuth();
  const { stats } = useMeshStats(anonymousId);
  const { width } = useWindowDimensions();
  const progress = useSharedValue(0);
  const pathname = usePathname();
  const router = useRouter();

  const isDesktop = width >= 768; // Include native tablets

  const Sidebar = () => (
    <View style={styles.sidebar}>
      <Text style={styles.sidebarTitle}>CircleGuard</Text>
      
      <TouchableOpacity 
        style={[styles.sidebarItem, pathname === '/' && styles.sidebarItemActive]}
        onPress={() => router.navigate('/')}
      >
        <LayoutDashboard size={24} color={Colors[colorScheme].text} />
        <Text style={styles.sidebarItemText}>Home</Text>
      </TouchableOpacity>

      {hasPermission('gate:scan') && (
        <TouchableOpacity 
          style={[styles.sidebarItem, pathname === '/scan' && styles.sidebarItemActive]}
          onPress={() => router.navigate('/scan')}
        >
          <ScanLine size={24} color={Colors[colorScheme].text} />
          <Text style={styles.sidebarItemText}>Scan</Text>
        </TouchableOpacity>
      )}

      <TouchableOpacity 
        style={[styles.sidebarItem, pathname === '/circles' && styles.sidebarItemActive]}
        onPress={() => router.navigate('/circles')}
      >
        <Users size={24} color={Colors[colorScheme].text} />
        <Text style={styles.sidebarItemText}>Circles</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={[styles.sidebarItem, pathname === '/visitor' && styles.sidebarItemActive]}
        onPress={() => router.navigate('/visitor')}
      >
        <Users size={24} color={Colors[colorScheme].text} />
        <Text style={styles.sidebarItemText}>Visitor Mode</Text>
      </TouchableOpacity>

      <View style={{ flex: 1 }} />

      <TouchableOpacity 
        style={[styles.sidebarItem, styles.adminSidebarItem]}
        onPress={() => router.navigate('/admin')}
      >
        <Shield size={20} color="#71717a" />
        <Text style={styles.adminSidebarText}>Health Admin</Text>
      </TouchableOpacity>

      <TouchableOpacity 
        style={[styles.sidebarItem, { marginTop: 'auto', backgroundColor: 'rgba(239, 68, 68, 0.1)' }]}
        onPress={async () => {
          console.log('Logging out...');
          await logout();
          console.log('Logged out, redirecting...');
          router.replace('/login');
        }}
      >
        <Shield size={20} color="#ef4444" />
        <Text style={[styles.sidebarItemText, { color: '#ef4444' }]}>Log Out</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <View style={[styles.container, isDesktop && { flexDirection: 'row' }]}>
      <MeshBackground 
        confirmedCount={stats?.confirmedCount || 0} 
        unconfirmedCount={stats?.unconfirmedCount || 0} 
      />
      {isDesktop && <Sidebar />}
      <View style={{ flex: 1, backgroundColor: 'transparent' }}>
        <Tabs
          screenOptions={{
            tabBarActiveTintColor: Colors[colorScheme].tint,
            headerShown: isDesktop ? false : useClientOnlyValue(false, true),
            tabBarStyle: isDesktop ? { display: 'none' } : {
              backgroundColor: 'transparent',
              borderTopWidth: 0,
              elevation: 0,
            },
          }}>
          <Tabs.Screen
            name="index"
            options={{
              title: 'Home',
              tabBarIcon: ({ color }) => (
                <LayoutDashboard size={28} color={color} />
              ),
              headerLeft: () => (
                !isDesktop && (
                  <TouchableOpacity 
                    onPress={async () => {
                      console.log('Mobile logging out...');
                      await logout();
                      router.replace('/login');
                    }}
                    style={{ marginLeft: 15 }}
                  >
                    <Text style={{ color: '#ef4444', fontWeight: 'bold' }}>LOGOUT</Text>
                  </TouchableOpacity>
                )
              ),
              headerRight: () => (
                <Link href="/modal" asChild>
                  <Pressable style={{ marginRight: 15 }}>
                    {({ pressed }) => (
                      <Info
                        size={25}
                        color={Colors[colorScheme].text}
                        style={{ opacity: pressed ? 0.5 : 1 }}
                      />
                    )}
                  </Pressable>
                </Link>
              ),
            }}
          />
          {hasPermission('gate:scan') && (
            <Tabs.Screen
              name="scan"
              options={{
                title: 'Scan',
                tabBarIcon: ({ color }) => (
                  <ScanLine size={28} color={color} />
                ),
              }}
            />
          )}
          <Tabs.Screen
            name="circles"
            options={{
              title: 'Circles',
              tabBarIcon: ({ color }) => (
                <Users size={28} color={color} />
              ),
            }}
          />
          <Tabs.Screen
            name="report"
            options={{
              href: null,
            }}
          />
          <Tabs.Screen
            name="admin"
            options={{
              href: null,
              headerShown: false,
            }}
          />
        </Tabs>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  sidebar: {
    width: 250,
    backgroundColor: 'rgba(20, 20, 20, 0.9)',
    borderRightWidth: 1,
    borderRightColor: '#333',
    padding: 20,
    paddingTop: 40,
  },
  sidebarTitle: {
    color: '#fff',
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 40,
  },
  sidebarItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 15,
    paddingHorizontal: 10,
    borderRadius: 8,
    marginBottom: 10,
  },
  sidebarItemActive: {
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
  },
  sidebarItemText: {
    color: '#fff',
    fontSize: 18,
    marginLeft: 15,
  },
  adminSidebarItem: {
    borderTopWidth: 1,
    borderTopColor: '#27272a',
    paddingTop: 20,
  },
  adminSidebarText: {
    color: '#71717a',
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 15,
  },
});

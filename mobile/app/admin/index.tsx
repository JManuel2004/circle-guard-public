import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { ShieldAlert, Map, UserPlus, ChevronRight, ChevronLeft } from 'lucide-react-native';
import { useRouter } from 'expo-router';

/**
 * Story 4.2: Admin Dashboard
 * Central hub for Health Center operations.
 */
export default function AdminDashboard() {
  const router = useRouter();

  const actions = [
    {
      id: 'intake',
      title: 'Register Positive Test',
      subtitle: 'Officially confirm a positive lab result',
      icon: <ShieldAlert color="#ef4444" size={24} />,
      route: '/admin/intake' as const,
    },
    {
      id: 'hotspots',
      title: 'Hotspot Analysis',
      subtitle: 'View active case clusters on campus',
      icon: <Map color="#0891B2" size={24} />,
      route: '/admin/spatial' as any,
    },
  ];

  return (
    <View style={styles.container}>
      <MeshBackground confirmedCount={0} unconfirmedCount={0} />
      
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
          <ChevronLeft color="#f4f4f5" size={24} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Health Admin</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.sectionTitle}>COMMAND CENTER</Text>
        
        {actions.map(action => (
          <TouchableOpacity 
            key={action.id} 
            style={styles.card}
            onPress={() => router.push(action.route)}
          >
            <View style={styles.iconContainer}>
              {action.icon}
            </View>
            <View style={styles.cardText}>
              <Text style={styles.cardTitle}>{action.title}</Text>
              <Text style={styles.cardSubtitle}>{action.subtitle}</Text>
            </View>
            <ChevronRight color="#3f3f46" size={20} />
          </TouchableOpacity>
        ))}
      </ScrollView>
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
  },
  sectionTitle: {
    color: '#71717a',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 2,
    marginBottom: 24,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    padding: 20,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    marginBottom: 16,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 16,
    backgroundColor: 'rgba(255,255,255,0.03)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  cardText: {
    flex: 1,
  },
  cardTitle: {
    color: '#f4f4f5',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
  },
  cardSubtitle: {
    color: '#71717a',
    fontSize: 12,
  },
});

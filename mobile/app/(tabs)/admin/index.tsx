import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { Activity, ShieldAlert, Users, Calendar } from 'lucide-react-native';

interface SummaryData {
  activeCount?: number;
  suspectCount?: number;
  probableCount?: number;
  confirmedCount?: number;
  fencedCount?: number;
  recoveredCount?: number;
  totalUsers?: number;
}

export default function AdminDashboardScreen() {
  const [summary, setSummary] = useState<SummaryData | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    fetchSummary();
  }, []);

  const fetchSummary = async () => {
    try {
      const response = await fetch('http://localhost:8084/api/v1/analytics/summary');
      if (response.ok) {
        const data = await response.json();
        setSummary(data);
      }
    } catch (err) {
      console.error('Failed to fetch summary:', err);
    } finally {
      setLoading(false);
    }
  };

  const MetricCard = ({ title, value, icon, color }: any) => (
    <View style={[styles.card, { borderLeftColor: color }]}>
      <View style={styles.cardHeader}>
        <Text style={styles.cardTitle}>{title}</Text>
        {icon}
      </View>
      <Text style={[styles.cardValue, { color }]}>{value !== undefined ? value : '--'}</Text>
    </View>
  );

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Health Analytics Dashboard</Text>
        <Text style={styles.headerSubtitle}>Campus-wide aggregated metrics</Text>
      </View>

      <View style={styles.tabBar}>
        <TouchableOpacity style={styles.tabActive}>
          <Text style={styles.tabTextActive}>Overview</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.tab} onPress={() => router.navigate('/admin/analytics')}>
          <Text style={styles.tabText}>Departments</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#3b82f6" style={{ marginTop: 50 }} />
      ) : (
        <View style={styles.grid}>
          <MetricCard 
            title="Total Tracked" 
            value={summary?.totalUsers} 
            icon={<Users size={20} color="#a1a1aa" />}
            color="#3b82f6" 
          />
          <MetricCard 
            title="Active Cases" 
            value={summary?.confirmedCount || 0} 
            icon={<Activity size={20} color="#a1a1aa" />}
            color="#ef4444" 
          />
          <MetricCard 
            title="Suspect / Probable" 
            value={(summary?.suspectCount || 0) + (summary?.probableCount || 0)} 
            icon={<ShieldAlert size={20} color="#a1a1aa" />}
            color="#f59e0b" 
          />
          <MetricCard 
            title="Mandatory Fence" 
            value={summary?.fencedCount || 0} 
            icon={<Calendar size={20} color="#a1a1aa" />}
            color="#8b5cf6" 
          />
        </View>
      )}

      {/* Mock Time-Series Chart Area */}
      <View style={styles.chartSection}>
        <Text style={styles.sectionTitle}>7-Day Trend</Text>
        <View style={styles.chartPlaceholder}>
          <Text style={{ color: '#52525b' }}>Time-Series Visualization Area</Text>
          <Text style={{ color: '#52525b', fontSize: 12, marginTop: 10 }}>(Requires D3/Recharts implementation)</Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  content: {
    padding: 20,
    maxWidth: 1200,
    marginHorizontal: 'auto',
    width: '100%',
  },
  header: {
    marginBottom: 30,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#fff',
  },
  headerSubtitle: {
    fontSize: 16,
    color: '#a1a1aa',
    marginTop: 5,
  },
  tabBar: {
    flexDirection: 'row',
    marginBottom: 30,
    borderBottomWidth: 1,
    borderBottomColor: '#27272a',
  },
  tabActive: {
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderBottomWidth: 2,
    borderBottomColor: '#3b82f6',
  },
  tab: {
    paddingVertical: 10,
    paddingHorizontal: 20,
  },
  tabTextActive: {
    color: '#3b82f6',
    fontWeight: 'bold',
  },
  tabText: {
    color: '#a1a1aa',
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 20,
    marginBottom: 40,
  },
  card: {
    backgroundColor: '#18181b',
    borderRadius: 12,
    padding: 20,
    minWidth: 250,
    flex: 1,
    borderLeftWidth: 4,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  cardTitle: {
    color: '#a1a1aa',
    fontSize: 14,
    fontWeight: '600',
  },
  cardValue: {
    fontSize: 32,
    fontWeight: 'bold',
  },
  chartSection: {
    marginTop: 20,
  },
  sectionTitle: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 15,
  },
  chartPlaceholder: {
    backgroundColor: '#18181b',
    height: 300,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#27272a',
    borderStyle: 'dashed',
    justifyContent: 'center',
    alignItems: 'center',
  },
});

import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { Building2, ShieldAlert } from 'lucide-react-native';

export default function DepartmentAnalyticsScreen() {
  const [department, setDepartment] = useState('Faculty of Engineering, Design and Applied Sciences (Barberi de Ingeniería, Diseño y Ciencias Aplicadas)');
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const departments = [
    'Faculty of Administrative and Economic Sciences (Negocios y Economía)',
    'Faculty of Engineering, Design and Applied Sciences (Barberi de Ingeniería, Diseño y Ciencias Aplicadas)',
    'Faculty of Law and Social Sciences (Ciencias Jurídicas y Sociales)',
    'Faculty of Natural Sciences (Ciencias Naturales)',
    'Faculty of Health Sciences (Ciencias de la Salud)',
    'School of Education (Escuela de Educación)'
  ];

  useEffect(() => {
    fetchDepartmentStats(department);
  }, [department]);

  const fetchDepartmentStats = async (dept: string) => {
    setLoading(true);
    try {
      const response = await fetch(`http://localhost:8084/api/v1/analytics/department/${dept}`);
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Failed to fetch department stats:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Department Analytics</Text>
        <Text style={styles.headerSubtitle}>Drill down by building or college</Text>
      </View>

      <View style={styles.tabBar}>
        <TouchableOpacity style={styles.tab} onPress={() => router.navigate('/admin')}>
          <Text style={styles.tabText}>Overview</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.tabActive}>
          <Text style={styles.tabTextActive}>Departments</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.filterSection}>
        <Text style={styles.filterLabel}>Select Department:</Text>
        <View style={styles.pillContainer}>
          {departments.map(dept => (
            <TouchableOpacity 
              key={dept} 
              style={[styles.pill, department === dept && styles.pillActive]}
              onPress={() => setDepartment(dept)}
            >
              <Text style={[styles.pillText, department === dept && styles.pillTextActive]}>
                {dept}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#3b82f6" style={{ marginTop: 50 }} />
      ) : (
        <View style={styles.resultsArea}>
          {stats?.note === "Insufficient data for privacy" ? (
            <View style={styles.privacyAlert}>
              <ShieldAlert size={32} color="#f59e0b" style={{ marginBottom: 15 }} />
              <Text style={styles.privacyTitle}>Privacy Guard Active (K-Anonymity)</Text>
              <Text style={styles.privacyText}>
                Data for {stats.department} is masked because the population size ({stats.totalUsers}) 
                is below the privacy threshold. Exact case counts cannot be displayed to prevent 
                individual identification.
              </Text>
            </View>
          ) : (
            <View style={styles.dataGrid}>
              <View style={styles.statBox}>
                <Text style={styles.statLabel}>Total Tracked</Text>
                <Text style={styles.statValue}>{stats?.totalUsers || 0}</Text>
              </View>
              <View style={styles.statBox}>
                <Text style={styles.statLabel}>Active Cases</Text>
                <Text style={[styles.statValue, { color: '#ef4444' }]}>{stats?.confirmedCount || 0}</Text>
              </View>
              <View style={styles.statBox}>
                <Text style={styles.statLabel}>Suspects</Text>
                <Text style={[styles.statValue, { color: '#f59e0b' }]}>{stats?.suspectCount || 0}</Text>
              </View>
              <View style={styles.statBox}>
                <Text style={styles.statLabel}>Healthy</Text>
                <Text style={[styles.statValue, { color: '#22c55e' }]}>{stats?.activeCount || 0}</Text>
              </View>
            </View>
          )}
        </View>
      )}
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
  filterSection: {
    marginBottom: 30,
  },
  filterLabel: {
    color: '#fff',
    fontSize: 16,
    marginBottom: 10,
  },
  pillContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  pill: {
    backgroundColor: '#18181b',
    borderWidth: 1,
    borderColor: '#3f3f46',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
  },
  pillActive: {
    backgroundColor: 'rgba(59, 130, 246, 0.2)',
    borderColor: '#3b82f6',
  },
  pillText: {
    color: '#a1a1aa',
  },
  pillTextActive: {
    color: '#3b82f6',
    fontWeight: 'bold',
  },
  resultsArea: {
    marginTop: 10,
  },
  privacyAlert: {
    backgroundColor: 'rgba(245, 158, 11, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(245, 158, 11, 0.3)',
    borderRadius: 12,
    padding: 30,
    alignItems: 'center',
  },
  privacyTitle: {
    color: '#f59e0b',
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  privacyText: {
    color: '#d4d4d8',
    textAlign: 'center',
    lineHeight: 24,
  },
  dataGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 20,
  },
  statBox: {
    backgroundColor: '#18181b',
    padding: 20,
    borderRadius: 12,
    flex: 1,
    minWidth: 200,
    alignItems: 'center',
  },
  statLabel: {
    color: '#a1a1aa',
    fontSize: 14,
    marginBottom: 10,
  },
  statValue: {
    color: '#fff',
    fontSize: 48,
    fontWeight: 'bold',
  },
});

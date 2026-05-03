import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { QrCode, Thermometer, Users, Smartphone, Shield, LogOut, Activity } from 'lucide-react-native';
import { useAuth } from '@/context/AuthContext';
import { useQrToken } from '@/hooks/useQrToken';
import { Link, useRouter } from 'expo-router';
import QRCode from 'react-native-qrcode-svg';

/**
 * Main Campus Entry Screen.
 * Implements Story 2.2: Secure Rotating QR Token display.
 */
export default function HomeScreen() {
  const { anonymousId, token: authToken, logout } = useAuth();
  const { token, timeLeft } = useQrToken(anonymousId, authToken);
  const router = useRouter();

  return (
    <View style={styles.container}>
      <MeshBackground confirmedCount={0} unconfirmedCount={0} />
      
      <View style={styles.header}>
        <Text style={styles.statusLabel}>STATUS</Text>
        <Text style={styles.statusValue}>ACTIVE</Text>
      </View>

      <View style={styles.qrContainer}>
        <View style={styles.qrInner}>
          {token ? (
            <QRCode
              value={token}
              size={200}
              backgroundColor="transparent"
              color="#f4f4f5"
            />
          ) : (
            <QrCode size={200} color="#f4f4f5" strokeWidth={1.5} />
          )}
        </View>
        <Text style={styles.expiresText}>
          EXPIRES IN {Math.floor(timeLeft / 60)}:{String(timeLeft % 60).padStart(2, '0')}
        </Text>
      </View>

      <View style={styles.actionGrid}>
        <Link href="/questionnaire" asChild>
          <TouchableOpacity style={styles.actionButton}>
            <Activity size={20} color="#0891B2" />
            <Text style={styles.actionButtonText}>REPORT</Text>
          </TouchableOpacity>
        </Link>
        <Link href="/visitor" asChild>
          <TouchableOpacity style={StyleSheet.flatten([styles.actionButton, styles.visitorButton])}>
            <Users size={20} color="#0891B2" />
            <Text style={styles.actionButtonText}>VISITOR</Text>
          </TouchableOpacity>
        </Link>
        <TouchableOpacity 
          onPress={async () => {
            console.log('Emergency logout triggered');
            await logout();
            router.replace('/login');
          }}
          style={StyleSheet.flatten([styles.actionButton, { backgroundColor: 'rgba(239, 68, 68, 0.2)', borderColor: '#ef4444' }])}
        >
          <LogOut size={20} color="#ef4444" />
          <Text style={StyleSheet.flatten([styles.actionButtonText, { color: '#ef4444' }])}>LOGOUT</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.footer}>
        <View style={styles.statBox}>
          <Text style={styles.statLabel}>FENCES</Text>
          <Text style={styles.statValue}>0 CLEAN</Text>
        </View>
        <View style={styles.statBox}>
          <Text style={styles.statLabel}>THE MESH</Text>
          <View style={styles.meshStatusContainer}>
            <View style={styles.pulse} />
            <Text style={styles.meshStatusText}>ACTIVE</Text>
          </View>
        </View>
      </View>

      <View style={styles.adminRow}>
        <Link href="/admin" asChild>
          <TouchableOpacity style={styles.adminButton}>
            <Shield size={14} color="#71717a" style={{ marginRight: 6 }} />
            <Text style={styles.adminButtonText}>HEALTH ADMIN</Text>
          </TouchableOpacity>
        </Link>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#09090b',
    padding: 24,
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 60,
  },
  header: {
    alignItems: 'center',
  },
  statusLabel: {
    color: '#52525b',
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 2,
    marginBottom: 4,
  },
  statusValue: {
    color: '#22c55e',
    fontSize: 48,
    fontWeight: '800',
    letterSpacing: -1,
  },
  qrContainer: {
    alignItems: 'center',
  },
  qrInner: {
    padding: 24,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: 32,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  expiresText: {
    marginTop: 24,
    color: '#0891B2',
    fontWeight: '700',
    fontSize: 14,
    letterSpacing: 1,
  },
  actionGrid: {
    flexDirection: 'row',
    gap: 12,
    width: '100%',
    justifyContent: 'center',
    marginBottom: 20,
  },
  actionButton: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    borderRadius: 20,
    borderWidth: 1,
    gap: 8,
  },
  actionButtonText: {
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1,
  },
  reportButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    borderColor: 'rgba(239, 68, 68, 0.2)',
  },
  visitorButton: {
    backgroundColor: 'rgba(8, 145, 178, 0.1)',
    borderColor: 'rgba(8, 145, 178, 0.2)',
  },
  enrollButton: {
    backgroundColor: 'rgba(139, 92, 246, 0.1)',
    borderColor: 'rgba(139, 92, 246, 0.2)',
  },
  footer: {
    flexDirection: 'row',
    gap: 16,
    width: '100%',
  },
  statBox: {
    flex: 1,
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    padding: 16,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  statLabel: {
    color: '#52525b',
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 4,
  },
  statValue: {
    color: '#f4f4f5',
    fontSize: 16,
    fontWeight: '700',
  },
  meshStatusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  pulse: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#0891B2',
    shadowColor: '#0891B2',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 4,
  },
  meshStatusText: {
    color: '#0891B2',
    fontSize: 14,
    fontWeight: '800',
    letterSpacing: 1,
  },
  adminRow: {
    width: '100%',
    alignItems: 'center',
    marginTop: 20,
  },
  adminButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#27272a',
  },
  adminButtonText: {
    color: '#71717a',
    fontSize: 11,
    fontWeight: 'bold',
    letterSpacing: 1,
  },
});

import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { MeshBackground } from '@/components/MeshBackground';
import { Shield, Smartphone, QrCode } from 'lucide-react-native';
import { useAuth } from '@/context/AuthContext';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { useRouter } from 'expo-router';
import { useState } from 'react';

/**
 * Frictionless Enrollment Landing Page.
 * Implements Story 1.1 & 2.1: Campus Identity Handshake.
 * Implements Story 8.3: Visitor Handoff Scanning.
 */
export default function EnrollmentScreen() {
  const router = useRouter();
  const { enroll } = useAuth();
  const [permission, requestPermission] = useCameraPermissions();
  const [isScanning, setIsScanning] = useState(false);

  const handleEnroll = () => {
    // Navigate to login — enrollment requires authentication
    router.push('/login');
  };

  const handleScanPress = async () => {
    if (!permission?.granted) {
      const { granted } = await requestPermission();
      if (!granted) return;
    }
    setIsScanning(true);
  };

  const handleBarCodeScanned = async ({ data }: { data: string }) => {
    if (data.startsWith('HANDOFF_TOKEN:')) {
      setIsScanning(false);
      try {
        const parts = data.split(':');
        if (parts.length === 3) {
          const anonymousId = parts[1];
          const token = parts[2];
          await enroll(anonymousId, token);
        } else {
          Alert.alert("Invalid QR Code", "This QR code is not a valid handoff token.");
        }
      } catch (e) {
        Alert.alert("Error", "Failed to process handoff token.");
      }
    }
  };

  if (isScanning) {
    return (
      <View style={styles.container}>
        <CameraView 
          style={StyleSheet.absoluteFillObject}
          facing="back"
          onBarcodeScanned={handleBarCodeScanned}
          barcodeScannerSettings={{
            barcodeTypes: ["qr"],
          }}
        />
        <View style={styles.scanOverlay}>
          <Text style={styles.scanText}>Scan Visitor QR Code</Text>
          <TouchableOpacity 
            style={styles.cancelButton}
            onPress={() => setIsScanning(false)}
          >
            <Text style={styles.cancelButtonText}>Cancel</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <MeshBackground confirmedCount={0} unconfirmedCount={0} />
      
      <View style={styles.content}>
        <View style={styles.iconContainer}>
          <Shield size={48} color="#0891B2" />
        </View>

        <Text style={styles.title}>Secure Your Campus Identity</Text>
        <Text style={styles.subtitle}>
          CircleGuard uses anonymous vaults to protect your data. 
          One tap to begin your secure campus experience.
        </Text>

        <TouchableOpacity 
          style={styles.button} 
          activeOpacity={0.8}
          onPress={handleEnroll}
        >
          <Text style={styles.buttonText}>Start Frictionless Enroll</Text>
        </TouchableOpacity>

        <TouchableOpacity 
          style={styles.signInButton} 
          activeOpacity={0.8}
          onPress={() => router.push('/login')}
        >
          <Text style={styles.signInButtonText}>Already have an account? Sign In</Text>
        </TouchableOpacity>

        <TouchableOpacity 
          style={styles.secondaryButton} 
          activeOpacity={0.8}
          onPress={handleScanPress}
        >
          <QrCode size={20} color="#0891B2" />
          <Text style={styles.secondaryButtonText}>Scan Visitor Pass</Text>
        </TouchableOpacity>

        <View style={styles.footer}>
          <Smartphone size={16} color="#52525b" />
          <Text style={styles.footerText}>Hardware-Attested Security Active</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#09090b',
    padding: 24,
    justifyContent: 'center',
  },
  content: {
    alignItems: 'center',
  },
  iconContainer: {
    marginBottom: 32,
    padding: 24,
    backgroundColor: 'rgba(8, 145, 178, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(8, 145, 178, 0.2)',
    borderRadius: 40,
  },
  title: {
    color: '#f4f4f5',
    fontSize: 28,
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 16,
    fontFamily: 'Outfit', // Assuming global font config
  },
  subtitle: {
    color: '#a1a1aa',
    fontSize: 16,
    lineHeight: 24,
    textAlign: 'center',
    marginBottom: 48,
    paddingHorizontal: 12,
  },
  button: {
    width: '100%',
    backgroundColor: '#0891B2',
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    shadowColor: '#0891B2',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 8,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  footer: {
    marginTop: 48,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  footerText: {
    color: '#52525b',
    fontSize: 12,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  secondaryButton: {
    width: '100%',
    backgroundColor: 'transparent',
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 12,
    marginTop: 16,
    borderWidth: 1,
    borderColor: 'rgba(8, 145, 178, 0.3)',
  },
  secondaryButtonText: {
    color: '#0891B2',
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  signInButton: {
    width: '100%',
    alignItems: 'center',
    paddingVertical: 12,
    marginTop: 12,
  },
  signInButtonText: {
    color: '#71717a',
    fontSize: 14,
    fontWeight: '600',
    textDecorationLine: 'underline',
  },
  scanOverlay: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 32,
    backgroundColor: 'rgba(0,0,0,0.7)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  scanText: {
    color: '#ffffff',
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 24,
  },
  cancelButton: {
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 12,
    backgroundColor: 'rgba(255,255,255,0.2)',
  },
  cancelButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});

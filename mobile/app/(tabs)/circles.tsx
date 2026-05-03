import React, { useEffect, useState } from 'react';
import { 
  StyleSheet, 
  FlatList, 
  TouchableOpacity, 
  TextInput, 
  Modal, 
  ActivityIndicator,
  Alert,
  Clipboard,
  Animated
} from 'react-native';
import { Text, View } from '@/components/Themed';
import { useCircles, Circle } from '@/hooks/useCircles';
import { useAuth } from '@/context/AuthContext';
import { Users, Plus, UserPlus, Copy, Share2, Shield, X, MapPin, Search, Link as LinkIcon } from 'lucide-react-native';
import { BlurView } from 'expo-blur';
import { Platform } from 'react-native';
import axios from 'axios';
import { IDENTITY_BASE_URL } from '@/constants/Config';

export default function CirclesScreen() {
  const { anonymousId } = useAuth();
  const { circles, loading, error, fetchCircles, createCircle, joinCircle, addMember } = useCircles(anonymousId || '');
  
  const [isCreateModalVisible, setCreateModalVisible] = useState(false);
  const [isJoinModalVisible, setJoinModalVisible] = useState(false);
  const [isSearchModalVisible, setSearchModalVisible] = useState(false);
  const [newName, setNewName] = useState('');
  const [inviteCode, setInviteCode] = useState('');
  const [searchEmail, setSearchEmail] = useState('');
  const [foundUser, setFoundUser] = useState<string | null>(null);
  const [selectedCircleId, setSelectedCircleId] = useState<string | null>(null);

  const fadeAnim = useState(new Animated.Value(0))[0];

  useEffect(() => {
    if (anonymousId) {
      fetchCircles();
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 1000,
        useNativeDriver: true,
      }).start();
    }
  }, [anonymousId, fetchCircles]);

  const handleCreate = async () => {
    if (!newName.trim()) return;
    try {
      await createCircle(newName);
      setNewName('');
      setCreateModalVisible(false);
      Alert.alert('Success', 'Circle created successfully!');
    } catch (err: any) {
      Alert.alert('Error', err.message);
    }
  };

  const handleJoin = async () => {
    if (!inviteCode.trim()) return;
    try {
      await joinCircle(inviteCode.toUpperCase());
      setInviteCode('');
      setJoinModalVisible(false);
      Alert.alert('Success', 'Joined circle successfully!');
    } catch (err: any) {
      Alert.alert('Error', err.message);
    }
  };

  const copyToClipboard = (code: string) => {
    Clipboard.setString(code);
    if (Platform.OS !== 'web') {
      Alert.alert('Copied', 'Invite code copied to clipboard!');
    }
  };

  const copyJoinLink = (code: string) => {
    const baseUrl = Platform.OS === 'web' ? window.location.origin : 'circleguard://';
    const link = `${baseUrl}/join/${code}`;
    Clipboard.setString(link);
    if (Platform.OS !== 'web') {
      Alert.alert('Copied', 'Join link copied to clipboard!');
    }
  };

  const handleSearch = async () => {
    if (!searchEmail.trim()) return;
    try {
      const response = await axios.post(`${IDENTITY_BASE_URL}/api/v1/identities/map`, {
        realIdentity: searchEmail.toLowerCase()
      });
      setFoundUser(response.data.anonymousId);
    } catch (err: any) {
      Alert.alert('Not Found', 'User not found in the identity mesh.');
    }
  };

  const handleManualAdd = async () => {
    if (!foundUser || !selectedCircleId) return;
    try {
      await addMember(selectedCircleId, foundUser);
      setSearchModalVisible(false);
      setFoundUser(null);
      setSearchEmail('');
      Alert.alert('Success', 'User added to your safety circle.');
    } catch (err: any) {
      Alert.alert('Error', err.message);
    }
  };

  const renderCircleItem = ({ item }: { item: Circle }) => (
    <View style={styles.circleCard}>
      <BlurView intensity={20} style={styles.blurCard}>
        <View style={styles.cardHeader}>
          <View style={styles.iconContainer}>
            <Users color="#6366f1" size={24} />
          </View>
          <View style={styles.headerText}>
            <Text style={styles.circleName}>{item.name}</Text>
            <View style={styles.locationContainer}>
              <MapPin size={12} color="#94a3b8" />
              <Text style={styles.locationText}>{item.locationId || 'Global Mesh'}</Text>
            </View>
          </View>
          <View style={styles.statusBadge}>
            <Shield size={14} color="#10b981" />
            <Text style={styles.statusText}>SECURE</Text>
          </View>
        </View>

        <TouchableOpacity 
          style={styles.codeContainer} 
          onPress={() => copyToClipboard(item.inviteCode)}
        >
          <Text style={styles.codeLabel}>INVITE CODE</Text>
          <View style={styles.codeRow}>
            <Text style={styles.codeText}>{item.inviteCode}</Text>
            <Copy size={16} color="#6366f1" />
          </View>
        </TouchableOpacity>

        <View style={styles.cardFooter}>
          <TouchableOpacity style={styles.footerButton} onPress={() => copyJoinLink(item.inviteCode)}>
            <LinkIcon size={18} color="#94a3b8" />
            <Text style={styles.footerButtonText}>Copy Link</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.footerButton, { marginLeft: 16 }]} 
            onPress={() => {
              setSelectedCircleId(item.id);
              setSearchModalVisible(true);
            }}
          >
            <UserPlus size={18} color="#94a3b8" />
            <Text style={styles.footerButtonText}>Manual Add</Text>
          </TouchableOpacity>
        </View>
      </BlurView>
    </View>
  );

  return (
    <View style={styles.container}>
      <Animated.View style={[styles.content, { opacity: fadeAnim }]}>
        <View style={styles.header}>
          <Text style={styles.title}>Safety Circles</Text>
          <Text style={styles.subtitle}>Direct health propagation mesh</Text>
        </View>

        {loading && circles.length === 0 ? (
          <View style={styles.centerFixed}>
            <ActivityIndicator size="large" color="#6366f1" />
          </View>
        ) : circles.length === 0 ? (
          <View style={styles.emptyState}>
            <Users size={64} color="#334155" />
            <Text style={styles.emptyTitle}>No Circles Yet</Text>
            <Text style={styles.emptyText}>
              Create a safety circle for your family or team to receive instant status updates.
            </Text>
          </View>
        ) : (
          <FlatList
            data={circles}
            keyExtractor={(item) => item.id}
            renderItem={renderCircleItem}
            contentContainerStyle={styles.listContent}
            showsVerticalScrollIndicator={false}
            refreshing={loading}
            onRefresh={fetchCircles}
          />
        )}

        <View style={styles.fabContainer}>
          <TouchableOpacity 
            style={[styles.fab, styles.fabSecondary]} 
            onPress={() => setJoinModalVisible(true)}
          >
            <UserPlus color="#fff" size={24} />
          </TouchableOpacity>
          <TouchableOpacity 
            style={styles.fab} 
            onPress={() => setCreateModalVisible(true)}
          >
            <Plus color="#fff" size={32} />
          </TouchableOpacity>
        </View>
      </Animated.View>

      {/* Create Modal */}
      <Modal visible={isCreateModalVisible} transparent animationType="slide">
        <BlurView intensity={80} style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Create Circle</Text>
              <TouchableOpacity onPress={() => setCreateModalVisible(false)}>
                <X color="#94a3b8" size={24} />
              </TouchableOpacity>
            </View>
            <TextInput
              style={styles.input}
              placeholder="Circle Name (e.g. Family Alpha)"
              placeholderTextColor="#64748b"
              value={newName}
              onChangeText={setNewName}
              autoFocus
            />
            <TouchableOpacity style={styles.modalButton} onPress={handleCreate}>
              <Text style={styles.modalButtonText}>Establish Mesh</Text>
            </TouchableOpacity>
          </View>
        </BlurView>
      </Modal>

      {/* Join Modal */}
      <Modal visible={isJoinModalVisible} transparent animationType="slide">
        <BlurView intensity={80} style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Join Circle</Text>
              <TouchableOpacity onPress={() => setJoinModalVisible(false)}>
                <X color="#94a3b8" size={24} />
              </TouchableOpacity>
            </View>
            <TextInput
              style={styles.input}
              placeholder="Invite Code (MESH-XXXX)"
              placeholderTextColor="#64748b"
              value={inviteCode}
              onChangeText={setInviteCode}
              autoCapitalize="characters"
              autoFocus
            />
            <TouchableOpacity 
              style={[styles.modalButton, { backgroundColor: '#10b981' }]} 
              onPress={handleJoin}
            >
              <Text style={styles.modalButtonText}>Connect to Mesh</Text>
            </TouchableOpacity>
          </View>
        </BlurView>
      </Modal>

      {/* Manual Search Modal */}
      <Modal visible={isSearchModalVisible} transparent animationType="slide">
        <BlurView intensity={80} style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Manual Member Add</Text>
              <TouchableOpacity onPress={() => {
                setSearchModalVisible(false);
                setFoundUser(null);
              }}>
                <X color="#94a3b8" size={24} />
              </TouchableOpacity>
            </View>

            <View style={styles.searchContainer}>
              <TextInput
                style={[styles.input, { flex: 1, marginBottom: 0 }]}
                placeholder="Search by University Email"
                placeholderTextColor="#64748b"
                value={searchEmail}
                onChangeText={setSearchEmail}
                keyboardType="email-address"
                autoCapitalize="none"
              />
              <TouchableOpacity style={styles.searchButton} onPress={handleSearch}>
                <Search color="#fff" size={24} />
              </TouchableOpacity>
            </View>

            {foundUser && (
              <Animated.View style={styles.foundUserBox}>
                <View style={styles.hashAvatar}>
                  <Text style={styles.hashText}>{foundUser.substring(0, 4)}</Text>
                </View>
                <View style={styles.foundUserInfo}>
                  <Text style={styles.foundUserTitle}>Identity Verified</Text>
                  <Text style={styles.foundUserSubtitle}>Anonymous ID: {foundUser.substring(0, 8)}...</Text>
                </View>
                <TouchableOpacity style={styles.addButton} onPress={handleManualAdd}>
                  <Plus color="#fff" size={20} />
                </TouchableOpacity>
              </Animated.View>
            )}

            {!foundUser && (
              <Text style={styles.searchHint}>
                Enter a university email to find their anonymous mesh identity and add them to your circle.
              </Text>
            )}
          </View>
        </BlurView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  content: {
    flex: 1,
    paddingTop: 60,
  },
  header: {
    paddingHorizontal: 20,
    marginBottom: 24,
  },
  title: {
    fontSize: 34,
    fontWeight: '900',
    color: '#f8fafc',
    letterSpacing: -1,
  },
  subtitle: {
    fontSize: 16,
    color: '#64748b',
    marginTop: 4,
  },
  listContent: {
    paddingHorizontal: 20,
    paddingBottom: 100,
  },
  circleCard: {
    marginBottom: 16,
    borderRadius: 24,
    overflow: 'hidden',
    backgroundColor: 'rgba(30, 41, 59, 0.5)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  blurCard: {
    padding: 20,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 16,
    backgroundColor: 'rgba(99, 102, 241, 0.1)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  headerText: {
    flex: 1,
  },
  circleName: {
    fontSize: 18,
    fontWeight: '700',
    color: '#f8fafc',
  },
  locationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 2,
  },
  locationText: {
    fontSize: 12,
    color: '#94a3b8',
    marginLeft: 4,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  statusText: {
    fontSize: 10,
    fontWeight: '800',
    color: '#10b981',
    marginLeft: 4,
  },
  codeContainer: {
    backgroundColor: 'rgba(15, 23, 42, 0.6)',
    borderRadius: 16,
    padding: 12,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(99, 102, 241, 0.3)',
  },
  codeLabel: {
    fontSize: 10,
    fontWeight: '700',
    color: '#64748b',
    marginBottom: 4,
    letterSpacing: 2,
  },
  codeRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  codeText: {
    fontSize: 24,
    fontWeight: '900',
    color: '#6366f1',
    marginRight: 10,
    letterSpacing: 4,
  },
  cardFooter: {
    marginTop: 16,
    flexDirection: 'row',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.05)',
    paddingTop: 16,
  },
  footerButton: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  footerButtonText: {
    fontSize: 14,
    color: '#94a3b8',
    marginLeft: 8,
  },
  fabContainer: {
    position: 'absolute',
    bottom: 30,
    right: 20,
    flexDirection: 'column',
    alignItems: 'center',
  },
  fab: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#6366f1',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#6366f1',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.4,
    shadowRadius: 16,
    elevation: 8,
  },
  fabSecondary: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: '#334155',
    marginBottom: 16,
    shadowColor: '#000',
    shadowOpacity: 0.2,
  },
  centerFixed: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 40,
  },
  emptyTitle: {
    fontSize: 24,
    fontWeight: '700',
    color: '#f8fafc',
    marginTop: 20,
  },
  emptyText: {
    fontSize: 16,
    color: '#64748b',
    textAlign: 'center',
    marginTop: 12,
    lineHeight: 24,
  },
  modalOverlay: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#1e293b',
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    padding: 30,
    paddingBottom: 50,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  modalTitle: {
    fontSize: 24,
    fontWeight: '800',
    color: '#f8fafc',
  },
  input: {
    backgroundColor: '#0f172a',
    borderRadius: 16,
    padding: 16,
    color: '#f8fafc',
    fontSize: 16,
    marginBottom: 20,
    borderWidth: 1,
    borderColor: '#334155',
  },
  modalButton: {
    backgroundColor: '#6366f1',
    borderRadius: 16,
    padding: 18,
    alignItems: 'center',
  },
  modalButtonText: {
    fontSize: 18,
    fontWeight: '700',
    color: '#fff',
  },
  searchContainer: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 20,
  },
  searchButton: {
    backgroundColor: '#6366f1',
    borderRadius: 16,
    width: 56,
    alignItems: 'center',
    justifyContent: 'center',
  },
  foundUserBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(99, 102, 241, 0.1)',
    borderRadius: 20,
    padding: 16,
    borderWidth: 1,
    borderColor: 'rgba(99, 102, 241, 0.2)',
  },
  hashAvatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#6366f1',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  hashText: {
    color: '#fff',
    fontWeight: '900',
    fontSize: 12,
  },
  foundUserInfo: {
    flex: 1,
  },
  foundUserTitle: {
    color: '#f8fafc',
    fontSize: 16,
    fontWeight: '700',
  },
  foundUserSubtitle: {
    color: '#94a3b8',
    fontSize: 12,
  },
  addButton: {
    backgroundColor: '#10b981',
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchHint: {
    color: '#64748b',
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
    marginTop: 10,
  },
});

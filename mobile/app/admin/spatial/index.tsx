import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, Modal, TextInput, Pressable, ActivityIndicator } from 'react-native';
import { useSpatial, Building } from '@/hooks/useSpatial';
import { Link } from 'expo-router';

export default function BuildingsScreen() {
  const { getBuildings, createBuilding, deleteBuilding, loading } = useSpatial();
  const [buildings, setBuildings] = useState<Building[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [newBuilding, setNewBuilding] = useState({ name: '', code: '', description: '' });

  const fetchBuildings = async () => {
    const data = await getBuildings();
    setBuildings(data);
  };

  useEffect(() => {
    fetchBuildings();
  }, []);

  const handleCreate = async () => {
    await createBuilding(newBuilding);
    setModalVisible(false);
    setNewBuilding({ name: '', code: '', description: '' });
    fetchBuildings();
  };

  const handleDelete = async (id: string) => {
    await deleteBuilding(id);
    fetchBuildings();
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>University Buildings</Text>
        <TouchableOpacity style={styles.addButton} onPress={() => setModalVisible(true)}>
          <Text style={styles.addButtonText}>+ Add Building</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#06b6d4" />
      ) : (
        <FlatList
          data={buildings}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View>
                <Text style={styles.cardTitle}>{item.name}</Text>
                <Text style={styles.cardSubtitle}>{item.code} | {item.description}</Text>
              </View>
              <View style={styles.actions}>
                <Link href={`/admin/spatial/${item.id}/floors/default`} asChild>
                  <TouchableOpacity style={styles.editButton}>
                    <Text style={styles.buttonText}>Floors</Text>
                  </TouchableOpacity>
                </Link>
                <TouchableOpacity onPress={() => handleDelete(item.id)} style={styles.deleteButton}>
                  <Text style={styles.buttonText}>Delete</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        />
      )}

      <Modal visible={modalVisible} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>New Building</Text>
            <TextInput
              style={styles.input}
              placeholder="Name"
              placeholderTextColor="#71717a"
              value={newBuilding.name}
              onChangeText={(t) => setNewBuilding({...newBuilding, name: t})}
            />
            <TextInput
              style={styles.input}
              placeholder="Code (e.g. BLD-01)"
              placeholderTextColor="#71717a"
              value={newBuilding.code}
              onChangeText={(t) => setNewBuilding({...newBuilding, code: t})}
            />
            <TextInput
              style={[styles.input, { height: 80 }]}
              placeholder="Description"
              placeholderTextColor="#71717a"
              multiline
              value={newBuilding.description}
              onChangeText={(t) => setNewBuilding({...newBuilding, description: t})}
            />
            <View style={styles.modalActions}>
              <Pressable style={styles.cancelButton} onPress={() => setModalVisible(false)}>
                <Text style={styles.buttonText}>Cancel</Text>
              </Pressable>
              <Pressable style={styles.saveButton} onPress={handleCreate}>
                <Text style={styles.buttonText}>Create</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000', padding: 20 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  title: { fontSize: 24, fontWeight: 'bold', color: '#fff', fontFamily: 'Outfit' },
  addButton: { backgroundColor: '#0891b2', paddingHorizontal: 15, paddingVertical: 10, borderRadius: 8 },
  addButtonText: { color: '#fff', fontWeight: 'bold' },
  card: { backgroundColor: '#18181b', padding: 20, borderRadius: 12, marginBottom: 15, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderContent: '#27272a', borderWidth: 1 },
  cardTitle: { fontSize: 18, fontWeight: '600', color: '#fff' },
  cardSubtitle: { color: '#71717a', fontSize: 14, marginTop: 4 },
  actions: { flexDirection: 'row', gap: 10 },
  editButton: { backgroundColor: '#27272a', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 6 },
  deleteButton: { backgroundColor: '#7f1d1d', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 6 },
  buttonText: { color: '#fff', fontSize: 12, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.8)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { backgroundColor: '#18181b', width: '90%', maxWidth: 400, padding: 25, borderRadius: 16, borderContent: '#27272a', borderWidth: 1 },
  modalTitle: { fontSize: 20, fontWeight: 'bold', color: '#fff', marginBottom: 20 },
  input: { backgroundColor: '#09090b', color: '#fff', padding: 12, borderRadius: 8, marginBottom: 15, borderContent: '#27272a', borderWidth: 1 },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10 },
  cancelButton: { padding: 10 },
  saveButton: { backgroundColor: '#0891b2', paddingHorizontal: 20, paddingVertical: 10, borderRadius: 8 },
});

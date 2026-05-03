import React, { useEffect, useState, useRef } from 'react';
import { View, Text, StyleSheet, Image, Pressable, ScrollView, TextInput, TouchableOpacity, ActivityIndicator, Alert, PanResponder, Dimensions, LayoutChangeEvent, Platform } from 'react-native';
import { useLocalSearchParams } from 'expo-router';
import { useSpatial, AccessPoint, Floor } from '@/hooks/useSpatial';
import { Upload, Map as MapIcon, Save, Trash2, X as CloseIcon, Info } from 'lucide-react-native';

export default function MapEditorScreen() {
  const { buildingId, floorId: initialFloorId } = useLocalSearchParams<{ buildingId: string, floorId: string }>();
  const { getFloors, getAccessPoints, saveAccessPoint, deleteAccessPoint, updateFloor, uploadFloorPlan } = useSpatial();

  const [floors, setFloors] = useState<Floor[]>([]);
  const [selectedFloorId, setSelectedFloorId] = useState<string>(initialFloorId === 'default' ? '' : initialFloorId);
  const [selectedFloor, setSelectedFloor] = useState<Floor | null>(null);
  const [aps, setAps] = useState<AccessPoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedAp, setSelectedAp] = useState<AccessPoint | null>(null);
  
  const [editAp, setEditAp] = useState<AccessPoint | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  // Map container dimensions for normalization
  const [containerLayout, setContainerLayout] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const fetchFloors = async () => {
      const data = await getFloors(buildingId);
      setFloors(data);
      if (data.length > 0 && !selectedFloorId) {
        setSelectedFloorId(data[0].id);
      } else if (selectedFloorId) {
        setSelectedFloor(data.find(f => f.id === selectedFloorId) || null);
      }
    };
    fetchFloors();
  }, [buildingId]);

  useEffect(() => {
    if (selectedFloorId) {
      const f = floors.find(f => f.id === selectedFloorId);
      if (f) setSelectedFloor(f);
      fetchAps();
    }
  }, [selectedFloorId, floors]);

  const fetchAps = async () => {
    setLoading(true);
    try {
        const data = await getAccessPoints(selectedFloorId);
        setAps(data);
    } catch (e) {
        console.error("Failed to fetch APs", e);
    } finally {
        setLoading(false);
    }
  };

  const handleLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setContainerLayout({ width, height });
  };

  const handleMapClick = (event: any) => {
    if (!containerLayout.width || !containerLayout.height) return;
    
    // For Web/Native, get locationX/Y relative to the target
    const { locationX, locationY } = event.nativeEvent;
    
    // Normalize coordinates (0.0 to 1.0)
    const normX = locationX / containerLayout.width;
    const normY = locationY / containerLayout.height;
    
    const newAp: AccessPoint = {
      macAddress: '00:00:00:00:00:00',
      name: `AP-${aps.length + 1}`,
      coordinateX: normX,
      coordinateY: normY,
      floorId: selectedFloorId
    };
    
    setEditAp(newAp);
    setSelectedAp(null);
  };

  const handleSaveAp = async () => {
    const apToSave = editAp || selectedAp;
    if (!apToSave) return;
    
    try {
      await saveAccessPoint(selectedFloorId, apToSave);
      setEditAp(null);
      setSelectedAp(null);
      fetchAps();
      Alert.alert("Success", "Access Point saved successfully.");
    } catch (e) {
      Alert.alert("Error", "Failed to save Access Point.");
    }
  };

  const handleDeleteAp = async (id: string) => {
    try {
      await deleteAccessPoint(id);
      setSelectedAp(null);
      setEditAp(null);
      fetchAps();
    } catch (e) {
      Alert.alert("Error", "Failed to delete Access Point.");
    }
  };

  const handleFileSelect = (event: any) => {
    const file = event.target.files[0];
    if (file) {
      performUpload(file);
    }
  };

  const performUpload = async (file: File) => {
    setIsUploading(true);
    try {
      const updatedFloor = await uploadFloorPlan(selectedFloorId, file);
      setSelectedFloor(updatedFloor);
      setFloors(prev => prev.map(f => f.id === selectedFloorId ? updatedFloor : f));
      Alert.alert("Success", "Floor plan uploaded successfully.");
    } catch (e) {
      Alert.alert("Error", "Failed to upload floor plan.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleDrag = (apId: string, normX: number, normY: number) => {
    // Keep within bounds
    const clampedX = Math.max(0, Math.min(1, normX));
    const clampedY = Math.max(0, Math.min(1, normY));

    if (selectedAp?.id === apId) {
      setSelectedAp({ ...selectedAp, coordinateX: clampedX, coordinateY: clampedY });
    }
    
    setAps(prev => prev.map(ap => 
      ap.id === apId ? { ...ap, coordinateX: clampedX, coordinateY: clampedY } : ap
    ));
  };

  const finalizeDrag = async (ap: AccessPoint) => {
    await saveAccessPoint(selectedFloorId, ap);
  };

  return (
    <View style={styles.container}>
      <View style={styles.sidebar}>
        <Text style={styles.sidebarTitle}>Building & Floor</Text>
        <ScrollView style={styles.floorList}>
          {floors.map(f => (
            <TouchableOpacity 
              key={f.id} 
              onPress={() => setSelectedFloorId(f.id)}
              style={[styles.floorItem, selectedFloorId === f.id && styles.floorItemSelected]}
            >
              <Text style={[styles.floorText, selectedFloorId === f.id && styles.floorTextSelected]}>
                Floor {f.floorNumber} - {f.name}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <View style={styles.divider} />

        {selectedFloor && (
          <View style={styles.uploadSection}>
            <Text style={styles.sidebarTitle}>Floor Plan</Text>
            {Platform.OS === 'web' ? (
              <View>
                <input 
                  type="file" 
                  id="floorPlanUpload" 
                  style={{ display: 'none' }} 
                  accept="image/png,image/svg+xml"
                  onChange={handleFileSelect}
                />
                <TouchableOpacity 
                  style={styles.uploadButton} 
                  onPress={() => document.getElementById('floorPlanUpload')?.click()}
                  disabled={isUploading}
                >
                  {isUploading ? <ActivityIndicator size="small" color="#fff" /> : (
                    <>
                      <Upload size={16} color="#fff" />
                      <Text style={styles.uploadButtonText}>Upload Image (SVG/PNG)</Text>
                    </>
                  )}
                </TouchableOpacity>
              </View>
            ) : (
                <View style={styles.mobileWarning}>
                    <Info size={16} color="#71717a" />
                    <Text style={styles.mobileWarningText}>Floor plan upload is only available on web.</Text>
                </View>
            )}
          </View>
        )}

        <View style={styles.divider} />

        {editAp || selectedAp ? (
          <View style={styles.editSection}>
            <View style={styles.headerRow}>
              <Text style={styles.sidebarTitle}>{editAp ? 'New AP' : 'Edit AP'}</Text>
              <Pressable onPress={() => { setEditAp(null); setSelectedAp(null); }}>
                <CloseIcon size={16} color="#71717a" />
              </Pressable>
            </View>
            <View style={styles.formItem}>
              <Text style={styles.label}>AP Name</Text>
              <TextInput 
                style={styles.input} 
                value={editAp?.name || selectedAp?.name} 
                onChangeText={(t) => editAp ? setEditAp({...editAp, name: t}) : setSelectedAp({...selectedAp!, name: t})}
              />
            </View>
            <View style={styles.formItem}>
              <Text style={styles.label}>MAC Address</Text>
              <TextInput 
                style={styles.input} 
                value={editAp?.macAddress || selectedAp?.macAddress} 
                onChangeText={(t) => editAp ? setEditAp({...editAp, macAddress: t}) : setSelectedAp({...selectedAp!, macAddress: t})}
              />
            </View>
            <View style={styles.coords}>
              <Text style={styles.coordLabel}>X: {((editAp?.coordinateX || selectedAp?.coordinateX || 0) * 100).toFixed(1)}%</Text>
              <Text style={styles.coordLabel}>Y: {((editAp?.coordinateY || selectedAp?.coordinateY || 0) * 100).toFixed(1)}%</Text>
            </View>
            <TouchableOpacity style={styles.saveButton} onPress={handleSaveAp}>
              <Save size={16} color="#fff" style={{ marginRight: 8 }} />
              <Text style={styles.saveButtonText}>Save AP</Text>
            </TouchableOpacity>
            {(editAp?.id || selectedAp?.id) && (
              <TouchableOpacity style={styles.deleteButton} onPress={() => handleDeleteAp(editAp?.id || selectedAp!.id!)}>
                <Trash2 size={16} color="#fff" style={{ marginRight: 8 }} />
                <Text style={styles.deleteButtonText}>Delete</Text>
              </TouchableOpacity>
            )}
          </View>
        ) : (
          <View style={styles.hintSection}>
            <MapIcon size={24} color="#0891b2" style={{ marginBottom: 10 }} />
            <Text style={styles.hintText}>Click on the map to place a new Access Point.</Text>
            <Text style={styles.hintText}>Drag existing points to move them.</Text>
          </View>
        )}
      </View>

      <View style={styles.mapArea}>
        {loading ? (
          <ActivityIndicator size="large" color="#06b6d4" />
        ) : (
          <View 
            style={styles.mapContainer} 
            onLayout={handleLayout}
          >
             {selectedFloor?.floorPlanUrl ? (
               <Pressable onPress={handleMapClick} style={styles.floorPlanWrapper}>
                <Image 
                  source={{ uri: selectedFloor.floorPlanUrl }}
                  style={styles.floorPlan}
                  resizeMode="contain"
                />
               </Pressable>
             ) : (
               <View style={styles.noPlan}>
                  <Upload size={48} color="#27272a" />
                  <Text style={styles.noPlanText}>No floor plan uploaded</Text>
               </View>
             )}
            
            {aps.map(ap => (
              <DraggableAP 
                key={ap.id || 'new'} 
                ap={ap} 
                isSelected={selectedAp?.id === ap.id}
                containerLayout={containerLayout}
                onSelect={() => { setSelectedAp(ap); setEditAp(null); }}
                onDrag={handleDrag}
                onDragEnd={finalizeDrag}
              />
            ))}
            
            {editAp && (
              <View 
                style={[
                  styles.apMarker, 
                  styles.apMarkerNew,
                  { 
                    top: editAp.coordinateY * containerLayout.height - 12, 
                    left: editAp.coordinateX * containerLayout.width - 12 
                  }
                ]}
              />
            )}
          </View>
        )}
      </View>
    </View>
  );
}

function DraggableAP({ ap, isSelected, containerLayout, onSelect, onDrag, onDragEnd }: { 
  ap: AccessPoint, 
  isSelected: boolean, 
  containerLayout: { width: number, height: number },
  onSelect: () => void, 
  onDrag: (id: string, x: number, y: number) => void, 
  onDragEnd: (ap: AccessPoint) => void 
}) {
  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onPanResponderMove: (_, gestureState) => {
        if (!containerLayout.width || !containerLayout.height) return;
        
        // Calculate new normalized position
        // The gestureState.dx/dy are deltas in pixels
        const deltaXNorm = gestureState.dx / containerLayout.width;
        const deltaYNorm = gestureState.dy / containerLayout.height;
        
        onDrag(ap.id!, ap.coordinateX + deltaXNorm, ap.coordinateY + deltaYNorm);
      },
      onPanResponderRelease: () => {
        onDragEnd(ap);
      },
    })
  ).current;

  const posX = ap.coordinateX * containerLayout.width;
  const posY = ap.coordinateY * containerLayout.height;

  return (
    <View 
      {...panResponder.panHandlers}
      style={[
        styles.apMarker, 
        { top: posY - 12, left: posX - 12 },
        isSelected && styles.apMarkerSelected
      ]}
    >
      <Pressable onPress={onSelect} style={{ width: '100%', height: '100%' }} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000', flexDirection: 'row' },
  sidebar: { width: 320, backgroundColor: '#111', borderRightWidth: 1, borderRightColor: '#27272a', padding: 20 },
  sidebarTitle: { color: '#71717a', fontSize: 11, fontWeight: 'bold', textTransform: 'uppercase', marginBottom: 12, letterSpacing: 1 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 15 },
  floorList: { maxHeight: 150, marginBottom: 10 },
  floorItem: { padding: 12, borderRadius: 8, marginBottom: 8, backgroundColor: '#18181b' },
  floorItemSelected: { backgroundColor: '#0891b2' },
  floorText: { color: '#a1a1aa', fontSize: 13 },
  floorTextSelected: { color: '#fff', fontWeight: '600' },
  divider: { height: 1, backgroundColor: '#27272a', marginVertical: 20 },
  uploadSection: { marginBottom: 10 },
  uploadButton: { backgroundColor: '#27272a', padding: 12, borderRadius: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  uploadButtonText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  mobileWarning: { flexDirection: 'row', alignItems: 'center', gap: 8, backgroundColor: '#18181b', padding: 10, borderRadius: 8 },
  mobileWarningText: { color: '#71717a', fontSize: 11, flex: 1 },
  editSection: { flex: 1 },
  formItem: { marginBottom: 15 },
  label: { color: '#a1a1aa', fontSize: 12, marginBottom: 6 },
  input: { backgroundColor: '#09090b', color: '#fff', padding: 12, borderRadius: 8, borderWidth: 1, borderColor: '#27272a', fontSize: 14 },
  coords: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20, backgroundColor: '#18181b', padding: 10, borderRadius: 8 },
  coordLabel: { color: '#06b6d4', fontFamily: 'monospace', fontSize: 12 },
  saveButton: { backgroundColor: '#0891b2', padding: 14, borderRadius: 8, alignItems: 'center', flexDirection: 'row', justifyContent: 'center' },
  saveButtonText: { color: '#fff', fontWeight: 'bold', fontSize: 14 },
  deleteButton: { backgroundColor: '#7f1d1d', padding: 12, borderRadius: 8, alignItems: 'center', marginTop: 10, flexDirection: 'row', justifyContent: 'center' },
  deleteButtonText: { color: '#fff', fontSize: 13 },
  hintSection: { padding: 20, backgroundColor: '#18181b', borderRadius: 12, borderStyle: 'dashed', borderWidth: 1, borderColor: '#27272a', alignItems: 'center' },
  hintText: { color: '#71717a', fontSize: 12, marginBottom: 8, lineHeight: 18, textAlign: 'center' },
  mapArea: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: '#09090b', padding: 40 },
  mapContainer: { width: '100%', height: '100%', maxWidth: 1000, maxHeight: 800, backgroundColor: '#000', borderRadius: 12, overflow: 'hidden', position: 'relative', borderWidth: 1, borderColor: '#27272a', shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.5, shadowRadius: 20 },
  floorPlanWrapper: { width: '100%', height: '100%' },
  floorPlan: { width: '100%', height: '100%', opacity: 0.8 },
  noPlan: { flex: 1, justifyContent: 'center', alignItems: 'center', gap: 15 },
  noPlanText: { color: '#52525b', fontSize: 16, fontWeight: '500' },
  apMarker: { position: 'absolute', width: 24, height: 24, backgroundColor: '#0891b2', borderRadius: 12, borderWidth: 2, borderColor: '#fff', shadowColor: '#06b6d4', shadowRadius: 10, shadowOpacity: 0.8, zIndex: 10 },
  apMarkerSelected: { backgroundColor: '#fff', borderColor: '#0891b2', transform: [{ scale: 1.2 }], zIndex: 20 },
  apMarkerNew: { backgroundColor: '#059669', borderColor: '#fff', borderStyle: 'dashed' },
});

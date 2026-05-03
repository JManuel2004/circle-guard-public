import { useState, useCallback } from 'react';
import { PROMOTION_BASE_URL, FILE_BASE_URL } from '../constants/Config';
const API_BASE = `${PROMOTION_BASE_URL}/api/v1`;
const FILE_API_BASE = `${FILE_BASE_URL}/api/v1/files`;

export interface Building {
    id: string;
    name: string;
    code: string;
    description: string;
    latitude?: number;
    longitude?: number;
    address?: string;
}

export interface Floor {
    id: string;
    buildingId: string;
    floorNumber: number;
    name: string;
    floorPlanUrl?: string;
}

export interface AccessPoint {
    id?: string;
    macAddress: string;
    coordinateX: number;
    coordinateY: number;
    name: string;
    floorId: string;
}

export const useSpatial = () => {
    const [loading, setLoading] = useState(false);

    const getBuildings = useCallback(async (): Promise<Building[]> => {
        setLoading(true);
        try {
            const resp = await fetch(`${API_BASE}/buildings`);
            return await resp.json();
        } finally {
            setLoading(false);
        }
    }, []);

    const createBuilding = async (data: Omit<Building, 'id'>) => {
        const resp = await fetch(`${API_BASE}/buildings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await resp.json();
    };

    const updateBuilding = async (id: string, data: Omit<Building, 'id'>) => {
        await fetch(`${API_BASE}/buildings/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    };

    const deleteBuilding = async (id: string) => {
        await fetch(`${API_BASE}/buildings/${id}`, { method: 'DELETE' });
    };

    const getFloors = useCallback(async (buildingId: string): Promise<Floor[]> => {
        const resp = await fetch(`${API_BASE}/buildings/${buildingId}/floors`);
        return await resp.json();
    }, []);

    const createFloor = async (buildingId: string, data: { floorNumber: number, name: string }) => {
        const resp = await fetch(`${API_BASE}/buildings/${buildingId}/floors`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await resp.json();
    };

    const updateFloor = async (id: string, data: { floorNumber?: number, name?: string, floorPlanUrl?: string }) => {
        const resp = await fetch(`${API_BASE}/floors/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await resp.json();
    };

    const uploadFloorPlan = async (floorId: string, file: any) => {
        const formData = new FormData();
        formData.append('file', file);
        
        const resp = await fetch(`${FILE_API_BASE}/upload`, {
            method: 'POST',
            body: formData
        });
        const { filename } = await resp.json();
        const url = `${FILE_API_BASE}/download/${filename}`;
        
        return await updateFloor(floorId, { floorPlanUrl: url });
    };

    const getAccessPoints = useCallback(async (floorId: string): Promise<AccessPoint[]> => {
        const resp = await fetch(`${API_BASE}/floors/${floorId}/access-points`);
        return await resp.json();
    }, []);

    const saveAccessPoint = async (floorId: string, ap: AccessPoint) => {
        const method = ap.id ? 'PUT' : 'POST';
        const url = ap.id 
            ? `${API_BASE}/access-points/${ap.id}` 
            : `${API_BASE}/floors/${floorId}/access-points`;
        
        const resp = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ap)
        });
        return await resp.json();
    };

    const deleteAccessPoint = async (id: string) => {
        await fetch(`${API_BASE}/access-points/${id}`, { method: 'DELETE' });
    };

    return {
        loading,
        getBuildings,
        createBuilding,
        updateBuilding,
        deleteBuilding,
        getFloors,
        createFloor,
        updateFloor,
        uploadFloorPlan,
        getAccessPoints,
        saveAccessPoint,
        deleteAccessPoint
    };
};

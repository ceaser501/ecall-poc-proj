import { projectId, publicAnonKey } from './supabase/info';

const API_BASE = `https://${projectId}.supabase.co/functions/v1/make-server-dd6e4f12`;

interface IncidentData {
  phoneNumber: string;
  callerName: string;
  location: string;
  incidentType: string;
  severity: string;
  transcript: Array<{
    sender: string;
    message: string;
    timestamp: string;
  }>;
  operatorId?: string;
  status: 'active' | 'completed';
  createdAt?: string;
}

export async function saveIncident(incident: IncidentData) {
  try {
    const response = await fetch(`${API_BASE}/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${publicAnonKey}`
      },
      body: JSON.stringify(incident)
    });

    if (!response.ok) {
      throw new Error('Failed to save incident');
    }

    return await response.json();
  } catch (error) {
    console.error('Error saving incident:', error);
    throw error;
  }
}

export async function getActiveIncidents() {
  try {
    const response = await fetch(`${API_BASE}/incidents?status=active`, {
      headers: {
        'Authorization': `Bearer ${publicAnonKey}`
      }
    });

    if (!response.ok) {
      throw new Error('Failed to fetch incidents');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching incidents:', error);
    throw error;
  }
}

export async function updateTranscript(incidentId: string, message: any) {
  try {
    const response = await fetch(`${API_BASE}/incidents/${incidentId}/transcript`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${publicAnonKey}`
      },
      body: JSON.stringify(message)
    });

    if (!response.ok) {
      throw new Error('Failed to update transcript');
    }

    return await response.json();
  } catch (error) {
    console.error('Error updating transcript:', error);
    throw error;
  }
}

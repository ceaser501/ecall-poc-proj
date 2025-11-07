import { Hono } from 'npm:hono';
import { cors } from 'npm:hono/cors';
import { logger } from 'npm:hono/logger';
import * as kv from './kv_store.tsx';

const app = new Hono();

// Middleware
app.use('*', cors());
app.use('*', logger(console.log));

// Incidents endpoints
app.post('/make-server-dd6e4f12/incidents', async (c) => {
  try {
    const incident = await c.req.json();
    const incidentId = `incident_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    const incidentData = {
      ...incident,
      id: incidentId,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    await kv.set(incidentId, incidentData);
    
    // Also add to active incidents list
    const activeIncidents = await kv.get('active_incidents') || [];
    activeIncidents.push(incidentId);
    await kv.set('active_incidents', activeIncidents);

    console.log('Incident created:', incidentId);
    return c.json({ success: true, data: incidentData });
  } catch (error) {
    console.error('Error creating incident:', error);
    return c.json({ success: false, error: String(error) }, 500);
  }
});

app.get('/make-server-dd6e4f12/incidents', async (c) => {
  try {
    const status = c.req.query('status');
    const activeIncidents = await kv.get('active_incidents') || [];
    
    const incidents = await Promise.all(
      activeIncidents.map((id: string) => kv.get(id))
    );

    const filteredIncidents = incidents.filter(incident => 
      incident && (!status || incident.status === status)
    );

    return c.json({ success: true, data: filteredIncidents });
  } catch (error) {
    console.error('Error fetching incidents:', error);
    return c.json({ success: false, error: String(error) }, 500);
  }
});

app.post('/make-server-dd6e4f12/incidents/:id/transcript', async (c) => {
  try {
    const incidentId = c.req.param('id');
    const message = await c.req.json();
    
    const incident = await kv.get(incidentId);
    if (!incident) {
      return c.json({ success: false, error: 'Incident not found' }, 404);
    }

    incident.transcript.push(message);
    incident.updatedAt = new Date().toISOString();
    
    await kv.set(incidentId, incident);

    console.log('Transcript updated for incident:', incidentId);
    return c.json({ success: true, data: incident });
  } catch (error) {
    console.error('Error updating transcript:', error);
    return c.json({ success: false, error: String(error) }, 500);
  }
});

app.patch('/make-server-dd6e4f12/incidents/:id', async (c) => {
  try {
    const incidentId = c.req.param('id');
    const updates = await c.req.json();
    
    const incident = await kv.get(incidentId);
    if (!incident) {
      return c.json({ success: false, error: 'Incident not found' }, 404);
    }

    const updatedIncident = {
      ...incident,
      ...updates,
      updatedAt: new Date().toISOString()
    };
    
    await kv.set(incidentId, updatedIncident);

    // If status changed to completed, remove from active list
    if (updates.status === 'completed') {
      const activeIncidents = await kv.get('active_incidents') || [];
      const filtered = activeIncidents.filter((id: string) => id !== incidentId);
      await kv.set('active_incidents', filtered);
    }

    console.log('Incident updated:', incidentId);
    return c.json({ success: true, data: updatedIncident });
  } catch (error) {
    console.error('Error updating incident:', error);
    return c.json({ success: false, error: String(error) }, 500);
  }
});

// Health check
app.get('/make-server-dd6e4f12/health', (c) => {
  return c.json({ status: 'ok', timestamp: new Date().toISOString() });
});

Deno.serve(app.fetch);

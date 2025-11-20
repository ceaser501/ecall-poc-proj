import { useState } from 'react';
import { ChevronDown, ChevronUp, Play } from 'lucide-react';

interface GlobalQuickViewProps {
  onFocusOnMap: (unitId: string) => void;
}

const allUnits = [
  { id: 'EMS-1', status: 'En-route', eta: 6, incident: 'A25', location: '테헤란로 152' },
  { id: 'Fire-3', status: 'Dispatched', eta: 8, incident: 'A25', location: '테헤란로 152' },
  { id: 'Police-2', status: 'On-scene', incident: 'A25', location: '테헤란로 152' },
  { id: 'EMS-5', status: 'Available' },
  { id: 'Fire-1', status: 'On-scene', incident: 'B07', location: '올림픽대로' },
];

const allIncidents = [
  { id: 'A25', priority: 'P4', type: 'medical', location: '테헤란로 152', status: 'Active' },
  { id: 'B07', priority: 'P3', type: 'traffic', location: '올림픽대로', status: 'Transport' },
];

export function GlobalQuickView({ onFocusOnMap }: GlobalQuickViewProps) {
  const [collapsed, setCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState<'units' | 'incidents'>('units');

  if (collapsed) {
    return (
      <button
        onClick={() => setCollapsed(false)}
        className="w-full bg-white rounded-2xl p-3 shadow-sm hover:shadow-md transition-shadow flex items-center justify-between"
      >
        <span className="text-[#0F172A]">Global Quick View</span>
        <ChevronDown className="size-4 text-gray-600" />
      </button>
    );
  }

  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-[#0F172A]">Global Quick View</h2>
        <button
          onClick={() => setCollapsed(true)}
          className="p-1 rounded hover:bg-[#F5F7FA]"
        >
          <ChevronUp className="size-4 text-gray-600" />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-3 bg-[#F5F7FA] rounded-lg p-1">
        <button
          onClick={() => setActiveTab('units')}
          className={`flex-1 px-3 py-1.5 rounded text-sm transition-all ${
            activeTab === 'units'
              ? 'bg-white text-[#0F172A] shadow-sm'
              : 'text-gray-600 hover:text-[#0F172A]'
          }`}
        >
          All Units
        </button>
        <button
          onClick={() => setActiveTab('incidents')}
          className={`flex-1 px-3 py-1.5 rounded text-sm transition-all ${
            activeTab === 'incidents'
              ? 'bg-white text-[#0F172A] shadow-sm'
              : 'text-gray-600 hover:text-[#0F172A]'
          }`}
        >
          Incidents
        </button>
      </div>

      {/* Content */}
      <div className="space-y-1.5" style={{ maxHeight: '120px', overflowY: 'auto' }}>
        {activeTab === 'units' ? (
          allUnits.map(unit => (
            <div 
              key={unit.id}
              className="flex items-center justify-between p-2 rounded-lg hover:bg-[#F5F7FA] text-sm"
            >
              <div className="flex items-center gap-2 flex-1">
                <span className="text-[#0F172A]">{unit.id}</span>
                <span className="text-gray-500">•</span>
                <span className="text-gray-600">{unit.status}</span>
                {unit.eta && (
                  <>
                    <span className="text-gray-500">•</span>
                    <span className="text-gray-600">ETA {unit.eta}</span>
                  </>
                )}
              </div>
              <div className="flex items-center gap-2">
                {unit.incident ? (
                  <span className="text-sm text-gray-600">
                    INC {unit.incident} ({unit.location})
                  </span>
                ) : (
                  <span className="text-sm text-gray-400">—</span>
                )}
                <button 
                  onClick={() => onFocusOnMap(unit.id)}
                  className="p-1 rounded hover:bg-white text-[#1D4ED8] transition-colors"
                  title="Focus on map"
                >
                  <Play className="size-4" />
                </button>
              </div>
            </div>
          ))
        ) : (
          allIncidents.map(incident => (
            <div 
              key={incident.id}
              className="flex items-center gap-2 p-2 rounded-lg hover:bg-[#F5F7FA] text-sm"
            >
              <span className="text-[#0F172A]">{incident.id}</span>
              <span className="text-gray-500">|</span>
              <span className="px-2 py-0.5 bg-[#FEF3C7] text-[#F59E0B] rounded text-xs">
                {incident.priority}
              </span>
              <span className="text-gray-600">{incident.type}</span>
              <span className="text-gray-500">|</span>
              <span className="text-gray-600">{incident.location}</span>
              <span className="text-gray-500">|</span>
              <span className="text-[#10B981]">{incident.status}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

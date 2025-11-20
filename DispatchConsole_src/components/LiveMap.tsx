import { useState } from 'react';
import { MapPin, Ambulance, Flame, Shield, Navigation } from 'lucide-react';
import type { FocusMode } from '../App';

interface LiveMapProps {
  activeLayers: {
    units: boolean;
    incidents: boolean;
    routes: boolean;
    traffic: boolean;
    geofence: boolean;
  };
  focusMode: FocusMode;
  onFocusUnit: (unitId: string) => void;
}

interface MapUnit {
  id: string;
  type: 'EMS' | 'Fire' | 'Police';
  status: string;
  position: { x: number; y: number };
  incident?: string;
  priority?: string;
  eta?: number;
}

const units: MapUnit[] = [
  { id: 'EMS-1', type: 'EMS', status: 'en-route', position: { x: 35, y: 45 }, incident: 'A25', priority: 'P4', eta: 6 },
  { id: 'Fire-3', type: 'Fire', status: 'dispatched', position: { x: 25, y: 60 }, incident: 'A25', priority: 'P4', eta: 8 },
  { id: 'Police-2', type: 'Police', status: 'on-scene', position: { x: 55, y: 35 }, incident: 'A25', priority: 'P4' },
  { id: 'EMS-5', type: 'EMS', status: 'available', position: { x: 70, y: 50 } },
  { id: 'Fire-1', type: 'Fire', status: 'on-scene', position: { x: 80, y: 70 }, incident: 'B07', priority: 'P3' },
];

const statusColors: Record<string, string> = {
  available: '#10B981',
  dispatched: '#38BDF8',
  'en-route': '#F59E0B',
  'on-scene': '#1D4ED8',
};

const typeIcons = {
  EMS: Ambulance,
  Fire: Flame,
  Police: Shield,
};

export function LiveMap({ activeLayers, focusMode, onFocusUnit }: LiveMapProps) {
  const [selectedUnit, setSelectedUnit] = useState<string | null>(null);

  const visibleUnits = units.filter(unit => {
    if (focusMode === 'incident') {
      return unit.incident === 'A25';
    }
    return true;
  });

  const handleUnitClick = (unitId: string) => {
    setSelectedUnit(selectedUnit === unitId ? null : unitId);
  };

  const selectedUnitData = visibleUnits.find(u => u.id === selectedUnit);

  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm relative" style={{ height: '380px' }}>
      <h2 className="text-[#0F172A] mb-3">Live Map</h2>
      
      {/* Map Container */}
      <div className="relative w-full h-[310px] bg-gradient-to-br from-gray-50 to-gray-100 rounded-xl overflow-hidden border border-gray-200">
        {/* Grid pattern */}
        <div className="absolute inset-0" style={{
          backgroundImage: 'linear-gradient(#e5e7eb 1px, transparent 1px), linear-gradient(90deg, #e5e7eb 1px, transparent 1px)',
          backgroundSize: '40px 40px'
        }} />

        {/* Roads */}
        <div className="absolute top-1/3 left-0 right-0 h-16 bg-gray-200 opacity-40" />
        <div className="absolute top-0 bottom-0 left-1/2 w-16 bg-gray-200 opacity-40" />

        {/* Incident Location (if showing incidents layer) */}
        {activeLayers.incidents && (
          <div 
            className="absolute size-12 -ml-6 -mt-6"
            style={{ left: '55%', top: '35%' }}
          >
            <div className="relative">
              <div className="absolute inset-0 bg-[#EF4444] rounded-full opacity-20 animate-ping" />
              <MapPin className="size-12 text-[#EF4444] drop-shadow-lg" fill="#EF4444" />
            </div>
          </div>
        )}

        {/* Unit pins */}
        {activeLayers.units && visibleUnits.map(unit => {
          const Icon = typeIcons[unit.type];
          return (
            <button
              key={unit.id}
              onClick={() => handleUnitClick(unit.id)}
              className="absolute -ml-5 -mt-5 transition-transform hover:scale-110 focus:outline-none focus:ring-2 focus:ring-[#1D4ED8] focus:ring-offset-2 rounded-lg"
              style={{ 
                left: `${unit.position.x}%`, 
                top: `${unit.position.y}%` 
              }}
            >
              <div className="relative">
                <div 
                  className="size-10 rounded-xl flex items-center justify-center shadow-lg"
                  style={{ backgroundColor: statusColors[unit.status] || '#6B7280' }}
                >
                  <Icon className="size-5 text-white" />
                </div>
                {focusMode === 'both' && unit.incident && (
                  <div className="absolute -top-2 -right-2 px-1.5 py-0.5 bg-[#EF4444] text-white text-xs rounded">
                    {unit.incident}
                  </div>
                )}
              </div>
            </button>
          );
        })}

        {/* Routes */}
        {activeLayers.routes && (
          <svg className="absolute inset-0 w-full h-full pointer-events-none">
            <path
              d="M 35% 45% Q 45% 40%, 55% 35%"
              stroke="#F59E0B"
              strokeWidth="3"
              fill="none"
              strokeDasharray="8,4"
            />
          </svg>
        )}

        {/* Unit Tooltip */}
        {selectedUnit && selectedUnitData && (
          <div 
            className="absolute z-10 bg-white rounded-xl shadow-xl p-4 border border-gray-200"
            style={{ 
              left: `${selectedUnitData.position.x}%`, 
              top: `${selectedUnitData.position.y - 15}%`,
              transform: 'translateX(-50%)',
              minWidth: '200px'
            }}
          >
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[#0F172A]">{selectedUnitData.id}</span>
                <span 
                  className="px-2 py-0.5 rounded text-sm"
                  style={{ 
                    color: statusColors[selectedUnitData.status],
                    backgroundColor: `${statusColors[selectedUnitData.status]}20`
                  }}
                >
                  {selectedUnitData.status}
                </span>
              </div>
              {selectedUnitData.eta && (
                <div className="text-sm text-gray-600">
                  ETA: {selectedUnitData.eta.toString().padStart(2, '0')} min
                </div>
              )}
              <button 
                onClick={() => onFocusUnit(selectedUnitData.id)}
                className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-[#1D4ED8] text-white rounded-lg hover:bg-[#1e40af] text-sm"
              >
                <Navigation className="size-4" />
                사건 포커스
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

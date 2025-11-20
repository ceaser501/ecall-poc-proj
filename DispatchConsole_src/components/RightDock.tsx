import { useState } from 'react';
import { ChevronLeft, ChevronRight, Users, AlertCircle, Search, Play } from 'lucide-react';

interface RightDockProps {
  isExpanded: boolean;
  onToggle: () => void;
  onFocusOnMap: (unitId: string) => void;
}

const allUnits = [
  { id: 'EMS-1', status: 'En-route', eta: 6, incident: 'A25', location: '테헤란로 152' },
  { id: 'EMS-2', status: 'Available' },
  { id: 'EMS-3', status: 'On-scene', incident: 'A25', location: '테헤란로 152' },
  { id: 'EMS-5', status: 'Available' },
  { id: 'Fire-1', status: 'On-scene', incident: 'B07', location: '올림픽대로' },
  { id: 'Fire-2', status: 'Available' },
  { id: 'Fire-3', status: 'Dispatched', eta: 8, incident: 'A25', location: '테헤란로 152' },
  { id: 'Police-1', status: 'Available' },
  { id: 'Police-2', status: 'On-scene', incident: 'A25', location: '테헤란로 152' },
];

const allIncidents = [
  { id: 'A25', priority: 'P4', type: 'medical', location: '테헤란로 152', status: 'Active' },
  { id: 'B07', priority: 'P3', type: 'traffic', location: '올림픽대로', status: 'Transport' },
  { id: 'C12', priority: 'P2', type: 'fire', location: '강남대로 405', status: 'Pending' },
];

export function RightDock({ isExpanded, onToggle, onFocusOnMap }: RightDockProps) {
  const [activeTab, setActiveTab] = useState<'roster' | 'incidents'>('roster');
  const [searchQuery, setSearchQuery] = useState('');

  if (!isExpanded) {
    return (
      <div className="w-16 bg-white border-l border-gray-200 flex flex-col items-center py-4 gap-3">
        <button
          onClick={onToggle}
          className="p-2 rounded-lg hover:bg-[#F5F7FA] text-gray-600"
        >
          <ChevronLeft className="size-5" />
        </button>
        <button
          onClick={() => {
            onToggle();
            setActiveTab('roster');
          }}
          className={`p-2 rounded-lg ${
            activeTab === 'roster' 
              ? 'bg-[#1D4ED8] text-white' 
              : 'hover:bg-[#F5F7FA] text-gray-600'
          }`}
        >
          <Users className="size-5" />
        </button>
        <button
          onClick={() => {
            onToggle();
            setActiveTab('incidents');
          }}
          className={`p-2 rounded-lg ${
            activeTab === 'incidents' 
              ? 'bg-[#1D4ED8] text-white' 
              : 'hover:bg-[#F5F7FA] text-gray-600'
          }`}
        >
          <AlertCircle className="size-5" />
        </button>
      </div>
    );
  }

  return (
    <div 
      className="bg-white border-l border-gray-200 flex flex-col"
      style={{ width: '280px' }}
    >
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-[#0F172A]">Global View</h2>
          <button
            onClick={onToggle}
            className="p-1 rounded hover:bg-[#F5F7FA] text-gray-600"
          >
            <ChevronRight className="size-5" />
          </button>
        </div>

        {/* Tabs */}
        <div className="flex gap-1 bg-[#F5F7FA] rounded-lg p-1">
          <button
            onClick={() => setActiveTab('roster')}
            className={`flex-1 px-3 py-1.5 rounded text-sm transition-all ${
              activeTab === 'roster'
                ? 'bg-white text-[#0F172A] shadow-sm'
                : 'text-gray-600 hover:text-[#0F172A]'
            }`}
          >
            Roster
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
      </div>

      {/* Search */}
      <div className="p-4 border-b border-gray-200">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-gray-400" />
          <input
            type="text"
            placeholder="Search..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1D4ED8] text-sm"
          />
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4">
        {activeTab === 'roster' ? (
          <div className="space-y-2">
            {allUnits.map(unit => (
              <div 
                key={unit.id}
                className="p-3 rounded-xl bg-[#F5F7FA] hover:bg-gray-100 transition-colors"
              >
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <div className="text-[#0F172A] mb-1">{unit.id}</div>
                    <div className="text-sm text-gray-600">{unit.status}</div>
                    {unit.eta && (
                      <div className="text-sm text-gray-600">ETA: {unit.eta} min</div>
                    )}
                  </div>
                  <button 
                    onClick={() => onFocusOnMap(unit.id)}
                    className="p-1.5 rounded-lg hover:bg-white text-[#1D4ED8] transition-colors"
                  >
                    <Play className="size-4" />
                  </button>
                </div>
                {unit.incident && (
                  <div className="text-sm text-gray-600 pt-2 border-t border-gray-200">
                    INC {unit.incident} • {unit.location}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-2">
            {allIncidents.map(incident => (
              <div 
                key={incident.id}
                className="p-3 rounded-xl bg-[#F5F7FA] hover:bg-gray-100 transition-colors"
              >
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-[#0F172A]">{incident.id}</span>
                  <span className="px-2 py-0.5 bg-[#FEF3C7] text-[#F59E0B] rounded text-xs">
                    {incident.priority}
                  </span>
                </div>
                <div className="text-sm text-gray-600 mb-1">{incident.type}</div>
                <div className="text-sm text-gray-600 mb-2">{incident.location}</div>
                <div className="flex items-center justify-between pt-2 border-t border-gray-200">
                  <span className="text-sm text-[#10B981]">{incident.status}</span>
                  <button className="p-1 rounded hover:bg-white text-[#1D4ED8]">
                    <Play className="size-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

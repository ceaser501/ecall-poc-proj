import { Users, AlertCircle, Route, Car, Hexagon } from 'lucide-react';

interface LayerChipsProps {
  activeLayers: {
    units: boolean;
    incidents: boolean;
    routes: boolean;
    traffic: boolean;
    geofence: boolean;
  };
  onToggle: (layer: keyof LayerChipsProps['activeLayers']) => void;
}

const layerConfig = [
  { key: 'units' as const, label: 'Units', Icon: Users },
  { key: 'incidents' as const, label: 'Incidents', Icon: AlertCircle },
  { key: 'routes' as const, label: 'Routes', Icon: Route },
  { key: 'traffic' as const, label: 'Traffic', Icon: Car },
  { key: 'geofence' as const, label: 'Geofence', Icon: Hexagon },
];

export function LayerChips({ activeLayers, onToggle }: LayerChipsProps) {
  return (
    <div className="flex items-center gap-2">
      {layerConfig.map(({ key, label, Icon }) => (
        <button
          key={key}
          onClick={() => onToggle(key)}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm transition-all ${
            activeLayers[key]
              ? 'bg-[#1D4ED8] text-white shadow-sm'
              : 'bg-white text-gray-600 border border-gray-200 hover:border-[#1D4ED8]'
          }`}
        >
          <Icon className="size-4" />
          <span>{label}</span>
        </button>
      ))}
    </div>
  );
}
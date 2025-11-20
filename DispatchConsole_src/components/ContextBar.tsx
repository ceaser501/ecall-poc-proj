import { Clock, Users } from 'lucide-react';
import { FocusToggle } from './FocusToggle';
import { LayerChips } from './LayerChips';
import type { FocusMode } from '../App';

interface ContextBarProps {
  focusMode: FocusMode;
  onFocusModeChange: (mode: FocusMode) => void;
  activeLayers: {
    units: boolean;
    incidents: boolean;
    routes: boolean;
    traffic: boolean;
    geofence: boolean;
  };
  onToggleLayer: (layer: keyof ContextBarProps['activeLayers']) => void;
}

export function ContextBar({ 
  focusMode, 
  onFocusModeChange, 
  activeLayers, 
  onToggleLayer 
}: ContextBarProps) {
  return (
    <div className="sticky top-0 z-50 bg-white border-b border-gray-200">
      <div className="px-6 py-3">
        {/* Top row - Incident info and units */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-3">
            <span className="px-3 py-1 bg-[#1D4ED8] text-white rounded-lg">INC A25-031</span>
            <span className="px-2 py-1 bg-[#F59E0B] text-white rounded text-sm">P4(높음)</span>
            <span className="text-[#0F172A]">medical</span>
            <span className="text-gray-500">•</span>
            <span className="text-[#0F172A]">서울 강남구 테헤란로 152</span>
            <span className="text-gray-500">•</span>
            <div className="flex items-center gap-2 text-gray-600">
              <Clock className="size-4" />
              <span>Elapsed 04:12</span>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            <Users className="size-4 text-gray-600" />
            <span className="text-[#0F172A]">Units:</span>
            <span className="text-[#EF4444]">Fire 2</span>
            <span className="text-gray-400">•</span>
            <span className="text-[#10B981]">EMS 3</span>
            <span className="text-gray-400">•</span>
            <span className="text-[#1D4ED8]">Police 1</span>
          </div>
        </div>

        {/* Bottom row - Focus toggle and layers */}
        <div className="flex items-center justify-between">
          <FocusToggle mode={focusMode} onChange={onFocusModeChange} />
          <LayerChips activeLayers={activeLayers} onToggle={onToggleLayer} />
        </div>
      </div>
    </div>
  );
}

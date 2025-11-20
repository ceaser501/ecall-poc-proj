import { useState } from 'react';
import { ContextBar } from './components/ContextBar';
import { IncidentPanel } from './components/IncidentPanel';
import { LiveMap } from './components/LiveMap';
import { Comms } from './components/Comms';
import { GlobalQuickView } from './components/GlobalQuickView';
import { RightDock } from './components/RightDock';
import { Footer } from './components/Footer';

export type FocusMode = 'incident' | 'global' | 'both';

export default function App() {
  const [focusMode, setFocusMode] = useState<FocusMode>('both');
  const [activeLayers, setActiveLayers] = useState({
    units: true,
    incidents: true,
    routes: true,
    traffic: false,
    geofence: false,
  });
  const [rightDockExpanded, setRightDockExpanded] = useState(true);

  const toggleLayer = (layer: keyof typeof activeLayers) => {
    setActiveLayers(prev => ({ ...prev, [layer]: !prev[layer] }));
  };

  const handleFocusModeChange = (mode: FocusMode) => {
    setFocusMode(mode);
    if (mode === 'incident') {
      setRightDockExpanded(false);
    } else if (mode === 'both') {
      setRightDockExpanded(true);
    }
  };

  const handleFocusOnMap = (unitId: string) => {
    setFocusMode('incident');
    // In a real app, would pan map to unit location
    console.log('Focus on unit:', unitId);
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <ContextBar
        focusMode={focusMode}
        onFocusModeChange={handleFocusModeChange}
        activeLayers={activeLayers}
        onToggleLayer={toggleLayer}
      />
      
      <div className="flex" style={{ height: 'calc(100vh - 104px)' }}>
        {/* Left Panel - 4 columns */}
        <div 
          className={`transition-opacity duration-300 ${
            focusMode === 'global' ? 'opacity-60' : 'opacity-100'
          }`}
          style={{ width: 'calc(33.333% - 8px)', minWidth: '380px' }}
        >
          <IncidentPanel />
        </div>

        {/* Center Panel - 6 columns */}
        <div 
          className="flex-1 px-4"
          style={{ minWidth: '500px' }}
        >
          <div className="space-y-4 h-full flex flex-col">
            <LiveMap 
              activeLayers={activeLayers}
              focusMode={focusMode}
              onFocusUnit={handleFocusOnMap}
            />
            <Comms />
            {focusMode !== 'incident' && <GlobalQuickView onFocusOnMap={handleFocusOnMap} />}
          </div>
        </div>

        {/* Right Dock - 2 columns */}
        {focusMode !== 'incident' && (
          <RightDock 
            isExpanded={rightDockExpanded}
            onToggle={() => setRightDockExpanded(!rightDockExpanded)}
            onFocusOnMap={handleFocusOnMap}
          />
        )}
      </div>

      <Footer />
    </div>
  );
}

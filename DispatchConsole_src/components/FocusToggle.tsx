import type { FocusMode } from '../App';

interface FocusToggleProps {
  mode: FocusMode;
  onChange: (mode: FocusMode) => void;
}

export function FocusToggle({ mode, onChange }: FocusToggleProps) {
  return (
    <div className="inline-flex rounded-xl bg-[#F5F7FA] p-1">
      <button
        onClick={() => onChange('incident')}
        className={`px-4 py-1.5 rounded-lg text-sm transition-all ${
          mode === 'incident'
            ? 'bg-white text-[#0F172A] shadow-sm'
            : 'text-gray-600 hover:text-[#0F172A]'
        }`}
      >
        Incident Only
      </button>
      <button
        onClick={() => onChange('global')}
        className={`px-4 py-1.5 rounded-lg text-sm transition-all ${
          mode === 'global'
            ? 'bg-white text-[#0F172A] shadow-sm'
            : 'text-gray-600 hover:text-[#0F172A]'
        }`}
      >
        Global
      </button>
      <button
        onClick={() => onChange('both')}
        className={`px-4 py-1.5 rounded-lg text-sm transition-all ${
          mode === 'both'
            ? 'bg-white text-[#0F172A] shadow-sm'
            : 'text-gray-600 hover:text-[#0F172A]'
        }`}
      >
        Both
      </button>
    </div>
  );
}

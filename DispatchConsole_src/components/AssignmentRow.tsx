import { ArrowLeft, X, CheckCircle } from 'lucide-react';

type UnitStatus = 'available' | 'dispatched' | 'en-route' | 'on-scene' | 'transport' | 'clear';

interface AssignmentRowProps {
  id: string;
  type: string;
  status: UnitStatus;
  eta?: number;
}

const statusConfig: Record<UnitStatus, { label: string; color: string; bgColor: string }> = {
  available: { label: 'Available', color: '#10B981', bgColor: '#D1FAE5' },
  dispatched: { label: 'Dispatched', color: '#38BDF8', bgColor: '#E0F2FE' },
  'en-route': { label: 'En-route', color: '#F59E0B', bgColor: '#FEF3C7' },
  'on-scene': { label: 'On-scene', color: '#1D4ED8', bgColor: '#DBEAFE' },
  transport: { label: 'Transport', color: '#8B5CF6', bgColor: '#EDE9FE' },
  clear: { label: 'Clear', color: '#6B7280', bgColor: '#F3F4F6' },
};

export function AssignmentRow({ id, type, status, eta }: AssignmentRowProps) {
  const config = statusConfig[status];
  const showRecallReassign = status === 'dispatched' || status === 'en-route';
  const showClear = status === 'on-scene';

  return (
    <div className="flex items-center justify-between p-3 rounded-xl bg-[#F5F7FA] hover:bg-gray-100 transition-colors">
      <div className="flex items-center gap-3">
        <span className="text-[#0F172A]">{id}</span>
        <span className="text-gray-500">•</span>
        <span 
          className="px-2 py-0.5 rounded text-sm"
          style={{ 
            color: config.color,
            backgroundColor: config.bgColor 
          }}
        >
          {config.label}
        </span>
        {eta && (
          <>
            <span className="text-gray-500">•</span>
            <span className="text-sm text-gray-600">ETA {eta.toString().padStart(2, '0')}:</span>
          </>
        )}
      </div>

      <div className="flex gap-2">
        {showRecallReassign && (
          <>
            <button className="px-3 py-1 text-sm text-gray-600 hover:text-[#1D4ED8] hover:bg-white rounded-lg transition-colors flex items-center gap-1">
              <ArrowLeft className="size-3" />
              Reassign
            </button>
            <button className="px-3 py-1 text-sm text-gray-600 hover:text-[#EF4444] hover:bg-white rounded-lg transition-colors flex items-center gap-1">
              <X className="size-3" />
              Recall
            </button>
          </>
        )}
        {showClear && (
          <>
            <button className="px-3 py-1 text-sm text-gray-600 hover:text-[#1D4ED8] hover:bg-white rounded-lg transition-colors flex items-center gap-1">
              <ArrowLeft className="size-3" />
              Reassign
            </button>
            <button className="px-3 py-1 text-sm text-gray-600 hover:text-[#10B981] hover:bg-white rounded-lg transition-colors flex items-center gap-1">
              <CheckCircle className="size-3" />
              Clear
            </button>
          </>
        )}
      </div>
    </div>
  );
}

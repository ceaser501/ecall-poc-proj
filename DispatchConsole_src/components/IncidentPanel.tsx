import { Plus, UserPlus } from 'lucide-react';
import { AssignmentRow } from './AssignmentRow';
import { Timeline } from './Timeline';

const assignments = [
  { id: 'EMS-1', type: 'EMS', status: 'en-route' as const, eta: 6 },
  { id: 'Fire-3', type: 'Fire', status: 'dispatched' as const, eta: 8 },
  { id: 'Police-2', type: 'Police', status: 'on-scene' as const },
];

const timelineEvents = [
  { time: '10:12', event: '지시' },
  { time: '10:22', event: '출동' },
  { time: '10:29', event: '도착' },
  { time: '10:31', event: 'EMS-1 현장 도착' },
  { time: '10:34', event: 'Fire-3 출동' },
];

export function IncidentPanel() {
  return (
    <div className="p-4 space-y-4 overflow-y-auto h-full">
      {/* Assignment Card */}
      <div className="bg-white rounded-2xl p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-[#0F172A]">Assignment — This Incident</h2>
          <div className="flex gap-2">
            <button className="p-1.5 rounded-lg hover:bg-[#F5F7FA] text-[#1D4ED8]">
              <Plus className="size-4" />
            </button>
            <button className="flex items-center gap-1 px-3 py-1.5 bg-[#1D4ED8] text-white rounded-lg hover:bg-[#1e40af] text-sm">
              <UserPlus className="size-4" />
              Request
            </button>
          </div>
        </div>

        <div className="space-y-2">
          {assignments.map(assignment => (
            <AssignmentRow key={assignment.id} {...assignment} />
          ))}
        </div>
      </div>

      {/* Timeline Card */}
      <div className="bg-white rounded-2xl p-5 shadow-sm">
        <h2 className="text-[#0F172A] mb-4">Incident Timeline</h2>
        <Timeline events={timelineEvents} />
      </div>
    </div>
  );
}

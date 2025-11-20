import { Clock } from 'lucide-react';

interface TimelineEvent {
  time: string;
  event: string;
}

interface TimelineProps {
  events: TimelineEvent[];
}

export function Timeline({ events }: TimelineProps) {
  return (
    <div className="space-y-3">
      {events.map((item, index) => (
        <div key={index} className="flex items-start gap-3">
          <div className="flex items-center justify-center size-8 rounded-lg bg-[#DBEAFE] text-[#1D4ED8] shrink-0">
            <Clock className="size-4" />
          </div>
          <div className="flex-1 pt-1">
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-600">{item.time}</span>
              <span className="text-[#0F172A]">{item.event}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

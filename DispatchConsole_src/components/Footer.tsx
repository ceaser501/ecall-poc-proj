import { CheckCircle, Radio, Map, Clock } from 'lucide-react';

export function Footer() {
  return (
    <div className="sticky bottom-0 bg-white border-t border-gray-200 px-6 py-2.5">
      <div className="flex items-center justify-between text-sm">
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            <CheckCircle className="size-4 text-[#10B981]" />
            <span className="text-gray-600">CAD:</span>
            <span className="text-[#10B981]">OK</span>
          </div>
          
          <div className="flex items-center gap-2">
            <Radio className="size-4 text-[#10B981]" />
            <span className="text-gray-600">Radio:</span>
            <span className="text-[#10B981]">OK</span>
          </div>
          
          <div className="flex items-center gap-2">
            <Map className="size-4 text-[#10B981]" />
            <span className="text-gray-600">Map:</span>
            <span className="text-[#10B981]">OK</span>
          </div>
        </div>

        <div className="flex items-center gap-2 text-gray-600">
          <Clock className="size-4" />
          <span>Last update 2s ago</span>
        </div>
      </div>
    </div>
  );
}

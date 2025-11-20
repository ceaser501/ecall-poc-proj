import { Send, ChevronDown } from 'lucide-react';

const messages = [
  { time: '10:31', from: 'EMS-1', message: 'Arriving via Gate 3', type: 'unit' as const },
  { time: '10:32', from: 'Dispatch', message: 'Use south access', type: 'dispatch' as const },
  { time: '10:33', from: 'Fire-3', message: 'En route, ETA 8 minutes', type: 'unit' as const },
  { time: '10:34', from: 'Dispatch', message: 'Traffic clear on Teheran-ro', type: 'dispatch' as const },
];

export function Comms() {
  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm" style={{ height: '180px' }}>
      <h2 className="text-[#0F172A] mb-3">Communications</h2>
      
      {/* Messages */}
      <div className="space-y-2 mb-3 overflow-y-auto" style={{ height: '80px' }}>
        {messages.map((msg, index) => (
          <div 
            key={index}
            className={`flex items-start gap-2 text-sm p-2 rounded-lg ${
              msg.type === 'dispatch' 
                ? 'bg-[#DBEAFE]' 
                : 'bg-[#F5F7FA]'
            }`}
          >
            <span className="text-gray-500 shrink-0">[{msg.time}</span>
            <span 
              className={`shrink-0 ${
                msg.type === 'dispatch' ? 'text-[#1D4ED8]' : 'text-[#0F172A]'
              }`}
            >
              {msg.from}]
            </span>
            <span className="text-gray-700">{msg.message}</span>
          </div>
        ))}
      </div>

      {/* Input */}
      <div className="flex gap-2">
        <input
          type="text"
          placeholder="Message…"
          className="flex-1 px-3 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1D4ED8] text-sm"
        />
        <button className="px-4 py-2 bg-[#1D4ED8] text-white rounded-lg hover:bg-[#1e40af] transition-colors">
          <Send className="size-4" />
        </button>
        <button className="px-3 py-2 border border-gray-200 rounded-lg hover:border-[#1D4ED8] transition-colors">
          <ChevronDown className="size-4" />
        </button>
      </div>
    </div>
  );
}

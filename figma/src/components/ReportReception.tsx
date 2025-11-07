import { useState } from 'react';
import { 
  Phone, 
  MapPin, 
  User, 
  Clock,
  Settings,
  UserCircle,
  ChevronDown,
  Play,
  Eye,
  AlertCircle,
  FileAudio,
  CheckCircle,
  XCircle
} from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';

interface TranscriptMessage {
  id: number;
  sender: 'operator' | 'caller';
  senderName: string;
  message: string;
  timestamp: string;
}

interface TimelineEvent {
  id: number;
  type: string;
  timestamp: string;
}

interface ChecklistItem {
  id: number;
  question: string;
  answer: boolean | null;
}

const checklistsByType: Record<string, ChecklistItem[]> = {
  emergency: [
    { id: 1, question: 'Is the patient conscious?', answer: null },
    { id: 2, question: 'Is the patient breathing?', answer: null },
    { id: 3, question: 'Is there any bleeding?', answer: null },
    { id: 4, question: 'Does the patient complain of chest pain?', answer: null },
    { id: 5, question: 'Is there any vomiting?', answer: null },
  ],
  fire: [
    { id: 1, question: 'Did the fire occur inside the building?', answer: null },
    { id: 2, question: 'Are there any casualties?', answer: null },
    { id: 3, question: 'Has evacuation been completed?', answer: null },
    { id: 4, question: 'Do you smell gas?', answer: null },
  ],
  crime: [
    { id: 1, question: 'Is the perpetrator at the scene?', answer: null },
    { id: 2, question: 'Is the perpetrator armed?', answer: null },
    { id: 3, question: 'Are there any injured persons?', answer: null },
    { id: 4, question: 'Is the caller in a safe location?', answer: null },
  ],
  accident: [
    { id: 1, question: 'Are there any injured persons?', answer: null },
    { id: 2, question: 'Is the vehicle blocking the road?', answer: null },
    { id: 3, question: 'Is there any fuel leakage?', answer: null },
    { id: 4, question: 'Is an ambulance needed?', answer: null },
  ],
};

type IncidentStatus = 'received' | 'dispatched' | 'completed' | 'cancelled';

const statusConfig: Record<IncidentStatus, { label: string; color: string; bgColor: string }> = {
  received: { label: 'Received', color: 'text-blue-700', bgColor: 'bg-blue-100' },
  dispatched: { label: 'Dispatched', color: 'text-orange-700', bgColor: 'bg-orange-100' },
  completed: { label: 'Completed', color: 'text-green-700', bgColor: 'bg-green-100' },
  cancelled: { label: 'Cancelled', color: 'text-slate-700', bgColor: 'bg-slate-100' },
};

export default function ReportReception() {
  const [activeCall, setActiveCall] = useState(true);
  const [currentStatus, setCurrentStatus] = useState<IncidentStatus>('received');
  const [selectedIncidentType, setSelectedIncidentType] = useState('emergency');
  const [checklistOpen, setChecklistOpen] = useState(false);
  const [checklist, setChecklist] = useState<ChecklistItem[]>(checklistsByType.emergency);
  const [audioUploadOpen, setAudioUploadOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [transcriptMessages, setTranscriptMessages] = useState<TranscriptMessage[]>([
    {
      id: 1,
      sender: 'operator',
      senderName: 'Operator',
      message: 'I understand. Is the patient conscious? Are their eyes open?',
      timestamp: '14:23:35'
    },
    {
      id: 2,
      sender: 'caller',
      senderName: 'Caller',
      message: 'Their eyes are closed. They look pale and... there\'s a lot of blood! What should I do?!',
      timestamp: '14:23:40'
    },
    {
      id: 3,
      sender: 'operator',
      senderName: 'Operator',
      message: 'Stay calm. An ambulance is on the way. Are they breathing?',
      timestamp: '14:23:48'
    },
    {
      id: 4,
      sender: 'caller',
      senderName: 'Caller',
      message: 'Yes... they\'re breathing but it sounds rough. Please hurry!',
      timestamp: '14:23:53'
    }
  ]);

  const [timelineEvents] = useState<TimelineEvent[]>([
    { id: 1, type: 'Call Started', timestamp: '14:23:15' },
    { id: 2, type: 'Location Confirmed', timestamp: '14:23:42' },
    { id: 3, type: 'Elapsed Time', timestamp: '03:42' }
  ]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Header */}
      <div className="bg-white border-b border-slate-200">
        <div className="max-w-[1600px] mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="bg-gradient-to-br from-red-500 to-red-600 p-2.5 rounded-lg">
                <Phone className="w-5 h-5 text-white" />
              </div>
              <h1 className="text-slate-900">Report Reception — ECA103</h1>
            </div>
            
            <div className="flex items-center gap-4">
              <Button 
                variant="outline" 
                className="bg-blue-50 text-blue-700 border-blue-200"
                onClick={() => setAudioUploadOpen(true)}
              >
                <FileAudio className="w-4 h-4 mr-2" />
                Upload Audio File
              </Button>
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                <span className="text-slate-600">On Call</span>
                <span className="text-slate-900">03:42</span>
              </div>
              <Button variant="ghost" size="icon">
                <Settings className="w-5 h-5 text-slate-600" />
              </Button>
              <Button variant="ghost" size="icon">
                <UserCircle className="w-5 h-5 text-slate-600" />
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-[1600px] mx-auto px-6 py-6">
        <div className="grid grid-cols-12 gap-6">
          {/* Left Sidebar - 필수 정보 */}
          <div className="col-span-3">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
              <div className="flex items-center gap-2 mb-4">
                <AlertCircle className="w-5 h-5 text-slate-700" />
                <h2 className="text-slate-900">Caller Information</h2>
              </div>

              <div className="space-y-3">
                {/* 발신번호 */}
                <div>
                  <Label className="text-slate-700 mb-1.5">Caller Number</Label>
                  <Input 
                    value="010-8734-2910" 
                    readOnly
                    className="bg-slate-50"
                  />
                </div>

                {/* 발신자명 */}
                <div>
                  <Label className="text-slate-700 mb-1.5">Caller Name</Label>
                  <Input 
                    defaultValue="Minsu Kim"
                    className="bg-white"
                  />
                </div>

                {/* 사고 위치 */}
                <div>
                  <Label className="text-slate-700 mb-1.5">Incident Location</Label>
                  <Input 
                    defaultValue="152 Daechi-dong, Gangnam-gu, Seoul"
                    className="bg-white"
                  />
                </div>

                {/* 사건 유형 */}
                <div>
                  <Label className="text-slate-700 mb-1.5">Incident Type</Label>
                  <Select defaultValue="emergency" onValueChange={(value) => {
                    setSelectedIncidentType(value);
                    setChecklist(checklistsByType[value]);
                  }}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="emergency">Emergency</SelectItem>
                      <SelectItem value="fire">Fire</SelectItem>
                      <SelectItem value="crime">Crime</SelectItem>
                      <SelectItem value="accident">Accident</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* 주신고위 */}
                <div>
                  <Label className="text-slate-700 mb-1.5">Severity Level</Label>
                  <Select defaultValue="4">
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="1">Level 1 (Minor)</SelectItem>
                      <SelectItem value="2">Level 2</SelectItem>
                      <SelectItem value="3">Level 3</SelectItem>
                      <SelectItem value="4">Level 4 (Severe)</SelectItem>
                      <SelectItem value="5">Level 5 (Critical)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* 수정 버튼 */}
                <Button className="w-full bg-slate-600 hover:bg-slate-700">
                  Update
                </Button>
              </div>

              {/* 체크리스트 버튼 */}
              <div className="mt-5 pt-5 border-t border-slate-200">
                <Button 
                  variant="outline" 
                  className="w-full justify-center"
                  onClick={() => setChecklistOpen(!checklistOpen)}
                >
                  View Checklist
                </Button>
              </div>
            </div>

            {/* 체크리스트 Dialog */}
            <Dialog open={checklistOpen} onOpenChange={setChecklistOpen}>
              <DialogContent className="max-w-2xl">
                <DialogHeader>
                  <DialogTitle>Incident Type Checklist</DialogTitle>
                  <DialogDescription>
                    {selectedIncidentType === 'emergency' && 'Questions to assess the emergency situation.'}
                    {selectedIncidentType === 'fire' && 'Questions to assess the fire situation.'}
                    {selectedIncidentType === 'crime' && 'Questions to assess the crime situation.'}
                    {selectedIncidentType === 'accident' && 'Questions to assess the accident situation.'}
                  </DialogDescription>
                </DialogHeader>
                
                <div className="space-y-4 mt-4">
                  {checklist.map((item) => (
                    <div 
                      key={item.id} 
                      className="flex items-center justify-between p-4 bg-slate-50 rounded-lg"
                    >
                      <span className="text-slate-700 flex-1">{item.question}</span>
                      <div className="flex items-center gap-2 ml-4">
                        <Button
                          size="sm"
                          variant={item.answer === true ? "default" : "outline"}
                          className={item.answer === true ? "bg-green-600 hover:bg-green-700" : ""}
                          onClick={() => {
                            const updatedChecklist = checklist.map(c => 
                              c.id === item.id ? { ...c, answer: true } : c
                            );
                            setChecklist(updatedChecklist);
                          }}
                        >
                          <CheckCircle className="w-4 h-4 mr-1" />
                          Yes
                        </Button>
                        <Button
                          size="sm"
                          variant={item.answer === false ? "default" : "outline"}
                          className={item.answer === false ? "bg-red-600 hover:bg-red-700" : ""}
                          onClick={() => {
                            const updatedChecklist = checklist.map(c => 
                              c.id === item.id ? { ...c, answer: false } : c
                            );
                            setChecklist(updatedChecklist);
                          }}
                        >
                          <XCircle className="w-4 h-4 mr-1" />
                          No
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </DialogContent>
            </Dialog>
          </div>

          {/* Center - 실시간 전사 */}
          <div className="col-span-6">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 h-[800px]">
              {/* Header */}
              <div className="border-b border-slate-200 p-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-slate-900">Live Transcript</h2>
                  <div className="flex items-center gap-3">
                    <span className="text-slate-600">AI Confidence: 94%</span>
                    <Button variant="ghost" size="sm">
                      <Play className="w-4 h-4 mr-1" />
                      Play
                    </Button>
                  </div>
                </div>
              </div>

              {/* Transcript Messages */}
              <div className="p-4 overflow-y-auto h-[680px] space-y-3">
                {transcriptMessages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`
                      p-4 rounded-lg border-2
                      ${msg.sender === 'operator' 
                        ? 'bg-green-50 border-green-200' 
                        : 'bg-yellow-50 border-yellow-300'
                      }
                    `}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <div 
                          className={`
                            w-6 h-6 rounded-full flex items-center justify-center
                            ${msg.sender === 'operator' 
                              ? 'bg-green-500' 
                              : 'bg-blue-500'
                            }
                          `}
                        >
                          <span className="text-white text-xs">
                            {msg.sender === 'operator' ? 'O' : 'C'}
                          </span>
                        </div>
                        <span className="text-slate-900">{msg.senderName}</span>
                      </div>
                      <span className="text-slate-500">{msg.timestamp}</span>
                    </div>
                    <p className="text-slate-700 ml-8">{msg.message}</p>
                  </div>
                ))}
              </div>

              {/* Status Bar */}
              <div className="border-t border-slate-200 p-3 bg-slate-50">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse" />
                  <span className="text-slate-600">Voice recognition active...</span>
                </div>
              </div>
            </div>
          </div>

          {/* Right Sidebar */}
          <div className="col-span-3 space-y-5">
            {/* 접수자 정보 */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
              <h2 className="text-slate-900 mb-3">Operator Information</h2>
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-full bg-slate-200 overflow-hidden flex-shrink-0">
                  <img 
                    src="https://images.unsplash.com/photo-1715866558475-d2543c67a840?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwcm9mZXNzaW9uYWwlMjB3b21hbiUyMG9wZXJhdG9yJTIwaGVhZHNldHxlbnwxfHx8fDE3NjI1MDY1NjN8MA&ixlib=rb-4.1.0&q=80&w=1080" 
                    alt="Operator"
                    className="w-full h-full object-cover"
                  />
                </div>
                <div className="flex flex-col gap-0.5">
                  <p className="text-slate-900">Jihyun Park</p>
                  <p className="text-slate-600">Emergency Center</p>
                  <p className="text-slate-500">3 years exp.</p>
                </div>
              </div>
            </div>

            {/* 메뉴얼 */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
              <h2 className="text-slate-900 mb-3">Response Manual</h2>
              <div className="space-y-2">
                {['CPR Procedure', 'Bleeding Control', 'Shock Management', 'Airway Support'].map((item, index) => (
                  <div 
                    key={index}
                    className="bg-slate-100 p-2.5 rounded-lg text-slate-700 hover:bg-slate-200 cursor-pointer transition-colors"
                  >
                    {item}
                  </div>
                ))}
              </div>
            </div>

            {/* 타임라인 */}
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5">
              <div className="flex items-center gap-2 mb-3">
                <Clock className="w-5 h-5 text-slate-700" />
                <h2 className="text-slate-900">Timeline</h2>
              </div>
              <div className="space-y-2.5">
                {timelineEvents.map((event) => (
                  <div 
                    key={event.id}
                    className="flex items-center justify-between text-slate-700"
                  >
                    <span>{event.type}</span>
                    <span className="text-slate-500">{event.timestamp}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Audio Upload Dialog */}
      <Dialog open={audioUploadOpen} onOpenChange={setAudioUploadOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Upload Audio File</DialogTitle>
            <DialogDescription>
              Upload an MP3 audio file for transcription and analysis.
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4 mt-4">
            <div className="border-2 border-dashed border-slate-300 rounded-lg p-8 text-center hover:border-blue-400 transition-colors">
              <FileAudio className="w-12 h-12 text-slate-400 mx-auto mb-3" />
              <input
                type="file"
                accept=".mp3,audio/mp3,audio/mpeg"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    setSelectedFile(file);
                  }
                }}
                className="hidden"
                id="audio-upload"
              />
              <label
                htmlFor="audio-upload"
                className="cursor-pointer inline-block"
              >
                <div className="text-slate-700 mb-1">
                  Click to upload or drag and drop
                </div>
                <div className="text-slate-500">
                  MP3 files only
                </div>
              </label>
            </div>

            {selectedFile && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <FileAudio className="w-5 h-5 text-blue-600" />
                    <div>
                      <p className="text-slate-900">{selectedFile.name}</p>
                      <p className="text-slate-500">
                        {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                      </p>
                    </div>
                  </div>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setSelectedFile(null)}
                  >
                    <XCircle className="w-4 h-4 text-slate-600" />
                  </Button>
                </div>
              </div>
            )}

            <div className="flex gap-2 pt-4">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => {
                  setSelectedFile(null);
                  setAudioUploadOpen(false);
                }}
              >
                Cancel
              </Button>
              <Button
                className="flex-1 bg-blue-600 hover:bg-blue-700"
                disabled={!selectedFile}
                onClick={() => {
                  // Handle upload logic here
                  console.log('Uploading file:', selectedFile);
                  setAudioUploadOpen(false);
                }}
              >
                Upload
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
import { useState } from 'react';
import { 
  LayoutDashboard, 
  Phone, 
  Radio, 
  History, 
  Users, 
  BookOpen, 
  BarChart3, 
  Settings,
  ChevronRight
} from 'lucide-react';
import { motion } from 'motion/react';
import ReportReception from './components/ReportReception';

type Page = 'home' | 'report-reception';

interface MenuCard {
  id: number;
  title: string;
  description: string;
  icon: any;
  color: string;
  gradient: string;
}

const menuCards: MenuCard[] = [
  {
    id: 1,
    title: 'Comprehensive Dashboard',
    description: 'Real-time Report & \nDispatch Monitoring',
    icon: LayoutDashboard,
    color: 'from-blue-500 to-blue-600',
    gradient: 'bg-gradient-to-br from-blue-50 to-blue-100'
  },
  {
    id: 2,
    title: 'Report Reception',
    description: 'Voice/Text Intake & \nIncident Classification',
    icon: Phone,
    color: 'from-red-500 to-red-600',
    gradient: 'bg-gradient-to-br from-red-50 to-red-100'
  },
  {
    id: 3,
    title: 'Dispatch Console',
    description: 'Responder Assignment & \nField Control',
    icon: Radio,
    color: 'from-orange-500 to-orange-600',
    gradient: 'bg-gradient-to-br from-orange-50 to-orange-100'
  },
  {
    id: 4,
    title: 'Response History',
    description: 'Incident Review & \nTimeline Tracking',
    icon: History,
    color: 'from-green-500 to-green-600',
    gradient: 'bg-gradient-to-br from-green-50 to-green-100'
  },
  {
    id: 5,
    title: 'Administrator Panel',
    description: 'User, Role & \nDepartment Management',
    icon: Users,
    color: 'from-purple-500 to-purple-600',
    gradient: 'bg-gradient-to-br from-purple-50 to-purple-100'
  },
  {
    id: 6,
    title: 'Manual Management',
    description: 'Manual Library & \nKeyword Search',
    icon: BookOpen,
    color: 'from-teal-500 to-teal-600',
    gradient: 'bg-gradient-to-br from-teal-50 to-teal-100'
  },
  {
    id: 7,
    title: 'Statistics & Reports',
    description: 'Analytics, Metrics & \nVisualization',
    icon: BarChart3,
    color: 'from-indigo-500 to-indigo-600',
    gradient: 'bg-gradient-to-br from-indigo-50 to-indigo-100'
  },
  {
    id: 8,
    title: 'Settings',
    description: 'System Configuration & \nPreferences',
    icon: Settings,
    color: 'from-gray-500 to-gray-600',
    gradient: 'bg-gradient-to-br from-gray-50 to-gray-100'
  }
];

export default function App() {
  const [hoveredCard, setHoveredCard] = useState<number | null>(null);
  const [currentPage, setCurrentPage] = useState<Page>('home');

  const handleCardClick = (cardId: number) => {
    if (cardId === 2) {
      setCurrentPage('report-reception');
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100">
      {/* Header Section */}
      <div className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-8 py-8">
          <div className="text-center">
            <h1 className="text-slate-900 mb-2">E-Call Assistant</h1>
            <p className="text-slate-600">Emergency Call Analysis and Management System</p>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        {currentPage === 'home' ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {menuCards.map((card, index) => {
              const Icon = card.icon;
              return (
                <motion.div
                  key={card.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.4, delay: index * 0.05 }}
                  onMouseEnter={() => setHoveredCard(card.id)}
                  onMouseLeave={() => setHoveredCard(null)}
                  onClick={() => handleCardClick(card.id)}
                >
                  <div
                    className={`
                      relative h-full bg-white rounded-xl shadow-sm border border-slate-200
                      transition-all duration-300 cursor-pointer overflow-hidden
                      ${hoveredCard === card.id ? 'shadow-xl -translate-y-1 border-slate-300' : ''}
                    `}
                  >
                    {/* Gradient Background on Hover */}
                    <div
                      className={`
                        absolute inset-0 ${card.gradient} opacity-0 transition-opacity duration-300
                        ${hoveredCard === card.id ? 'opacity-100' : ''}
                      `}
                    />

                    {/* Content */}
                    <div className="relative p-6 flex flex-col h-full">
                      {/* Title and Icon on same line */}
                      <div className="mb-6 flex justify-between items-center">
                        <h3 className="text-slate-900 font-bold">{card.title}</h3>
                        
                        <div
                          className={`
                            flex-shrink-0 p-3 rounded-xl bg-gradient-to-br ${card.color}
                            transition-all duration-300
                            ${hoveredCard === card.id ? 'scale-110 shadow-lg' : 'shadow-md'}
                          `}
                        >
                          <Icon className="w-6 h-6 text-white" />
                        </div>
                      </div>

                      {/* Description */}
                      <p className="text-slate-600 flex-grow leading-relaxed mt-2 whitespace-pre-line">
                        {card.description}
                      </p>

                      {/* Footer - Start button */}
                      <div className="mt-6 pt-4 border-t border-slate-100">
                        <div className="flex items-center justify-between">
                          <span className="text-slate-500">Get Started</span>
                          <ChevronRight 
                            className={`
                              w-4 h-4 text-slate-400 transition-transform duration-300
                              ${hoveredCard === card.id ? 'translate-x-1' : ''}
                            `}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </motion.div>
              );
            })}
          </div>
        ) : null}
      </div>

      {currentPage === 'report-reception' && <ReportReception />}

      {/* Footer */}
      <div className="text-center py-8 text-slate-500">
        <p className="mb-1">© 2025 E-Call Assistant. All rights reserved.</p>
        <p>Emergency Call Analysis System POC</p>
      </div>
    </div>
  );
}
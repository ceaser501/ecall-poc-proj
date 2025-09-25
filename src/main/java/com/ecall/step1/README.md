# Step 1: 음성 → 텍스트 변환

## 담당자: 김태수

### 📁 패키지 구조
- **s1speechrecognition** (1-1): 음성인식 및 발화자 분리 ✅ 구현완료
- **s2textconversion** (1-2): 음성 텍스트 변환 (기록화/구두점/시간정렬)
- **s3textcorrection** (1-3): 텍스트 문장 교정

### 🔧 사용 기술
- Azure Speech Services (실시간 STT)
- Speaker Recognition API
- WebSocket 통신

### 📋 현재 상태
- 1-1 음성인식 및 발화자 분리: **구현 완료**
- 1-2 음성 텍스트 변환: 개발 예정
- 1-3 텍스트 문장 교정: 개발 예정

### 🔗 API Endpoints
- `/api/voice/upload` - 음성 파일 업로드
- `/ws/voice` - 실시간 음성 스트리밍
- `/api/voice/stream` - 스트리밍 업로드
# Step 4: 데이터 저장 및 시각화

## 📋 개요
처리된 모든 데이터를 안전하게 저장하고, 실시간 대시보드를 통해 현황을 시각화하는 단계입니다. 신고 처리 현황, 위험도 분포, 리소스 사용량 등을 한눈에 파악할 수 있도록 지원합니다.

## 🚀 시작하기

### 1. 프로젝트 클론
```bash
# 프로젝트 저장소 복제
git clone https://github.com/ceaser501/ecall-poc-proj.git
cd ecall-poc-proj

# 작업 브랜치 생성
git checkout -b feature/step4-data-storage
```

### 2. 환경 설정

#### 필수 도구 설치
- **Java 17** 이상
- **IntelliJ IDEA** 또는 VS Code
- **Git**
- **Gradle** (프로젝트에 포함된 wrapper 사용 가능)
- **Node.js** (대시보드 개발용)

#### 데이터베이스 및 스토리지 설정
프로젝트 루트에 `.env` 파일 생성 또는 기존 파일에 추가:
```env
# Supabase (PostgreSQL + Storage)
SUPABASE_URL=your_project_url_here
SUPABASE_ANON_KEY=your_anon_key_here
SUPABASE_SERVICE_KEY=your_service_key_here

# Azure Blob Storage (선택)
AZURE_STORAGE_CONNECTION_STRING=your_connection_string
AZURE_STORAGE_CONTAINER_NAME=ecall-audio-files

# Redis (캐싱용, 선택)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_password_here
```

### 3. 데이터베이스 초기화
```bash
# Supabase CLI 설치 (Mac)
brew install supabase/tap/supabase

# 로컬 Supabase 시작
supabase start

# 테이블 생성 (SQL 스크립트 실행)
supabase db push
```

### 4. 애플리케이션 실행
```bash
# 백엔드 실행
./gradlew bootRun

# 프론트엔드 개발 서버 (별도 터미널)
cd src/main/resources/static
npm install
npm run dev
```

### 5. 대시보드 접속
브라우저에서 다음 URL 접속:
- 메인 대시보드: http://localhost:8082/pages/dashboard/dashboard.html
- 관리자 대시보드: http://localhost:8082/pages/admin/admin-dashboard.html

## 📁 프로젝트 구조

### 백엔드 구조
```
src/main/java/com/ecall/step4/
├── s1datastorage/           # 4-1. 텍스트 데이터 저장
│   ├── controller/
│   │   └── StorageController.java    # 저장 API
│   ├── service/
│   │   ├── TextStorageService.java   # 텍스트 DB 저장
│   │   ├── BlobStorageService.java   # 음성파일 저장
│   │   └── SecretStorageService.java # 보안키 관리
│   ├── repository/
│   │   └── CallRecordRepository.java # DB 접근
│   └── model/
│       ├── CallRecord.java           # 통화 기록 모델
│       └── AudioFile.java            # 음성 파일 메타데이터
└── s2datavisualization/     # 4-2. 텍스트 데이터 시각화
    ├── controller/
    │   ├── DashboardController.java  # 대시보드 API
    │   └── StatsController.java      # 통계 API
    ├── service/
    │   ├── StatisticsService.java    # 통계 계산
    │   └── RealtimeService.java      # 실시간 데이터
    └── model/
        ├── DashboardData.java        # 대시보드 데이터
        └── Statistics.java           # 통계 모델
```

### 프론트엔드 구조
```
src/main/resources/static/
├── pages/
│   ├── dashboard/           # 메인 대시보드
│   │   ├── dashboard.html
│   │   └── dashboard.js
│   └── admin/               # 관리자 대시보드
│       ├── admin-dashboard.html
│       └── admin-dashboard.js
├── assets/
│   ├── css/
│   │   └── dashboard.css
│   └── js/
│       ├── charts.js        # 차트 라이브러리
│       └── websocket.js     # 실시간 통신
└── components/              # 재사용 컴포넌트
    ├── charts/
    └── widgets/
```

### 데이터베이스 스키마
```sql
-- 통화 기록 테이블
CREATE TABLE call_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_id VARCHAR(50) UNIQUE NOT NULL,
    caller_number VARCHAR(20),
    received_at TIMESTAMP NOT NULL,
    duration_seconds INTEGER,
    transcript_text TEXT,
    summary TEXT,
    situation_type VARCHAR(50),
    urgency_level INTEGER,
    emotions JSONB,
    location JSONB,
    manual_used TEXT[],
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 음성 파일 메타데이터
CREATE TABLE audio_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_id VARCHAR(50) REFERENCES call_records(call_id),
    file_name VARCHAR(255),
    file_size BIGINT,
    file_url TEXT,
    mime_type VARCHAR(50),
    duration_seconds INTEGER,
    uploaded_at TIMESTAMP DEFAULT NOW()
);

-- 통계 테이블
CREATE TABLE statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    total_calls INTEGER,
    emergency_calls INTEGER,
    avg_duration_seconds INTEGER,
    situation_breakdown JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## 🔧 구현 내용

### 4-1. 텍스트 데이터 저장 (김태수)

#### 주요 기능
- **데이터 저장**
  - 텍스트 요약 DB 저장
  - 음성파일 Blob Storage 저장
  - 메타데이터 관리
  - 보안 키 안전한 저장

#### 구현 예정 내용
- Supabase PostgreSQL 연동
- Blob Storage 파일 업로드/다운로드
- 트랜잭션 처리
- 데이터 백업 전략

#### API 엔드포인트
- `POST /api/storage/save` - 통화 기록 저장
- `POST /api/storage/audio` - 음성 파일 업로드
- `GET /api/storage/record/{id}` - 기록 조회
- `GET /api/storage/audio/{id}` - 음성 파일 다운로드

#### 입력/출력 예시
```json
// 통화 기록 저장 요청
{
  "callId": "CALL-20240920-001",
  "callerNumber": "010-****-5678",
  "transcript": "화재가 발생했습니다...",
  "summary": "강남구 아파트 화재 신고",
  "situationType": "FIRE",
  "urgencyLevel": 9,
  "emotions": {
    "fear": 0.8,
    "panic": 0.7
  },
  "location": {
    "address": "서울시 강남구",
    "coordinates": {
      "lat": 37.5006,
      "lng": 127.0364
    }
  }
}

// 응답
{
  "success": true,
  "recordId": "uuid-here",
  "message": "저장 완료"
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 김태수 담당)

### 4-2. 텍스트 데이터 시각화 (김태수, 손장원, 전선민, 임송은)

#### 주요 기능
- **사용자 대시보드**
  - 실시간 신고 현황
  - 처리 건수 통계
  - 위험도/응급도 분포
  - 지역별 현황 지도

- **관리자 대시보드**
  - 시스템 리소스 사용량
  - API 호출 통계
  - 에러 로그 모니터링
  - 비용 추적

#### 구현 예정 내용
- Chart.js를 이용한 차트 구현
- WebSocket 실시간 업데이트
- 반응형 대시보드 UI
- 데이터 필터링/검색

#### API 엔드포인트
- `GET /api/dashboard/stats` - 전체 통계
- `GET /api/dashboard/realtime` - 실시간 데이터
- `GET /api/dashboard/charts` - 차트 데이터
- `WS /ws/dashboard` - 실시간 WebSocket

#### 대시보드 구성 요소
```javascript
// 실시간 통계 위젯
{
  "totalCalls": 1234,
  "todayCalls": 56,
  "activeCalls": 3,
  "avgResponseTime": "2.5초",
  "emergencyRate": "15%"
}

// 차트 데이터
{
  "hourlyDistribution": [...],
  "situationTypes": {
    "FIRE": 120,
    "ACCIDENT": 89,
    "MEDICAL": 156,
    "CRIME": 45
  },
  "urgencyLevels": {
    "IMMEDIATE": 45,
    "URGENT": 123,
    "NORMAL": 234
  }
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 전체 팀 참여)

## 🔍 테스트 방법

### 1. 데이터 저장 테스트
```bash
# 통화 기록 저장
curl -X POST http://localhost:8082/api/storage/save \
  -H "Content-Type: application/json" \
  -d '{
    "callId": "TEST-001",
    "transcript": "테스트 통화 내용",
    "situationType": "TEST"
  }'

# 음성 파일 업로드
curl -X POST http://localhost:8082/api/storage/audio \
  -F "file=@test_audio.mp3" \
  -F "callId=TEST-001"
```

### 2. 대시보드 테스트
1. http://localhost:8082/pages/dashboard/dashboard.html 접속
2. 실시간 데이터 업데이트 확인
3. 차트 인터랙션 테스트
4. 필터링 기능 테스트

### 3. 성능 테스트
```bash
# 부하 테스트 (Apache Bench)
ab -n 1000 -c 10 http://localhost:8082/api/dashboard/stats

# 메모리 사용량 모니터링
jconsole
```

## 📝 개발 시 주의사항

### 데이터 보안
- 개인정보 암호화 저장
- 음성 파일 접근 권한 관리
- API 키 환경변수 관리
- GDPR/개인정보보호법 준수

### 성능 최적화
- 대시보드 데이터 캐싱
- 쿼리 최적화
- 인덱스 설정
- 페이지네이션 구현

### 시각화 품질
- 색각 이상 고려 (색맹 친화적)
- 반응형 디자인
- 다크모드 지원
- 접근성 표준 준수

## 🤝 협업 방법

### Git 브랜치 전략
```bash
# 기능별 브랜치
git checkout -b feature/step4-storage
git checkout -b feature/step4-dashboard
git checkout -b feature/step4-charts

# 개발 완료 후
git add .
git commit -m "feat: 대시보드 차트 구현"
git push origin feature/step4-charts

# Pull Request 생성
```

### 작업 분담
- **김태수**: 데이터 저장 아키텍처, 대시보드 백엔드
- **손장원**: 통계 계산 로직, 차트 구현
- **전선민**: 실시간 데이터 처리, WebSocket
- **임송은**: UI/UX 디자인, 프론트엔드 구현

## 🆘 문제 해결

### Supabase 연결 오류
- API 키 확인
- 프로젝트 URL 확인
- 네트워크 설정 확인

### 차트 렌더링 문제
- 브라우저 콘솔 에러 확인
- Chart.js 버전 호환성
- 데이터 형식 검증

### 실시간 업데이트 안 됨
- WebSocket 연결 상태 확인
- 방화벽 설정
- 브라우저 개발자 도구 네트워크 탭 확인

## 📞 담당자
- **김태수**: Step 4 주요 담당
  - 4-1. 데이터 저장
  - 4-2. 대시보드 아키텍처
- **손장원**: 4-2. 시각화 (통계/차트)
- **전선민**: 4-2. 시각화 (실시간)
- **임송은**: 4-2. 시각화 (UI/UX)
- 기술 문의: 팀 Slack 채널 #step4-dashboard

## 📚 참고 자료
- [Supabase Documentation](https://supabase.com/docs)
- [Chart.js Documentation](https://www.chartjs.org/docs/)
- [WebSocket API](https://developer.mozilla.org/docs/Web/API/WebSocket)
- [PostgreSQL JSON Functions](https://www.postgresql.org/docs/current/functions-json.html)
- [Azure Blob Storage](https://docs.microsoft.com/azure/storage/blobs/)
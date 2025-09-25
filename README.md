# E-CALL — 실시간 음성 분석 기반 긴급신고전화 대응 시스템

신고자와 상황실 근무자의 통화를 실시간으로 문자화·분석하고, 상황별 질문·대응 매뉴얼을 자동 추천하는 PoC 프로젝트입니다. 통화 중 주요 내용을 사람이 직접 입력하느라 생기는 부담을 줄이고, 더 침착하고 정확한 접수를 돕습니다.

---

## 🔎 개요(Overview)
- **문제**: 긴급전화(112/119 등) 접수는 통화와 기록을 동시에 해야 해 놓침·지연이 발생하기 쉽고, 사건 유형이 다양해 표준 매뉴얼을 즉시 적용하기 어렵습니다.
- **해결**: 음성을 실시간으로 텍스트화(STT)하고 요약/분류/위험도 추정 → 질문 템플릿과 대응 매뉴얼을 추천, 부족 정보 알림 및 위치·주소 추출까지 지원.
- **효과**: 접수 요원의 인지 부하 감소, 응대 일관성 향상, 초기 대응 품질 개선.

---

## 🎯 목표(Goals)
1. 통화 음성 → 실시간 텍스트 변환 및 화자 분리
2. 텍스트 정제/요약/감정·위험도 분석 자동화
3. 대응 매뉴얼 추천(절차화된 단계 제시) 및 질문 가이드
4. 주소·장소 추출 및 좌표 변환(지오코딩)
5. 케이스 데이터 저장/조회 및 간단 대시보드(현황·건수·위험도)

---

## 📋 프로젝트 구조

### 백엔드 패키지 구조
```
com.ecall/
├── step1/                    # 음성 → 텍스트 변환
│   ├── s1speechrecognition/  # 1-1. 음성인식 및 발화자 분리
│   ├── s2textconversion/     # 1-2. 음성 텍스트 변환
│   └── s3textcorrection/     # 1-3. 텍스트 문장 교정
├── step2/                    # 텍스트 요약
│   ├── s1emotionanalysis/    # 2-1. 발화자 감정분석
│   └── s2locationextraction/ # 2-2. 발화자 장소/주소 추출
├── step3/                    # 텍스트 분석
│   ├── s1situationanalysis/  # 3-1. 상황분석 및 위험/응급 레벨
│   ├── s2datacollection/     # 3-2. 기존 데이터 수집 (RAG)
│   └── s3manualrecommend/    # 3-3. 대응 매뉴얼 추천
├── step4/                    # 데이터 저장 및 시각화
│   ├── s1datastorage/        # 4-1. 텍스트 데이터 저장
│   └── s2datavisualization/  # 4-2. 텍스트 데이터 시각화
└── common/                   # 공통 모듈
```

### 프론트엔드 구조
```
static/
├── pages/                    # 페이지별 HTML
│   ├── dashboard/            # 메인 대시보드
│   │   └── dashboard.html    # 통합 대시보드
│   └── voice/                # 음성 관련 페이지
│       ├── voice-test.html   # 실시간 음성 테스트
│       ├── voice-upload.html # 파일 업로드
│       └── voice-combined.html # 통합 테스트
├── assets/                   # 공통 리소스
│   ├── css/                  # 스타일시트
│   ├── js/                   # JavaScript
│   └── images/              # 이미지 리소스
└── modules/                 # 단계별 모듈 리소스
    ├── step1/               # 음성→텍스트 UI
    ├── step2/               # 텍스트 요약 UI
    ├── step3/               # 텍스트 분석 UI
    └── step4/               # 시각화 UI
```

**💡 패키지 네이밍 규칙**: `s1`, `s2`, `s3` 접두어로 매뉴얼 순서와 동일하게 정렬

---

## 👥 담당자

### Step 1: 음성 → 텍스트 변환
- **1-1. 음성인식 및 발화자 분리**: 전선민
- **1-2. 음성 텍스트 변환 (기록화/구두점/시간정렬)**: 전선민
- **1-3. 텍스트 문장 교정**: 전선민

### Step 2: 텍스트 요약
- **2-1. 발화자 감정분석**: 임송은
- **2-2. 발화자 장소/주소 추출**: 임송은

### Step 3: 텍스트 분석
- **3-1. 상황분석 및 위험/응급 레벨 설정**: 손장원
- **3-2. 기존 데이터 수집 (RAG)**: 손장원
- **3-3. 대응 매뉴얼 추천**: 김태수, 손장원

### Step 4: 데이터 저장 및 시각화
- **4-1. 텍스트 데이터 저장**: 김태수
- **4-2. 텍스트 데이터 시각화**: 김태수, 손장원, 전선민, 임송은

### Step 5: 마무리
- **리소스 정리 & 산출물 정리**: 김태수, 손장원, 전선민, 임송은
- **발표 자료 준비**:
- **최종 발표**: 임송은

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.4.0
- **Language**: Java 17
- **Build Tool**: Gradle 8.x
- **WebSocket**: Spring WebSocket

### Frontend
- **HTML5 / CSS3 / JavaScript (ES6+)**
- **WebSocket Client API**
- **Web Audio API**

### Database & Storage
- **Database**: Supabase (PostgreSQL)
- **Blob Storage**: Supabase Storage

### DevOps
- **Version Control**: Git
- **CI/CD**: (구축 예정)

### API
- **Azure Cognitive Services**
  - Azure Speech Services (Speech-to-Text)
  - Speaker Recognition API
  - Azure OpenAI Service (GPT 모델)

- **지도/위치 API** (선택 예정)
  - Azure Maps
  - Kakao Maps API
  - Daum 주소 API
  - Tmap API

### SDK & Libraries
- **Microsoft Cognitive Services Speech SDK 1.38.0**
  - 실시간 음성 인식
  - 발화자 분리/식별
  - 자동 구두점 생성

- **Azure OpenAI SDK** (도입 예정)
  - 텍스트 보정 및 교정
  - 문맥 분석
  - 요약 생성

### AI/ML 기술
- **Speech Recognition**
  - 실시간 스트리밍 STT
  - 발화자 인식 및 분리
  - 다중 화자 지원

- **Natural Language Processing**
  - LLM (GPT) 후처리
  - 감정 분석
  - 위치/주소 추출
  - 텍스트 요약

- **RAG (Retrieval-Augmented Generation)**
  - 문서 임베딩
  - 벡터 DB 검색
  - 매뉴얼 추천 시스템

---

### 보안
- **Environment Variables**: .env 파일
- **API Key Management**: Azure Key Vault (예정)
- **HTTPS/WSS**: 보안 통신

---

## 📦 설정 방법

### 1. 환경 변수 설정
`.env` 파일에 Azure 인증 정보 추가:
```
AZURE_SPEECH_SUBSCRIPTION_KEY=your_key_here
AZURE_SPEECH_REGION=koreacentral
AZURE_SPEECH_ENDPOINT=https://koreacentral.api.cognitive.microsoft.com/
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. 접속 URL
- 메인 대시보드: http://localhost:8082
- 음성 인식 테스트: http://localhost:8082/pages/voice/voice-combined.html
- API 헬스체크: http://localhost:8082/api/health

---

## 🔗 API Endpoints
- `/api/health` - 헬스체크
- `/api/modules` - 모듈 상태 확인
- `/api/voice/upload` - 음성 파일 업로드
- `/api/voice/stream` - 스트리밍 업로드
- `/ws/voice` - WebSocket 실시간 음성 스트리밍
- `/api/emotion` - 감정 분석
- `/api/location` - 위치 정보 추출
- `/api/text` - 텍스트 처리

---

## 🔄 브랜치 & 협업
- **브랜치 전략**: feature/* → PR → main
- **발표용**: main pull → 로컬 실행 후 화면 캡처로 결과 정리
- **배포 전환(선택)**:
  - 프런트: GitHub Pages 또는 Azure Static Web Apps
  - 백엔드: Azure App Service (Actions로 자동 배포)
  - DB: Supabase 그대로 사용

---

## 🔐 보안 & 데이터
- 키/비번은 환경변수/Secrets로 관리(.env 커밋 금지)
- 오디오는 가급적 압축 포맷(예: AAC/MP3 96–128kbps) 사용
- PoC 기간 이후 비식별화/보관주기 정책 적용 예정

---

## 📅 프로젝트 일정
- 프로젝트 시작: 2024년 9월
- 중간 점검: 진행 중
- 최종 발표: 2024년 12월 18일

---

## 📜 라이선스 / 주의
- 본 프로젝트는 학술/교육용 PoC입니다. 실제 긴급신고 체계에 적용하려면 보안·개인정보·안정성 검증이 필요합니다.
- 사용된 상표/서비스명은 각 소유자의 자산입니다.
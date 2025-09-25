# Step 1: 음성 → 텍스트 변환

## 📋 개요
프로젝트 주제 선정 및 프로세스 검토 단계입니다. 실시간 음성을 텍스트로 변환하고 화자를 구분하는 기능을 구현합니다.

## 🚀 시작하기

### 1. 프로젝트 클론
```bash
# 프로젝트 저장소 복제
git clone https://github.com/ceaser501/ecall-poc-proj.git
cd ecall-poc-proj

# 작업 브랜치 생성
git checkout -b feature/step1-voice-to-text
```

### 2. 환경 설정

#### 필수 도구 설치
- **Java 17** 이상
- **IntelliJ IDEA** 또는 VS Code
- **Git**
- **Gradle** (프로젝트에 포함된 wrapper 사용 가능)

#### Azure 계정 및 API 키 설정
1. [Azure Portal](https://portal.azure.com) 접속
2. Cognitive Services > Speech Services 생성
3. 키와 엔드포인트 확인

#### 환경변수 파일 생성 (.env)
프로젝트 루트에 `.env` 파일 생성:
```env
AZURE_SPEECH_SUBSCRIPTION_KEY=your_key_here
AZURE_SPEECH_REGION=koreacentral
AZURE_SPEECH_ENDPOINT=https://koreacentral.api.cognitive.microsoft.com/
```

### 3. 애플리케이션 실행
```bash
# Gradle로 실행
./gradlew bootRun

# 또는 IntelliJ에서 직접 실행
# EcallApplication.java 파일 우클릭 > Run
```

### 4. 테스트 페이지 접속
브라우저에서 다음 URL 접속:
- http://localhost:8082/pages/voice/voice-combined.html

## 📁 프로젝트 구조

### 백엔드 구조
```
src/main/java/com/ecall/step1/
├── s1speechrecognition/     # 1-1. 음성인식 및 발화자 분리
│   ├── config/
│   │   ├── AzureSpeechConfig.java    # Azure Speech SDK 설정
│   │   └── WebSocketConfig.java      # WebSocket 설정
│   ├── controller/
│   │   ├── VoiceStreamController.java # 실시간 스트리밍
│   │   └── FileUploadController.java  # 파일 업로드
│   ├── service/
│   │   └── VoiceRecognitionService.java # 음성 인식 서비스
│   └── model/
│       └── RecognitionResult.java     # 인식 결과 모델
├── s2textconversion/        # 1-2. 음성 텍스트 변환
│   ├── controller/
│   └── service/
└── s3textcorrection/        # 1-3. 텍스트 문장 교정
    ├── controller/
    └── service/
```

### 프론트엔드 구조
```
src/main/resources/static/
├── pages/voice/             # Step 1 관련 페이지
│   ├── voice-combined.html  # 통합 테스트 페이지
│   ├── voice-test.html      # 실시간 음성 테스트
│   └── voice-upload.html    # 파일 업로드 테스트
└── assets/                  # 공통 리소스
    ├── css/
    └── js/
```

## 🔧 구현 내용

### 1-1. 음성인식 및 발화자 분리 (전선민)

#### 주요 기능
- **실시간 음성 스트리밍 STT**
  - WebSocket을 통한 실시간 음성 데이터 전송
  - Azure Speech SDK를 사용한 음성 인식
  - 발화자 인식 및 구분

#### API 엔드포인트
- `GET /ws/voice` - WebSocket 연결
- `POST /api/voice/upload` - 음성 파일 업로드
- `POST /api/voice/stream` - 스트리밍 업로드

#### 구현 상태: ✅ 완료

### 1-2. 음성 텍스트 변환 (전선민)

#### 주요 기능
- **기록화/구두점/시간정렬**
  - 음성을 텍스트로 실시간 변환
  - 자동 구두점 생성
  - 타임스탬프 동기화
  - 발화 시간 정렬

#### 구현 예정 내용
- AI Speech 자동 구두점
- LLM(GPT) 후처리
- 문장부호 검증, 띄어쓰기 불완전, 맞춤/오타 보정 처리
- 발화 스타일 → 보고서체로 형식 변환 (연말 브리핑용으로 활용 가능)

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 9.21)

### 1-3. 텍스트 문장 교정 (전선민)

#### 주요 기능
- **LLM(Azure OpenAI) 후처리**
  - 문장 교정/요약
  - 맞춤법 검사
  - 문맥 기반 오류 수정

#### 구현 예정 내용
- Azure OpenAI 연동
- 텍스트 정제 및 교정
- 요약 생성

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 9.25)

## 🔍 테스트 방법

### 1. 실시간 음성 인식 테스트
1. http://localhost:8082/pages/voice/voice-test.html 접속
2. "Start Recording" 버튼 클릭
3. 마이크에 대고 말하기
4. 실시간으로 텍스트 변환 확인

### 2. 음성 파일 업로드 테스트
1. http://localhost:8082/pages/voice/voice-upload.html 접속
2. 음성 파일 선택 (WAV, MP3 등)
3. 업로드 후 변환 결과 확인

### 3. 통합 테스트
1. http://localhost:8082/pages/voice/voice-combined.html 접속
2. 실시간 음성 + 파일 업로드 동시 테스트

## 📝 개발 시 주의사항

### Azure API 키 관리
- `.env` 파일은 절대 Git에 커밋하지 마세요
- API 키는 팀 리더에게 별도로 요청하세요

### 브라우저 권한
- 마이크 접근 권한이 필요합니다
- HTTPS 환경에서만 마이크 API가 작동합니다 (localhost는 예외)

### 파일 업로드 제한
- 최대 파일 크기: 10MB
- 지원 형식: WAV, MP3, M4A, OGG

## 🤝 협업 방법

### Git 브랜치 전략
```bash
# 기능 개발 시
git checkout -b feature/step1-기능명

# 개발 완료 후
git add .
git commit -m "feat: 기능 설명"
git push origin feature/step1-기능명

# GitHub에서 Pull Request 생성
```

### 커밋 메시지 규칙
- `feat:` 새로운 기능 추가
- `fix:` 버그 수정
- `docs:` 문서 수정
- `style:` 코드 포맷팅
- `refactor:` 코드 리팩토링
- `test:` 테스트 코드

## 🆘 문제 해결

### Azure API 연결 실패
- API 키와 지역이 올바른지 확인
- 방화벽 설정 확인
- Azure 구독 상태 확인

### WebSocket 연결 오류
- 포트 8082가 사용 중인지 확인
- 브라우저 개발자 도구에서 콘솔 에러 확인

### 음성 인식이 안 되는 경우
- 마이크 권한 확인
- 브라우저 설정에서 마이크 차단 여부 확인
- 다른 브라우저로 테스트 (Chrome 권장)

## 📞 담당자
- **전선민**: Step 1 전체 담당
- 기술 문의: 팀 Slack 채널 #step1-voice

## 📚 참고 자료
- [Azure Speech Services 문서](https://docs.microsoft.com/azure/cognitive-services/speech-service/)
- [Spring WebSocket 가이드](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Web Audio API](https://developer.mozilla.org/docs/Web/API/Web_Audio_API)
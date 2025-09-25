# 🤖 AI 바이브코딩 개발 가이드

> **중요**: 이 프로젝트는 협업 프로젝트입니다. 각자의 개발 영역을 침범하지 않도록 아래 규칙을 엄격히 준수해주세요.

## 📁 프로젝트 구조 및 영역 분리

### 1. 공통 영역 (Common)
**위치**: `src/main/java/com/ecall/common/`

- **용도**: 모든 개발자가 공용으로 사용하는 모듈
- **하위 구조**:
  - `config/` - 공통 설정 파일 (DB, API, Security 등)
  - `utils/` - 유틸리티 클래스
  - `enums/` - 공통 열거형
  - `exception/` - 공통 예외 클래스
  - `constants/` - 공통 상수
- **규칙**:
  - 새로운 API 연동 설정은 반드시 `common/config/`에 작성
  - 2개 이상 패키지에서 사용되는 코드만 common에 작성

### 2. 백엔드 개발 영역
**위치**: `src/main/java/com/ecall/step[1-4]/`

#### 📍 **절대 규칙**
- **패키지 구조 변경 금지**: `step1`, `step2`, `step3`, `step4` 및 하위 패키지 구조는 절대 변경하지 마세요
- **영역 침범 금지**: 자신이 담당한 패키지(`step1.s1speechrecognition` 등) 외부에는 파일을 생성하거나 수정하지 마세요
- **MVC 구조 준수**: 반드시 아래 4개 패키지에만 코드를 작성하세요
  - `controller/` - REST API 컨트롤러
  - `service/` - 비즈니스 로직
  - `model/` - 엔티티/도메인 객체
  - `dto/` - 데이터 전송 객체

#### 📝 네이밍 규칙
- **DTO**: 카멜케이스 필드명 (예: `userName`, `phoneNumber`)
- **클래스**: 파스칼케이스 + 접미사
  - Controller: `~Controller` (예: `VoiceRecognitionController`)
  - Service: `~Service` (예: `VoiceRecognitionService`)
  - DTO: `~Request`, `~Response`, `~Dto` (예: `VoiceRecognitionRequest`)
  - Model: 명사형 (예: `RecognitionResult`)

### 3. 프론트엔드 개발 영역
**위치**: `src/main/resources/static/`

#### 📁 디렉토리 구조
```
static/
├── pages/          # 각 개발자별 화면 디렉토리
│   ├── step1-speech/    # step1 담당자 화면
│   ├── step2-analysis/  # step2 담당자 화면
│   ├── step3-situation/ # step3 담당자 화면
│   └── step4-storage/   # step4 담당자 화면
└── assets/         # 공통 리소스 (기능별 분리 없음)
    ├── css/
    ├── js/
    ├── images/
    └── data/       # 테스트용 데이터
```

#### 📍 규칙
- 자신의 화면은 반드시 해당 디렉토리(`pages/step1-speech/` 등) 하위에만 작성
- CSS/JS/이미지는 `assets/` 하위에 작성 (파일명에 기능 prefix 추가 권장)

## 🔧 개발 규칙

### 4. 환경설정 및 보안
- **환경변수**: 모든 키, 비밀번호, API 엔드포인트는 `.env` 파일에 작성
- **의존성**: 새로운 라이브러리는 `build.gradle`의 `dependencies`에 추가
- **설정파일**: `application.properties` 또는 `application.yml` 수정 시 팀 공유

### 5. 코드 작성 규칙
- **로깅**: `@Slf4j` 어노테이션 사용, `log.info()`, `log.error()` 활용
- **예외처리**: 비즈니스 예외는 `common/exception/`의 커스텀 예외 사용
- **의존성 주입**: `@RequiredArgsConstructor` + `private final` 패턴 사용
- **API 응답**: 일관된 응답 형태를 위해 `ResponseEntity<>` 사용

### 6. 테스트 코드
**위치**: `src/test/java/com/ecall/[해당패키지]/`
- 자신의 개발 영역에 해당하는 테스트만 작성
- 테스트 클래스명: `~Test` (예: `VoiceRecognitionServiceTest`)

## 🚨 금지사항

### ❌ 절대 하지 마세요
1. **다른 개발자의 패키지 수정**: step1 담당자는 step2, step3, step4 패키지 수정 금지
2. **패키지 구조 변경**: 기존 step* 구조 변경 금지
3. **환경파일 커밋**: `.env` 파일을 git에 커밋하지 마세요
4. **공통 설정 독단 변경**: `common/` 패키지 수정 시 팀 논의 필수
5. **하드코딩**: API 키, 경로 등을 코드에 직접 작성 금지

## 📋 개발 시작 전 체크리스트

### ✅ 개발 시작 전 확인사항
- [ ] 담당 영역 패키지 경로 확인 (step1.s1speechrecognition 등)
- [ ] .env 파일에 필요한 환경변수 설정 확인
- [ ] build.gradle에 필요한 의존성 추가
- [ ] 기존 common 패키지의 유틸리티 확인 및 재사용

### ✅ 코드 작성 시 확인사항
- [ ] MVC 패턴에 맞는 패키지에 파일 생성 (controller/service/model/dto)
- [ ] 네이밍 규칙 준수 (카멜케이스, 클래스 접미사)
- [ ] 로깅 및 예외처리 추가
- [ ] 자신의 영역 외부 파일 수정 여부 확인

## 🎯 AI 바이브코딩 프롬프트

**개발 시작 시 AI에게 다음과 같이 안내하세요:**

```
이 프로젝트는 팀 협업 프로젝트입니다. 다음 규칙을 반드시 준수해주세요:

1. 나는 [step1/step2/step3/step4] 중 [구체적 패키지명] 담당입니다
2. 내 작업은 오직 해당 패키지 하위에서만 진행해주세요
3. MVC 구조에 따라 controller/service/model/dto 패키지에만 파일을 생성해주세요
4. API 설정이 필요하면 common/config/에 작성해주세요
5. 환경변수는 .env 파일에, 의존성은 build.gradle에 추가해주세요
6. 다른 패키지(step*)는 절대 수정하지 마세요

담당 영역: [예: src/main/java/com/ecall/step1/s1speechrecognition/]
개발 목표: [구체적인 기능 설명]
```

---

**📞 문의사항이나 패키지 간 연동이 필요한 경우 팀 공유 후 진행하세요!**
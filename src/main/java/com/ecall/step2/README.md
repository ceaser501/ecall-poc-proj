# Step 2: 텍스트 요약

## 📋 개요
음성에서 변환된 텍스트를 분석하여 감정을 파악하고 위치 정보를 추출하는 단계입니다. 긴급 상황에서 신고자의 감정 상태와 정확한 위치를 빠르게 파악할 수 있도록 지원합니다.

## 🚀 시작하기

### 1. 프로젝트 클론
```bash
# 프로젝트 저장소 복제
git clone https://github.com/ceaser501/ecall-poc-proj.git
cd ecall-poc-proj

# 작업 브랜치 생성
git checkout -b feature/step2-text-summary
```

### 2. 환경 설정

#### 필수 도구 설치
- **Java 17** 이상
- **IntelliJ IDEA** 또는 VS Code
- **Git**
- **Gradle** (프로젝트에 포함된 wrapper 사용 가능)

#### API 키 설정
프로젝트 루트에 `.env` 파일 생성 또는 기존 파일에 추가:
```env
# 감정 분석용 (Azure 또는 OpenAI)
AZURE_TEXT_ANALYTICS_KEY=your_key_here
AZURE_TEXT_ANALYTICS_ENDPOINT=your_endpoint_here

# 위치 추출용 (선택)
KAKAO_API_KEY=your_kakao_key_here
NAVER_CLIENT_ID=your_naver_id_here
NAVER_CLIENT_SECRET=your_naver_secret_here
AZURE_MAPS_KEY=your_maps_key_here
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
- 감정 분석: http://localhost:8082/pages/emotion/emotion-test.html
- 위치 추출: http://localhost:8082/pages/location/location-test.html

## 📁 프로젝트 구조

### 백엔드 구조
```
src/main/java/com/ecall/step2/
├── s1emotionanalysis/       # 2-1. 발화자 감정분석
│   ├── controller/
│   │   └── EmotionController.java    # 감정 분석 API
│   ├── service/
│   │   └── EmotionService.java        # 감정 분석 비즈니스 로직
│   └── model/
│       └── EmotionResult.java         # 감정 분석 결과 모델
└── s2locationextraction/    # 2-2. 발화자 장소/주소 추출
    ├── controller/
    │   └── LocationController.java    # 위치 추출 API
    ├── service/
    │   └── LocationService.java       # 위치 추출 비즈니스 로직
    └── model/
        └── LocationResult.java         # 위치 추출 결과 모델
```

### 프론트엔드 구조 (생성 예정)
```
src/main/resources/static/
├── pages/
│   ├── emotion/             # Step 2-1 관련 페이지
│   │   └── emotion-test.html
│   └── location/            # Step 2-2 관련 페이지
│       └── location-test.html
└── assets/
    ├── css/
    └── js/
```

## 🔧 구현 내용

### 2-1. 발화자 감정분석 (임송은)

#### 주요 기능
- **감정 분류**
  - 긴급 상황 감정 구분 (공포, 불안, 당황, 분노, 침착 등)
  - 감정 강도 측정 (1-10 스케일)
  - 감정 변화 추적 (시간별 변화)

#### 구현 예정 내용
- 발화자의 감정을 분석
- 감정 구분은 별도 정의 필요
- 위험도/긴급도 판단 지표 활용

#### API 엔드포인트
- `POST /api/emotion` - 텍스트 감정 분석
- `GET /api/emotion/{id}` - 분석 결과 조회

#### 입력/출력 예시
```json
// 입력
{
  "text": "도와주세요! 누군가 집에 들어왔어요. 너무 무서워요.",
  "speakerId": "caller-001"
}

// 출력
{
  "emotions": {
    "fear": 0.85,
    "anxiety": 0.72,
    "anger": 0.15
  },
  "dominantEmotion": "fear",
  "urgencyLevel": 9,
  "timestamp": "2024-09-20T10:30:00Z"
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 임송은 담당)

### 2-2. 발화자 장소/주소 추출 (임송은)

#### 주요 기능
- **위치 정보 추출**
  - 주소 텍스트 추출 (도로명, 지번 주소)
  - 랜드마크 인식 (건물명, 상호명)
  - GPS 좌표 변환 (지오코딩)

#### 구현 예정 내용
- 텍스트에서 주소/위치 정보 추출
- Azure Maps, Kakao Maps, Naver Maps API 활용
- 정확한 좌표 매핑

#### API 엔드포인트
- `POST /api/location` - 텍스트에서 위치 추출
- `POST /api/location/geocode` - 주소를 좌표로 변환

#### 입력/출력 예시
```json
// 입력
{
  "text": "여기는 서울시 강남구 테헤란로 152 강남파이낸스센터 앞이에요"
}

// 출력
{
  "addresses": [
    {
      "fullAddress": "서울시 강남구 테헤란로 152",
      "roadAddress": "서울시 강남구 테헤란로 152",
      "jibunAddress": "서울시 강남구 역삼동 737",
      "landmark": "강남파이낸스센터",
      "coordinates": {
        "latitude": 37.5006,
        "longitude": 127.0364
      }
    }
  ],
  "confidence": 0.95
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 임송은 담당)

## 🔍 테스트 방법

### 1. 감정 분석 테스트
```bash
# API 직접 호출
curl -X POST http://localhost:8082/api/emotion \
  -H "Content-Type: application/json" \
  -d '{
    "text": "도와주세요! 지금 너무 위험해요!",
    "speakerId": "test-001"
  }'
```

### 2. 위치 추출 테스트
```bash
# API 직접 호출
curl -X POST http://localhost:8082/api/location \
  -H "Content-Type: application/json" \
  -d '{
    "text": "서울역 3번 출구 앞에서 사고가 났어요"
  }'
```

### 3. 웹 페이지 테스트
1. 감정 분석 페이지에서 텍스트 입력
2. 분석 버튼 클릭
3. 결과 확인

## 📝 개발 시 주의사항

### API 키 관리
- 각 서비스별 API 키 관리
- 일일 호출 제한 확인
- 과금 정책 확인

### 성능 고려사항
- 대량 텍스트 처리 시 배치 처리
- 캐싱 전략 수립
- 응답 시간 최적화

### 정확도 개선
- 긴급 상황 특화 학습 데이터 구축
- 오탐지 사례 수집 및 개선
- 지역별 방언/속어 대응

## 🤝 협업 방법

### Git 브랜치 전략
```bash
# 기능 개발 시
git checkout -b feature/step2-emotion
git checkout -b feature/step2-location

# 개발 완료 후
git add .
git commit -m "feat: 감정 분석 기능 구현"
git push origin feature/step2-emotion

# GitHub에서 Pull Request 생성
```

### 테스트 데이터 공유
- `src/test/resources/sample-texts/` 폴더에 테스트용 텍스트 저장
- 실제 긴급 통화 시나리오 기반 데이터 작성

## 🆘 문제 해결

### API 응답 오류
- API 키 유효성 확인
- 요청 형식 확인
- Rate limit 확인

### 한글 인코딩 문제
- UTF-8 설정 확인
- application.yml 파일 인코딩 설정

### 위치 정확도 문제
- 여러 지도 API 교차 검증
- 주소 정제 로직 개선

## 📞 담당자
- **임송은**: Step 2 전체 담당
  - 2-1. 감정 분석
  - 2-2. 위치 추출
- 기술 문의: 팀 Slack 채널 #step2-analysis

## 📚 참고 자료
- [Azure Text Analytics API](https://docs.microsoft.com/azure/cognitive-services/text-analytics/)
- [Kakao 주소 검색 API](https://developers.kakao.com/docs/latest/ko/local/dev-guide)
- [Naver 지도 API](https://www.ncloud.com/product/applicationService/maps)
- [감정 분석 모델 논문](https://arxiv.org/abs/emotion-analysis)
# Step 3: 텍스트 분석

## 📋 개요
변환된 텍스트를 분석하여 상황을 파악하고, 기존 데이터를 활용한 RAG(Retrieval-Augmented Generation)로 적절한 대응 매뉴얼을 추천하는 단계입니다. 긴급 상황에 맞는 표준 대응 절차를 신속하게 제공합니다.

## 🚀 시작하기

### 1. 프로젝트 클론
```bash
# 프로젝트 저장소 복제
git clone https://github.com/ceaser501/ecall-poc-proj.git
cd ecall-poc-proj

# 작업 브랜치 생성
git checkout -b feature/step3-text-analysis
```

### 2. 환경 설정

#### 필수 도구 설치
- **Java 17** 이상
- **IntelliJ IDEA** 또는 VS Code
- **Git**
- **Gradle** (프로젝트에 포함된 wrapper 사용 가능)

#### API 키 및 데이터베이스 설정
프로젝트 루트에 `.env` 파일 생성 또는 기존 파일에 추가:
```env
# LLM API (Azure OpenAI 또는 OpenAI)
AZURE_OPENAI_KEY=your_key_here
AZURE_OPENAI_ENDPOINT=your_endpoint_here
AZURE_OPENAI_DEPLOYMENT=gpt-4

# 벡터 데이터베이스 (선택)
PINECONE_API_KEY=your_key_here
PINECONE_ENV=your_env_here
# 또는
QDRANT_URL=http://localhost:6333
QDRANT_API_KEY=your_key_here

# Supabase (문서 저장용)
SUPABASE_URL=your_url_here
SUPABASE_KEY=your_key_here
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
- 상황 분석: http://localhost:8082/pages/situation/analysis.html
- 매뉴얼 추천: http://localhost:8082/pages/manual/recommend.html

## 📁 프로젝트 구조

### 백엔드 구조
```
src/main/java/com/ecall/step3/
├── s1situationanalysis/     # 3-1. 상황분석 및 위험/응급 레벨
│   ├── controller/
│   │   └── SituationController.java  # 상황 분석 API
│   ├── service/
│   │   └── SituationService.java     # 상황 분석 로직
│   └── model/
│       ├── SituationResult.java      # 분석 결과
│       └── UrgencyLevel.java         # 긴급도 레벨
├── s2datacollection/        # 3-2. 기존 데이터 수집 (RAG)
│   ├── controller/
│   │   └── DataController.java       # 데이터 검색 API
│   ├── service/
│   │   ├── EmbeddingService.java     # 문서 임베딩
│   │   └── VectorSearchService.java  # 벡터 검색
│   └── model/
│       └── SearchResult.java         # 검색 결과
└── s3manualrecommend/       # 3-3. 대응 매뉴얼 추천
    ├── controller/
    │   └── ManualController.java     # 매뉴얼 추천 API
    ├── service/
    │   └── ManualService.java        # 매뉴얼 생성 로직
    └── model/
        └── ManualResponse.java       # 매뉴얼 응답
```

### 프론트엔드 구조 (생성 예정)
```
src/main/resources/static/
├── pages/
│   ├── situation/           # Step 3-1 관련 페이지
│   │   └── analysis.html
│   └── manual/              # Step 3-3 관련 페이지
│       └── recommend.html
└── assets/
    ├── css/
    └── js/
```

## 🔧 구현 내용

### 3-1. 상황분석 및 위험/응급 레벨 설정 (손장원)

#### 주요 기능
- **상황 분류**
  - 사건 유형 분류 (화재, 의료, 범죄, 사고 등)
  - 위험도 평가 (1-10 레벨)
  - 응급 레벨 설정 (즉시/긴급/일반)

#### 구현 예정 내용
- LLM을 활용한 상황 분석
- 키워드 기반 긴급도 판단
- 복합 상황 처리 로직

#### API 엔드포인트
- `POST /api/situation/analyze` - 상황 분석
- `GET /api/situation/level` - 긴급 레벨 조회

#### 입력/출력 예시
```json
// 입력
{
  "text": "불이 났어요! 2층 아파트인데 연기가 너무 많아요. 사람들이 갇혀있어요.",
  "emotions": ["fear", "panic"],
  "location": "서울시 강남구"
}

// 출력
{
  "situationType": "FIRE",
  "subType": "RESIDENTIAL_FIRE",
  "urgencyLevel": 9,
  "emergencyClass": "IMMEDIATE",
  "keyFactors": [
    "화재 발생",
    "다수 인명 위험",
    "연기 확산"
  ],
  "requiredResources": [
    "소방차 3대",
    "구급차 2대",
    "구조대"
  ]
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 손장원 담당)

### 3-2. 기존 데이터 수집 (RAG) (손장원)

#### 주요 기능
- **문서 임베딩 및 검색**
  - 매뉴얼/SOP 문서 벡터화
  - 유사 사례 검색
  - 관련 법규/규정 검색

#### 구현 예정 내용
- 문서 임베딩 파이프라인
- 벡터 DB 구축 (Pinecone/Qdrant)
- 검색 최적화 알고리즘

#### API 엔드포인트
- `POST /api/data/embed` - 문서 임베딩
- `POST /api/data/search` - 유사 문서 검색
- `GET /api/data/cases` - 유사 사례 조회

#### 데이터 구조 예시
```json
// 매뉴얼 문서
{
  "id": "manual-001",
  "title": "화재 대응 표준 절차",
  "content": "화재 신고 접수 시...",
  "category": "FIRE",
  "tags": ["화재", "대피", "초기진압"],
  "embedding": [0.1, 0.2, ...],  // 768차원 벡터
  "metadata": {
    "version": "2.0",
    "lastUpdated": "2024-01-15"
  }
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 손장원 담당)

### 3-3. 대응 매뉴얼 추천 (김태수, 손장원)

#### 주요 기능
- **매뉴얼 생성 및 추천**
  - 상황별 맞춤 매뉴얼 생성
  - 단계별 대응 절차 제시
  - 질문 템플릿 제공

#### 구현 예정 내용
- RAG 기반 매뉴얼 생성
- 기존 사례 관련 매뉴얼 추천
- 검색 결과를 바탕으로 응답 생성

#### API 엔드포인트
- `POST /api/manual/recommend` - 매뉴얼 추천
- `POST /api/manual/generate` - 맞춤 매뉴얼 생성
- `GET /api/manual/templates` - 질문 템플릿 조회

#### 입력/출력 예시
```json
// 입력
{
  "situationType": "FIRE",
  "urgencyLevel": 9,
  "keyFactors": ["화재", "인명피해"],
  "context": "2층 아파트 화재, 다수 갇힘"
}

// 출력
{
  "manualId": "gen-001",
  "title": "주택 화재 대응 매뉴얼",
  "steps": [
    {
      "order": 1,
      "action": "신고자 안전 확인",
      "questions": [
        "현재 안전한 곳에 계신가요?",
        "건물 밖으로 대피하셨나요?"
      ]
    },
    {
      "order": 2,
      "action": "인명 파악",
      "questions": [
        "건물 안에 몇 명이 있나요?",
        "어린이나 노약자가 있나요?"
      ]
    }
  ],
  "criticalActions": [
    "즉시 소방차 출동",
    "구급차 대기",
    "인근 병원 통보"
  ],
  "referenceManuals": [
    "SOP-FIRE-001",
    "CASE-2024-0123"
  ]
}
```

#### 구현 상태: ⏳ 개발 예정 (예상 완료: 김태수, 손장원 담당)

## 🔍 테스트 방법

### 1. 상황 분석 테스트
```bash
# API 직접 호출
curl -X POST http://localhost:8082/api/situation/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "text": "교통사고가 났어요. 차가 전복됐고 운전자가 의식이 없어요."
  }'
```

### 2. RAG 검색 테스트
```bash
# 유사 문서 검색
curl -X POST http://localhost:8082/api/data/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "교통사고 대응 절차",
    "top_k": 5
  }'
```

### 3. 매뉴얼 추천 테스트
```bash
# 매뉴얼 생성
curl -X POST http://localhost:8082/api/manual/generate \
  -H "Content-Type: application/json" \
  -d '{
    "situationType": "ACCIDENT",
    "context": "고속도로 다중 추돌 사고"
  }'
```

## 📝 개발 시 주의사항

### LLM 사용
- API 호출 비용 관리
- 응답 시간 최적화 (캐싱 활용)
- 프롬프트 엔지니어링

### 벡터 DB 관리
- 임베딩 모델 버전 관리
- 인덱스 최적화
- 정기적인 데이터 업데이트

### 매뉴얼 품질
- 법적 검토 필요
- 정기적인 매뉴얼 업데이트
- 현장 피드백 반영

## 🤝 협업 방법

### Git 브랜치 전략
```bash
# 기능 개발 시
git checkout -b feature/step3-situation
git checkout -b feature/step3-rag
git checkout -b feature/step3-manual

# 개발 완료 후
git add .
git commit -m "feat: RAG 검색 기능 구현"
git push origin feature/step3-rag

# GitHub에서 Pull Request 생성
```

### 데이터 준비
- `data/manuals/` - 표준 대응 매뉴얼
- `data/cases/` - 과거 사례 데이터
- `data/sop/` - 표준 운영 절차

## 🆘 문제 해결

### LLM API 오류
- API 키 확인
- Rate limit 확인
- 타임아웃 설정 조정

### 벡터 검색 성능
- 임베딩 차원 축소 고려
- 인덱스 타입 변경
- 검색 파라미터 조정

### 메모리 문제
- 배치 처리 크기 조정
- 캐시 크기 제한
- 가비지 컬렉션 튜닝

## 📞 담당자
- **손장원**: Step 3 주요 담당
  - 3-1. 상황 분석
  - 3-2. RAG 구현
  - 3-3. 매뉴얼 추천 (공동)
- **김태수**: 3-3. 매뉴얼 추천 (공동)
- 기술 문의: 팀 Slack 채널 #step3-analysis

## 📚 참고 자료
- [LangChain Java](https://github.com/langchain4j/langchain4j)
- [Pinecone Documentation](https://docs.pinecone.io/)
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [RAG 논문](https://arxiv.org/abs/2005.11401)
- [Emergency Response Protocols](https://www.fema.gov/)
# 텍스트 처리 모듈 (Text Processing Module)

## 담당자: 전선민

### 📁 작업 폴더
- `/src/main/java/com/ecall/text/` - 백엔드 코드
- `/src/main/resources/static/text/` - 프론트엔드 파일

### 🎯 담당 기능
1. 음성 → 텍스트 변환 (STT)
2. 텍스트 요약 (Azure OpenAI)
3. 문장 교정
4. 핵심 정보 추출

### 📝 작업 가이드

#### 1. 브랜치 생성
```bash
git checkout -b feature/seonmin
```

#### 2. 작업할 파일들
- `controller/TextController.java` - API 엔드포인트
- `service/TextService.java` - 비즈니스 로직
- `/static/text/` 폴더에 HTML/CSS/JS 추가

#### 3. 테스트 API
- GET `/api/text/test` - 모듈 상태 확인
- POST `/api/text/stt` - 음성→텍스트 변환
- POST `/api/text/summarize` - 텍스트 요약

#### 4. 커밋 & 푸시
```bash
git add src/main/java/com/ecall/text/
git commit -m "feat: 텍스트 처리 기능 구현"
git push origin feature/seonmin
```

### ⚠️ 주의사항
- **text 폴더만 수정하세요!**
- 다른 팀원 모듈 건드리지 마세요
- Azure API 키는 별도 전달

### 💡 도움 필요시
- 통합 담당자에게 문의
- Git 사용법 모르면 바로 질문!
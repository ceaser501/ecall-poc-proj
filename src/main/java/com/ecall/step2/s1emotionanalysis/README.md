# 감정 분석 모듈 (Emotion Analysis Module)

## 담당자: 임송은

### 📁 작업 폴더
- `/src/main/java/com/ecall/emotion/` - 백엔드 코드
- `/src/main/resources/static/emotion/` - 프론트엔드 파일

### 🎯 담당 기능
1. 발화자 감정 분석
2. 긴급도 판단
3. 심리 상태 파악
4. 감정 지표 시각화

### 📝 작업 가이드

#### 1. 브랜치 생성
```bash
git checkout -b feature/songeun-emotion
```

#### 2. 작업할 파일들
- `controller/EmotionController.java` - API 엔드포인트
- `service/EmotionService.java` - 비즈니스 로직
- `/static/emotion/` 폴더에 HTML/CSS/JS 추가

#### 3. 테스트 API
- GET `/api/emotion/test` - 모듈 상태 확인
- POST `/api/emotion/analyze` - 감정 분석 처리

#### 4. 커밋 & 푸시
```bash
git add src/main/java/com/ecall/emotion/
git commit -m "feat: 감정 분석 기능 구현"
git push origin feature/songeun-emotion
```

### ⚠️ 주의사항
- **emotion 폴더만 수정하세요!**
- 다른 모듈 건드리지 마세요
- location 모듈도 작업하신다면 별도 커밋

### 💡 도움 필요시
- 통합 담당자에게 문의
- Slack/카톡으로 연락
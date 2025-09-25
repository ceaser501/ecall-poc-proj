# 음성인식 모듈 (Speech Recognition Module)

## 담당자: 김태수

### 📁 작업 폴더
- `/src/main/java/com/ecall/speech/` - 백엔드 코드
- `/src/main/resources/static/speech/` - 프론트엔드 파일

### 🎯 담당 기능
1. 발화자 인식 및 분리
2. AI Speech 자동 구두점 처리
3. Speaker Recognition API 연동
4. 음성 파일 처리

### 📝 작업 가이드

#### 1. 브랜치 생성
```bash
git checkout -b feature/taesu
```

#### 2. 작업할 파일들
- `controller/SpeechController.java` - API 엔드포인트
- `service/SpeechService.java` - 비즈니스 로직
- `/static/speech/` 폴더에 HTML/CSS/JS 추가

#### 3. 테스트 API
- GET `/api/speech/test` - 모듈 상태 확인
- POST `/api/speech/recognize` - 음성인식 처리

#### 4. 커밋 & 푸시
```bash
git add src/main/java/com/ecall/speech/
git commit -m "feat: 음성인식 기능 구현"
git push origin feature/taesu
```

### ⚠️ 주의사항
- **다른 모듈 건드리지 마세요!**
- speech 폴더 내에서만 작업
- 충돌 걱정 없음

### 💡 도움 필요시
- 통합 담당자에게 문의
- 이 README 파일 참고
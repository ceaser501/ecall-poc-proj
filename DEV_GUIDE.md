# 팀 개발 가이드

## 프로젝트 구조
- **백엔드**: Spring Boot (내장 Tomcat)
- **프론트엔드**: HTML/CSS/JavaScript (src/main/resources/static/)
- **포트**: 8082

## 개발 환경 설정

### 1. 프로젝트 클론
```bash
git clone https://github.com/ceaser501/ecall-final-web.git
cd ecall-final-web
```

### 2. 서버 실행
```bash
./gradlew bootRun
```
또는
```bash
./gradlew clean build
java -jar build/libs/ecall-poc-proj-0.0.1-SNAPSHOT.jar
```

### 3. 접속 확인
- 웹페이지: http://localhost:8082
- API 테스트: http://localhost:8082/api/health

## 개발 작업 영역

### 프론트엔드 개발
- 위치: `src/main/resources/static/`
- HTML/CSS/JS 파일 직접 수정
- 서버 재시작 없이 브라우저 새로고침으로 확인 가능

### 백엔드 개발
- 위치: `src/main/java/com/ecall/web/`
- Controller, Service, Repository 등 추가
- 변경 후 서버 재시작 필요

## Git 협업 플로우

### 1. 작업 시작 전
```bash
git pull origin main
```

### 2. 브랜치 생성
```bash
git checkout -b feature/[이름]/[기능명]
# 예: git checkout -b feature/taesu/login-api
```

### 3. 작업 후 커밋
```bash
git add .
git commit -m "feat: 로그인 API 구현"
git push origin feature/[이름]/[기능명]
```

### 4. Pull Request 생성
- GitHub에서 PR 생성
- 팀원 리뷰 후 main 브랜치에 머지

## 주요 디렉토리 구조
```
ecall-final-web/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecall/web/
│   │   │       ├── controller/  # REST API 컨트롤러
│   │   │       ├── service/     # 비즈니스 로직
│   │   │       └── model/       # 데이터 모델
│   │   └── resources/
│   │       ├── static/          # 프론트엔드 파일 (HTML/CSS/JS)
│   │       │   ├── index.html
│   │       │   ├── css/
│   │       │   └── js/
│   │       └── application.yml  # Spring Boot 설정
└── build.gradle                  # 의존성 관리

```

## 최종 발표 준비
1. main 브랜치 최신 버전 pull
2. `./gradlew bootRun` 으로 로컬 서버 실행
3. http://localhost:8082 접속하여 화면 캡처
4. 주요 기능 시연 및 캡처

## 팀원 역할
- **프론트엔드**: HTML/CSS/JS 작업 (static 폴더)
- **백엔드**: Spring Boot API 개발 (controller/service)
- **통합**: 프론트-백엔드 연동 테스트
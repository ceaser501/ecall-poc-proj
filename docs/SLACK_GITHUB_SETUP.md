# 📢 GitHub-Slack 연동 설정 가이드

## 🎯 개요
GitHub 저장소에 Push 또는 Pull Request가 발생할 때 자동으로 Slack 채널에 알림을 보내는 설정 가이드입니다.

## 📋 필요한 것
1. Slack 워크스페이스 관리자 권한
2. GitHub 저장소 Settings 접근 권한
3. 알림을 받을 Slack 채널

## 🔧 설정 단계

### 1단계: Slack Webhook URL 생성

1. **Slack App 생성**
   - https://api.slack.com/apps 접속
   - "Create New App" 클릭
   - "From scratch" 선택
   - App 이름: `GitHub Notifications` (원하는 이름)
   - 워크스페이스 선택

2. **Incoming Webhooks 활성화**
   - 좌측 메뉴에서 "Incoming Webhooks" 클릭
   - "Activate Incoming Webhooks" 토글을 ON으로 변경
   - 하단의 "Add New Webhook to Workspace" 클릭
   - 알림을 받을 채널 선택
   - "Allow" 클릭

3. **Webhook URL 복사**
   - 생성된 Webhook URL 복사 (예: `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX`)
   - ⚠️ **중요**: 이 URL은 비밀로 유지해야 합니다

### 2단계: GitHub Secret 설정

1. **GitHub 저장소 Settings 접속**
   - 저장소 페이지에서 Settings 탭 클릭
   - 좌측 메뉴에서 "Secrets and variables" → "Actions" 클릭

2. **Secret 추가**
   - "New repository secret" 클릭
   - Name: `SLACK_WEBHOOK_URL`
   - Value: 위에서 복사한 Webhook URL 붙여넣기
   - "Add secret" 클릭

### 3단계: 워크플로우 파일 확인

`.github/workflows/slack-notification.yml` 파일이 저장소에 있는지 확인

### 4단계: 테스트

1. **PR Merge 테스트**
   - 테스트 브랜치 생성 및 변경사항 커밋
   ```bash
   git checkout -b test/slack-notification
   git add .
   git commit -m "test: Slack 알림 테스트"
   git push origin test/slack-notification
   ```

2. **GitHub에서 PR 생성 및 Merge**
   - GitHub 웹사이트에서 Pull Request 생성
   - main 또는 develop 브랜치로 PR 생성
   - PR을 Merge (Merge 버튼 클릭)

3. **Slack 확인**
   - PR이 병합되면 설정한 채널에 알림이 도착
   - 알림에는 PR 정보, 작성자, 병합자, 변경사항 등이 포함됨

## 📌 알림 종류

### PR Merge 알림 (병합 시에만 알림)
- **발생 시점**: Pull Request가 main 또는 develop 브랜치에 병합될 때
- **알림 내용**:
  - PR 번호 및 제목
  - PR 작성자
  - 병합자 (누가 merge 했는지)
  - 소스 → 타겟 브랜치
  - 커밋 수
  - 변경사항 요약 (파일 수, 추가/삭제 라인)

## 🎨 알림 커스터마이징

### 브랜치 필터링 변경
`.github/workflows/slack-notification.yml` 파일의 `on.pull_request.branches` 섹션 수정:

```yaml
on:
  pull_request:
    types: [closed]
    branches:
      - main
      - develop
      # 추가하고 싶은 타겟 브랜치
```

### 알림 메시지 커스터마이징
워크플로우 파일의 `custom_payload` 섹션에서 메시지 형식 수정 가능

## 🚨 문제 해결

### 알림이 오지 않는 경우
1. **GitHub Actions 실행 확인**
   - 저장소 → Actions 탭에서 워크플로우 실행 상태 확인
   - 실패한 경우 로그 확인

2. **Secret 설정 확인**
   - Settings → Secrets에서 `SLACK_WEBHOOK_URL`이 있는지 확인
   - Secret 값이 올바른지 재설정

3. **Webhook URL 유효성**
   - Slack App 페이지에서 Webhook URL이 활성 상태인지 확인
   - 필요시 새로운 Webhook URL 생성

### 특정 브랜치만 알림 받기
워크플로우 파일에서 필요없는 브랜치 패턴 제거

## 📊 추가 기능 (선택사항)

### GitHub App 사용 (대안)
더 많은 기능이 필요한 경우 Slack의 공식 GitHub App 사용:
1. Slack 워크스페이스에서 `/github` 명령어 입력
2. GitHub 계정 연동
3. `/github subscribe owner/repo` 명령어로 저장소 구독

### 알림 필터링
특정 이벤트만 받기:
```
/github subscribe owner/repo issues pulls commits releases deployments
```

## 📝 참고사항
- Webhook URL은 절대 코드에 직접 포함하지 마세요
- 팀원이 변경된 경우 Slack 채널 권한 확인
- 워크플로우 수정 후에는 push/PR을 통해 테스트 필요

---

**문의사항이 있으면 팀 리더에게 문의하세요!**
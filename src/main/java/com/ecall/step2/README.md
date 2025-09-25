# Step 2: 텍스트 요약

## 담당자
- **감정분석**: 임송은
- **위치추출**: 임송은

### 📁 패키지 구조
- **s1emotionanalysis** (2-1): 발화자 감정분석
- **s2locationextraction** (2-2): 발화자 장소/주소 추출

### 🔧 사용 기술
- 감정 구분은 별도 정의 필요
- 주소 텍스트 추출 (Azure Maps, Daum, Kakao, Tmap 등 활용 가능)

### 📋 현재 상태
- 2-1 발화자 감정분석: 개발 예정
- 2-2 발화자 장소/주소 추출: 개발 예정

### 🔗 API Endpoints
- `/api/emotion` - 감정 분석
- `/api/location` - 위치 정보 추출
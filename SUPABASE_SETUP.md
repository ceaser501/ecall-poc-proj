# Supabase 간단 설정 가이드

## 1. Supabase에서 테이블 생성

1. Supabase 프로젝트 > **SQL Editor** 클릭
2. `supabase.sql` 파일의 내용을 복사하여 실행
3. `operator` 테이블과 `media_asset` 테이블이 생성됩니다

## 2. Supabase URL과 Key 가져오기

1. Supabase 프로젝트 > **Settings** > **API** 클릭
2. 두 가지 값만 복사:
   - **Project URL**: `https://xxxxx.supabase.co`
   - **anon public key**: `eyJhbG...` (긴 토큰)

## 3. .env 파일 생성

프로젝트 루트에 `.env` 파일 생성:

```bash
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=eyJhbG...your-anon-public-key
```

## 4. 애플리케이션 실행

```bash
./gradlew bootRun
```

## 5. 회원가입 테스트

1. 브라우저에서 `http://localhost:8083/pages/ecall-assistant/ecall-intro.html` 접속
2. 우측 하단 "Sign Up" 버튼 클릭
3. 정보 입력 후 "Create Account" 클릭
4. "Registration successful!" 메시지 확인

## 6. Storage 버킷 생성 (사진 업로드용)

사진 업로드 기능을 사용하려면 Supabase Storage에 버킷을 생성해야 합니다:

1. Supabase 프로젝트 > **Storage** 클릭
2. **New bucket** 버튼 클릭
3. 버킷 이름: `operator-photos`
4. **Public bucket** 체크 (공개 접근 허용)
5. **Create bucket** 클릭

### 버킷 정책 설정 (필수!)

Storage에 파일을 업로드하려면 RLS 정책을 설정해야 합니다:

**방법 1: SQL Editor 사용 (추천)**

1. Supabase > **SQL Editor** 클릭
2. 아래 SQL 실행:

```sql
-- operator-photos 버킷에 대한 업로드 허용
CREATE POLICY "Allow public uploads"
ON storage.objects
FOR INSERT
TO public
WITH CHECK (bucket_id = 'operator-photos');

-- operator-photos 버킷에 대한 읽기 허용
CREATE POLICY "Allow public reads"
ON storage.objects
FOR SELECT
TO public
USING (bucket_id = 'operator-photos');

-- operator-photos 버킷에 대한 업데이트 허용
CREATE POLICY "Allow public updates"
ON storage.objects
FOR UPDATE
TO public
USING (bucket_id = 'operator-photos')
WITH CHECK (bucket_id = 'operator-photos');

-- operator-photos 버킷에 대한 삭제 허용
CREATE POLICY "Allow public deletes"
ON storage.objects
FOR DELETE
TO public
USING (bucket_id = 'operator-photos');
```

**방법 2: UI에서 설정**

1. Storage > `operator-photos` 버킷 > **Policies** 탭
2. **New policy** > **For full customization**
3. INSERT, SELECT, UPDATE, DELETE 정책 각각 추가
   - Policy name: 적절한 이름 입력
   - Target roles: `public` 선택
   - USING/WITH CHECK expression: `true` 입력

## 7. RLS 정책 추가 (매우 중요!)

사진 업로드와 operator 정보 업데이트를 위해 RLS 정책이 필요합니다:

1. Supabase > **SQL Editor** 클릭
2. 아래 SQL 실행:

```sql
-- 기존 정책이 있다면 먼저 삭제 (에러 나면 무시하고 다음 단계로)
DROP POLICY IF EXISTS "Allow public inserts on operator" ON operator;
DROP POLICY IF EXISTS "Allow public selects on operator" ON operator;
DROP POLICY IF EXISTS "Allow public updates on operator" ON operator;
DROP POLICY IF EXISTS "Allow public inserts on media_asset" ON media_asset;
DROP POLICY IF EXISTS "Allow public selects on media_asset" ON media_asset;

-- operator 테이블 정책 추가
CREATE POLICY "Allow public inserts on operator"
ON operator FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public selects on operator"
ON operator FOR SELECT
TO public
USING (true);

CREATE POLICY "Allow public updates on operator"
ON operator FOR UPDATE
TO public
USING (true)
WITH CHECK (true);

-- media_asset 테이블 정책 추가
CREATE POLICY "Allow public inserts on media_asset"
ON media_asset FOR INSERT
TO public
WITH CHECK (true);

CREATE POLICY "Allow public selects on media_asset"
ON media_asset FOR SELECT
TO public
USING (true);
```

## 8. 데이터 확인

Supabase > **Table Editor** > **operator** 테이블 확인:
- `id`: `op-{uuid}` 형식으로 자동 생성
- `password`: BCrypt로 해싱되어 저장
- `photo_id`: 사진 업로드 시 자동으로 업데이트됨
- 나머지 정보들이 저장됨

## 끝!

이전처럼 DATABASE_URL, USERNAME, PASSWORD가 필요 없습니다.
**Supabase URL과 Key만 있으면 됩니다!** 🎉

### 문제 해결

**사진이 업로드되지 않거나 로그인 후 사진이 보이지 않는 경우:**
- 7번의 RLS 정책이 제대로 추가되었는지 확인하세요
- Supabase > Authentication > Policies에서 operator와 media_asset 테이블의 정책 확인

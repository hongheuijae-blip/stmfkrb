# Steampunk Game

## Secrets 등록 (Settings → Secrets and variables → Actions)
- ANTHROPIC_API_KEY
- FIREBASE_SERVICE_ACCOUNT       (Firebase 서비스 계정 JSON 전체 내용)
- GOOGLE_SERVICE_ACCOUNT         (Drive API 서비스 계정 JSON 전체 내용)
- DRIVE_IMAGE_FOLDER_ID
- DRIVE_APK_FOLDER_ID
- GOOGLE_SERVICES_JSON_BASE64    (google-services.json 파일을 base64 인코딩한 값)

## 실행 순서
1. Actions 탭 → "Generate Game Data" → Run workflow (데이터/이미지 생성)
2. android/ 코드 수정 후 push → "Build and Upload APK"가 자동 실행
3. Google Drive의 APK 폴더에서 최신 apk 다운로드해서 폰에 설치
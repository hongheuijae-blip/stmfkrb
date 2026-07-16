// scripts/drive.js
const { google } = require("googleapis");

/**
 * Google Drive OAuth2 Client 생성
 */
function getDriveClient() {
  const auth = new google.auth.OAuth2(
    process.env.GOOGLE_OAUTH_CLIENT_ID,
    process.env.GOOGLE_OAUTH_CLIENT_SECRET,
    process.env.GOOGLE_OAUTH_REFRESH_TOKEN
  );

  auth.setCredentials({
    refresh_token: process.env.GOOGLE_OAUTH_REFRESH_TOKEN,
  });

  return google.drive({ version: "v3", auth });
}

/**
 * 파일명 sanitize:
 * - 작은따옴표(') → 쿼리 깨짐 방지
 * - 공백, 특수문자 → 안전하게 유지
 */
function sanitizeFilename(name) {
  return name.replace(/'/g, "\\'");
}

/**
 * Google Drive에 파일 업로드 또는 교체
 * - buffer 기반 업로드 (generate.js와 호환)
 * - 동일 이름 파일 존재 시 update
 * - 없으면 create
 */
async function uploadOrReplace(buffer, filename, mimeType) {
  const drive = getDriveClient();
  const folderId = process.env.DRIVE_IMAGE_FOLDER_ID;

  if (!folderId) {
    throw new Error("DRIVE_IMAGE_FOLDER_ID 환경변수가 없습니다.");
  }

  // 🔥 파일명 안전 처리
  const safeName = sanitizeFilename(filename);

  // 🔥 Google Drive 검색 쿼리 (q)
  const q = `name = '${safeName}' and '${folderId}' in parents and trashed = false`;

  // 기존 파일 검색
  const existing = await drive.files.list({
    q,
    fields: "files(id)",
    spaces: "drive",
  });

  // 기존 파일이 있으면 update
  if (existing.data.files.length > 0) {
    const fileId = existing.data.files[0].id;

    await drive.files.update({
      fileId,
      media: {
        mimeType,
        body: buffer,
      },
    });

    return fileId;
  }

  // 없으면 새 파일 생성
  const res = await drive.files.create({
    requestBody: {
      name: safeName,
      parents: [folderId],
    },
    media: {
      mimeType,
      body: buffer,
    },
  });

  return res.data.id;
}

module.exports = {
  getDriveClient,
  uploadOrReplace,
};

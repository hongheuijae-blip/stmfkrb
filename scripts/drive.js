// scripts/drive.js
const { google } = require("googleapis");
const { Readable } = require("stream");

/**
 * Buffer → ReadableStream 변환
 */
function bufferToStream(buffer) {
  return Readable.from(buffer);
}

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
 * 파일명 sanitize
 */
function sanitizeFilename(name) {
  return name.replace(/'/g, "\\'");
}

/**
 * Google Drive 업로드 또는 교체
 */
async function uploadOrReplace(buffer, filename, mimeType) {
  const drive = getDriveClient();
  const folderId = process.env.DRIVE_IMAGE_FOLDER_ID;

  if (!folderId) {
    throw new Error("DRIVE_IMAGE_FOLDER_ID 환경변수가 없습니다.");
  }

  const safeName = sanitizeFilename(filename);

  const q = `name = '${safeName}' and '${folderId}' in parents and trashed = false`;

  // 기존 파일 검색
  const existing = await drive.files.list({
    q,
    fields: "files(id)",
    spaces: "drive",
  });

  const stream = bufferToStream(buffer);

  if (existing.data.files.length > 0) {
    const fileId = existing.data.files[0].id;

    await drive.files.update({
      fileId,
      media: {
        mimeType,
        body: stream,
      },
    });

    return fileId;
  }

  // 새 파일 업로드
  const res = await drive.files.create({
    requestBody: {
      name: safeName,
      parents: [folderId],
    },
    media: {
      mimeType,
      body: stream,
    },
  });

  return res.data.id;
}

module.exports = {
  getDriveClient,
  uploadOrReplace,
};

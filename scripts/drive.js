// scripts/drive.js
const { google } = require('googleapis');
const fs = require('fs');

function getDriveClient() {
  const auth = new google.auth.OAuth2(
    process.env.GOOGLE_OAUTH_CLIENT_ID,
    process.env.GOOGLE_OAUTH_CLIENT_SECRET,
    process.env.GOOGLE_OAUTH_REFRESH_TOKEN
  );

  auth.setCredentials({
    refresh_token: process.env.GOOGLE_OAUTH_REFRESH_TOKEN
  });

  return google.drive({ version: "v3", auth });
}

async function uploadToDrive(buffer, filename, mimeType) {
  const drive = getDriveClient();

  const folderId = process.env.DRIVE_IMAGE_FOLDER_ID;
  if (!folderId) {
    throw new Error("DRIVE_IMAGE_FOLDER_ID 환경변수가 없습니다.");
  }

  const res = await drive.files.create({
    requestBody: {
      name: filename,
      parents: [folderId]
    },
    media: {
      mimeType,
      body: buffer
    }
  });

  return res.data.id;
}

// 📌 추가: 동일 파일명 존재 시 덮어쓰기 지원하는 함수
async function uploadOrReplace(drive, folderId, filePath, filename, mimeType) {
  const response = await drive.files.list({
    q: `name = '${filename}' and '${folderId}' in parents and trashed = false`,
    fields: 'files(id)',
    spaces: 'drive',
  });

  const files = response.data.files;
  const media = {
    mimeType: mimeType,
    body: fs.createReadStream(filePath)
  };

  if (files && files.length > 0) {
    // 동일 이름의 파일이 이미 있으면 update 실행
    const fileId = files[0].id;
    await drive.files.update({
      fileId: fileId,
      media: media
    });
    return fileId;
  } else {
    // 없으면 신규 create 실행
    const res = await drive.files.create({
      requestBody: {
        name: filename,
        parents: [folderId]
      },
      media: media
    });
    return res.data.id;
  }
}

module.exports = {
  getDriveClient,
  uploadToDrive,
  uploadOrReplace // 📌 내보내기에 추가
};
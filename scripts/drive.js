// scripts/drive.js
const { google } = require('googleapis');

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

module.exports = {
  getDriveClient,
  uploadToDrive
};

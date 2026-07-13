const { google } = require("googleapis");
const fs = require("fs");
const path = require("path");

function getDriveClient() {
  const oauth2Client = new google.auth.OAuth2(
    process.env.GOOGLE_OAUTH_CLIENT_ID,
    process.env.GOOGLE_OAUTH_CLIENT_SECRET
  );
  oauth2Client.setCredentials({
    refresh_token: process.env.GOOGLE_OAUTH_REFRESH_TOKEN,
  });
  return google.drive({ version: "v3", auth: oauth2Client });
}

async function uploadOrReplace(drive, folderId, filePath, fileName, mimeType) {
  const existing = await drive.files.list({
    q: `'${folderId}' in parents and name='${fileName}' and trashed=false`,
    fields: "files(id)",
  });

  const media = { mimeType, body: fs.createReadStream(filePath) };

  if (existing.data.files.length > 0) {
    const fileId = existing.data.files[0].id;
    await drive.files.update({ fileId, media });
    return fileId;
  } else {
    const res = await drive.files.create({
      requestBody: { name: fileName, parents: [folderId] },
      media,
      fields: "id",
    });
    const fileId = res.data.id;
    await drive.permissions.create({
      fileId,
      requestBody: { role: "reader", type: "anyone" },
    });
    return fileId;
  }
}

if (require.main === module) {
  (async () => {
    const filePath = process.argv[2];
    const drive = getDriveClient();
    const fileId = await uploadOrReplace(
      drive,
      process.env.DRIVE_APK_FOLDER_ID,
      filePath,
      process.env.DRIVE_APK_FILE_NAME || path.basename(filePath),
      "application/vnd.android.package-archive"
    );
    console.log(`APK 업로드 완료: https://drive.google.com/uc?export=download&id=${fileId}`);
  })().catch((e) => { console.error(e); process.exit(1); });
}

module.exports = { getDriveClient, uploadOrReplace };
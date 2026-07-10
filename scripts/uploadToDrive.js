// generate.js에서 이미지 업로드용, build-apk.yml에서 APK 업로드용으로 공용 사용
const { google } = require("googleapis");
const fs = require("fs");
const path = require("path");

function getDriveClient() {
  const credentials = JSON.parse(process.env.GOOGLE_SERVICE_ACCOUNT);
  const auth = new google.auth.GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/drive"],
  });
  return google.drive({ version: "v3", auth });
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

// CLI 실행 시 (APK 업로드용): node uploadToDrive.js <파일경로>
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
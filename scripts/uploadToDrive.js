async function uploadOrReplace(drive, folderId, filePath, fileName, mimeType) {

  // 파일명을 쿼리에 넣지 않고 폴더 내 파일 목록만 조회
  const existingFiles = await drive.files.list({
    q: `'${folderId}' in parents and trashed=false`,
    fields: "files(id,name)",
  });

  // JavaScript에서 정확한 파일명 비교
  const existing = existingFiles.data.files.find(f => f.name === fileName);

  const media = {
    mimeType,
    body: fs.createReadStream(filePath),
  };

  if (existing) {
    await drive.files.update({
      fileId: existing.id,
      media,
    });
    return existing.id;
  } else {
    const res = await drive.files.create({
      requestBody: {
        name: fileName,
        parents: [folderId],
      },
      media,
      fields: "id",
    });

    const fileId = res.data.id;

    await drive.permissions.create({
      fileId,
      requestBody: {
        role: "reader",
        type: "anyone",
      },
    });

    return fileId;
  }
}
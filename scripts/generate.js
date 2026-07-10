const admin = require("firebase-admin");
const Anthropic = require("@anthropic-ai/sdk");
const fetch = require("node-fetch");
const fs = require("fs");
const path = require("path");
const { validateGameDataJson } = require("./schemaValidator");
const { getDriveClient, uploadOrReplace } = require("./uploadToDrive");

admin.initializeApp({
  credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
});
const db = admin.firestore();
const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });

const PROMPT = `너는 스팀펑크 세계관의 게임 디자이너이자 데이터 엔지니어야.
다음 형식의 JSON만 출력해줘. 다른 텍스트는 절대 출력하지 마.
{
  "monsters": [{"name":"", "level":1, "hp":1, "attack":1, "defense":1, "element":"", "lore":""}],
  "weapons": [{"name":"", "type":"", "attack":1, "rarity":"", "description":""}],
  "robotParts": [{"name":"", "slot":"", "bonusAttack":1, "bonusDefense":1, "specialEffect":""}],
  "cities": [{"name":"", "region":"", "population":1, "description":""}],
  "quests": [{"title":"", "description":"", "difficulty":1, "rewardExp":1}],
  "coreConfigs": [{"key":"", "value":{}, "description":""}]
}
몬스터 10종, 무기 10종, 로봇 파츠 5종, 도시 2개, 퀘스트 5개, 코어 설정 3개를 만들어줘.`;

async function downloadImage(url, tmpPath) {
  const res = await fetch(url);
  const buffer = await res.buffer();
  fs.writeFileSync(tmpPath, buffer);
}

async function main() {
  const msg = await anthropic.messages.create({
    model: "claude-sonnet-5",
    max_tokens: 4000,
    messages: [{ role: "user", content: PROMPT }],
  });

  const raw = msg.content[0].text.trim();
  const gameData = JSON.parse(raw);

  if (!validateGameDataJson(gameData)) {
    throw new Error("JSON 스키마 검증 실패");
  }

  const drive = getDriveClient();
  const imageTargets = ["monsters", "weapons", "robotParts", "cities"];

  for (const col of imageTargets) {
    for (const item of gameData[col]) {
      const prompt = `steampunk style ${col}, ${item.name}, gears and steam, detailed illustration`;
      const imgUrl = `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?width=768&height=768&nologo=true`;

      const tmpPath = path.join("/tmp", `${col}-${item.name}.png`.replace(/\s/g, "_"));
      await downloadImage(imgUrl, tmpPath);

      const fileId = await uploadOrReplace(
        drive,
        process.env.DRIVE_IMAGE_FOLDER_ID,
        tmpPath,
        path.basename(tmpPath),
        "image/png"
      );
      item.imagePath = `https://drive.google.com/uc?export=view&id=${fileId}`;
      fs.unlinkSync(tmpPath);
    }
  }

  const batch = db.batch();
  for (const [col, items] of Object.entries(gameData)) {
    items.forEach((item) => {
      const ref = db.collection(col).doc();
      batch.set(ref, {
        ...item,
        id: ref.id,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        version: 1,
        isActive: true,
      });
    });
  }
  await batch.commit();
  console.log("완료: Firestore 저장 + 이미지 업로드");
}

main().catch((e) => { console.error(e); process.exit(1); });
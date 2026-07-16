// scripts/generate.js
const admin = require("firebase-admin");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const fetch = require("node-fetch");
const fs = require("fs");
const path = require("path");
const Jimp = require("jimp");

const { validateGameDataJson } = require("./schemaValidator");
const { uploadOrReplace } = require("./drive");

// Firebase 초기화
admin.initializeApp({
  credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
});
const db = admin.firestore();

// Gemini 초기화
const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-flash-latest" });

// 메인 프롬프트
const PROMPT = `너는 스팀펑크 세계관의 게임 디자이너이자 데이터 엔지니어야.
[세계관 설정]
이 세계는 산업 혁명기의 증기 기관 문명과 고대 신앙이 공존한다.
사람들은 톱니바퀴와 증기 속에서도 옛 경전(성경 시편)의 구절을 새기고 암송하며 위안을 얻는다.
낡은 로봇 부품, 무기의 손잡이, 도시의 성당 첨탑, 몬스터의 전승 설화에는 드물게 개역한글판 성경 시편의 구절이 빛바랜 각인이나 인용구로 새겨져 있다.

[작업]
1) 몬스터 10종 생성
2) 무기 10종 생성
3) 로봇 파츠 5종 생성
4) 도시 2개 생성
5) 퀘스트 5개 생성
6) 코어 밸런스 설정 3개 생성

[시편 인용 규칙 - 중요]
- monsters, weapons, robotParts, cities, quests 각 배열마다, 전체 항목 수의 약 20%에만 scripture/scriptureRef 필드를 채워줘.
- 나머지 80% 항목은 scripture/scriptureRef를 빈 문자열("")로 둬.
- 인용은 실제 존재하는 시편 구절이어야 하고, 정확한 장:절을 표기해줘.
- lore/description에 자연스럽게 녹여줘.

[출력 형식]
{
 "monsters":[{"name":"", "level":1, "hp":1, "attack":1, "defense":1, "element":"", "lore":"", "scripture":"", "scriptureRef":""}],
 "weapons":[{"name":"", "type":"", "attack":1, "rarity":"", "description":"", "scripture":"", "scriptureRef":""}],
 "robotParts":[{"name":"", "slot":"", "bonusAttack":1, "bonusDefense":1, "specialEffect":"", "scripture":"", "scriptureRef":""}],
 "cities":[{"name":"", "region":"", "population":1, "description":"", "scripture":"", "scriptureRef":""}],
 "quests":[{"title":"", "description":"", "difficulty":1, "rewardExp":1, "scripture":"", "scriptureRef":""}],
 "coreConfigs":[{"key":"", "value":{}, "description":""}]
}
JSON 이외의 텍스트나 마크다운 코드블록 표시는 절대 출력하지 마.`;

// 이미지 다운로드
async function downloadImage(url, tmpPath) {
  const res = await fetch(url);
  const buffer = await res.buffer();
  fs.writeFileSync(tmpPath, buffer);
}

// 이미지 생성 URL
function buildImageUrl(col, item) {
  let detail = "";
  switch (col) {
    case "monsters": detail = `element ${item.element}, level ${item.level}`; break;
    case "weapons": detail = `type ${item.type}, rarity ${item.rarity}`; break;
    case "robotParts": detail = `slot ${item.slot}`; break;
    case "cities": detail = `region ${item.region}`; break;
  }

  const prompt =
    `steampunk game icon, ${col} concept, ${item.name}, ${detail}, ` +
    `centered single subject, plain solid white background, no shadow, ` +
    `flat icon illustration style, clean silhouette, no text, no watermark`;

  const seed = Math.floor(Math.random() * 1000000);

  return `https://image.pollinations.ai/prompt/${encodeURIComponent(prompt)}?width=768&height=768&nologo=true&seed=${seed}`;
}

// 흰 배경 투명화
async function makeBackgroundTransparent(filePath) {
  const image = await Jimp.read(filePath);
  const w = image.bitmap.width;
  const h = image.bitmap.height;

  const corners = [
    image.getPixelColor(2, 2),
    image.getPixelColor(w - 3, 2),
    image.getPixelColor(2, h - 3),
    image.getPixelColor(w - 3, h - 3),
  ];

  const rgbaList = corners.map((c) => Jimp.intToRGBA(c));
  const bgR = rgbaList.reduce((s, c) => s + c.r, 0) / 4;
  const bgG = rgbaList.reduce((s, c) => s + c.g, 0) / 4;
  const bgB = rgbaList.reduce((s, c) => s + c.b, 0) / 4;

  const threshold = 40;

  image.scan(0, 0, w, h, function (x, y, idx) {
    const r = this.bitmap.data[idx + 0];
    const g = this.bitmap.data[idx + 1];
    const b = this.bitmap.data[idx + 2];
    const dist = Math.sqrt((r - bgR) ** 2 + (g - bgG) ** 2 + (b - bgB) ** 2);
    if (dist < threshold) this.bitmap.data[idx + 3] = 0;
  });

  await image.writeAsync(filePath);
}

// Gemini 재시도
async function generateWithRetry(prompt, maxRetries = 3) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const result = await model.generateContent(prompt);
      return result.response.text().trim();
    } catch (e) {
      const retryable = e.status === 503 || e.status === 429;
      if (!retryable || i === maxRetries - 1) throw e;

      const waitMs = (i + 1) * 15000;
      console.log(`Gemini 재시도 ${i + 1}/${maxRetries} - ${waitMs / 1000}초 후`);
      await new Promise((r) => setTimeout(r, waitMs));
    }
  }
}

async function main() {
  let raw = await generateWithRetry(PROMPT);

  raw = raw.replace(/^```json\s*/i, "").replace(/```\s*$/, "").trim();

  const gameData = JSON.parse(raw);

  if (!validateGameDataJson(gameData)) {
    throw new Error("JSON 스키마 검증 실패");
  }

  const imageTargets = ["monsters", "weapons", "robotParts", "cities"];

  for (const col of imageTargets) {
    for (const item of gameData[col]) {
      const imgUrl = buildImageUrl(col, item);
      const tmpPath = path.join("/tmp", `${col}-${item.name}.png`.replace(/\s/g, "_"));

      await downloadImage(imgUrl, tmpPath);
      await makeBackgroundTransparent(tmpPath);

      const buffer = fs.readFileSync(tmpPath);

      const fileId = await uploadOrReplace(
        buffer,
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

main().catch((e) => {
  console.error(e);
  process.exit(1);
});

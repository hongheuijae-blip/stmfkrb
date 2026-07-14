// Firebase 설정 (steamrb-dfd04 프로젝트, 웹 앱 steamfunkRPG 설정값)
const firebaseConfig = {
  apiKey: "AIzaSyCeIOPRQeEGwbRnNQAZSSZ1cbCc_kAr-Y4",
  authDomain: "steamrb-dfd04.firebaseapp.com",
  projectId: "steamrb-dfd04",
  storageBucket: "steamrb-dfd04.firebasestorage.app",
  messagingSenderId: "938837590440",
  appId: "1:938837590440:web:5b972203cd9118b4885d9d"
};

// Kotlin에서 주입해주는 전역 변수. 없으면 null.
window.INJECTED_PLAYER_IMAGE_URL = window.INJECTED_PLAYER_IMAGE_URL || null;

firebase.initializeApp(firebaseConfig);
const db = firebase.firestore();

const MAP_W = 1600;
const MAP_H = 1200;
const PLAYER_SPEED = 220;

class OverworldScene extends Phaser.Scene {
  constructor() {
    super("OverworldScene");
    this.monsterData = [];
    this.playerImageUrl = null;
    this.encounterLock = false;
  }

  preload() {
    // 로딩 대기 텍스처(원)를 즉석에서 그려서 fallback으로 사용
    const g = this.make.graphics({ x: 0, y: 0, add: false });
    g.fillStyle(0xffc107, 1);
    g.fillCircle(28, 28, 28);
    g.generateTexture("fallback_player", 56, 56);
    g.clear();
    g.fillStyle(0xe53935, 1);
    g.fillCircle(30, 30, 30);
    g.generateTexture("fallback_monster", 60, 60);
    g.destroy();
  }

  async create() {
    // 격자 배경
    const grid = this.add.graphics();
    grid.lineStyle(1, 0xffffff, 0.06);
    for (let x = 0; x < MAP_W; x += 60) grid.lineBetween(x, 0, x, MAP_H);
    for (let y = 0; y < MAP_H; y += 60) grid.lineBetween(0, y, MAP_W, y);

    this.physics.world.setBounds(0, 0, MAP_W, MAP_H);
    this.cameras.main.setBounds(0, 0, MAP_W, MAP_H);

    // 몬스터/로봇파츠 데이터 로드
    const [monstersSnap, partsSnap] = await Promise.all([
      db.collection("monsters").get(),
      db.collection("robotParts").get()
    ]);

    this.monsterData = monstersSnap.docs.map(d => d.data());
    // Kotlin에서 실제 "장착된" 파츠 이미지를 주입받아 우선 사용
    this.playerImageUrl = window.INJECTED_PLAYER_IMAGE_URL || null;

    // 플레이어 이미지 로드 (있으면), 로드 후 스프라이트 생성
    const startPlayer = () => {
      const texKey = this.textures.exists("player_img") ? "player_img" : "fallback_player";
      this.player = this.physics.add.image(MAP_W / 2, MAP_H / 2, texKey).setDisplaySize(56, 56);
      this.player.setCircle(28);
      this.player.setCollideWorldBounds(true);
      this.cameras.main.startFollow(this.player, true, 0.1, 0.1);
      this.spawnMonsters();
    };

    if (this.playerImageUrl) {
      this.load.image("player_img", this.playerImageUrl);
      this.load.once("complete", startPlayer);
      this.load.once("loaderror", startPlayer);
      this.load.start();
    } else {
      startPlayer();
    }

    this.setupJoystick();
  }

  spawnMonsters() {
    this.monsterGroup = this.physics.add.group();
    const count = Math.min(this.monsterData.length, 8);
    for (let i = 0; i < count; i++) {
      const m = this.monsterData[i];
      const x = Phaser.Math.Between(100, MAP_W - 100);
      const y = Phaser.Math.Between(100, MAP_H - 100);
      const spawn = (key) => {
        const sprite = this.physics.add.image(x, y, key).setDisplaySize(60, 60);
        sprite.setCircle(30);
        sprite.monsterData = m;
        this.monsterGroup.add(sprite);
      };
      if (m.imagePath) {
        const key = "monster_" + i;
        this.load.image(key, m.imagePath);
        this.load.once("filecomplete-image-" + key, () => spawn(key));
        this.load.once("loaderror", () => spawn("fallback_monster"));
        this.load.start();
      } else {
        spawn("fallback_monster");
      }
    }

    this.physics.add.overlap(this.player, this.monsterGroup, (player, monsterSprite) => {
      if (this.encounterLock) return;
      this.encounterLock = true;
      const data = monsterSprite.monsterData;
      if (window.AndroidBridge) {
        window.AndroidBridge.onEncounter(data.id || "");
      }
      // 조우 후 잠깐 재조우 방지 및 몬스터 위치 리셋
      setTimeout(() => {
        monsterSprite.setPosition(
          Phaser.Math.Between(100, MAP_W - 100),
          Phaser.Math.Between(100, MAP_H - 100)
        );
        this.encounterLock = false;
      }, 1500);
    });
  }

  setupJoystick() {
    this.joystickBase = null;
    this.joystickThumb = null;
    this.joystickVector = { x: 0, y: 0 };
    this.joystickPointerId = null;

    const baseRadius = 55;
    const thumbRadius = 24;

    this.input.on("pointerdown", (pointer) => {
      if (pointer.x > this.scale.width * 0.5) return; // 화면 왼쪽 절반만 조이스틱 영역
      if (this.joystickPointerId !== null) return;
      this.joystickPointerId = pointer.id;

      this.joystickBaseX = pointer.x;
      this.joystickBaseY = pointer.y;

      if (this.joystickBase) this.joystickBase.destroy();
      if (this.joystickThumb) this.joystickThumb.destroy();

      this.joystickBase = this.add.circle(pointer.x, pointer.y, baseRadius, 0xffffff, 0.15).setScrollFactor(0);
      this.joystickThumb = this.add.circle(pointer.x, pointer.y, thumbRadius, 0xffffff, 0.6).setScrollFactor(0);
    });

    this.input.on("pointermove", (pointer) => {
      if (pointer.id !== this.joystickPointerId || !this.joystickBase) return;
      const dx = pointer.x - this.joystickBaseX;
      const dy = pointer.y - this.joystickBaseY;
      const dist = Math.sqrt(dx * dx + dy * dy);
      const clamped = Math.min(dist, baseRadius);
      const angle = Math.atan2(dy, dx);
      const tx = this.joystickBaseX + Math.cos(angle) * clamped;
      const ty = this.joystickBaseY + Math.sin(angle) * clamped;
      this.joystickThumb.setPosition(tx, ty);
      this.joystickVector = { x: (Math.cos(angle) * clamped) / baseRadius, y: (Math.sin(angle) * clamped) / baseRadius };
    });

    const clearJoystick = (pointer) => {
      if (pointer.id !== this.joystickPointerId) return;
      this.joystickPointerId = null;
      this.joystickVector = { x: 0, y: 0 };
      if (this.joystickBase) { this.joystickBase.destroy(); this.joystickBase = null; }
      if (this.joystickThumb) { this.joystickThumb.destroy(); this.joystickThumb = null; }
    };
    this.input.on("pointerup", clearJoystick);
    this.input.on("pointerupoutside", clearJoystick);
  }

  update() {
    if (!this.player) return;
    const v = this.joystickVector;
    this.player.setVelocity(v.x * PLAYER_SPEED, v.y * PLAYER_SPEED);
  }
}

const config = {
  type: Phaser.AUTO,
  parent: "game-container",
  width: window.innerWidth,
  height: window.innerHeight,
  backgroundColor: "#1b1b1b",
  physics: {
    default: "arcade",
    arcade: { gravity: { y: 0 }, debug: false }
  },
  scale: {
    mode: Phaser.Scale.RESIZE,
    autoCenter: Phaser.Scale.CENTER_BOTH
  },
  scene: [OverworldScene]
};

new Phaser.Game(config);
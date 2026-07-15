package com.hjhong.steampunkgame
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random
private const val MAP_W = 900f
private const val MAP_H = 600f
private const val TRAILER_RADIUS = 24f
private const val COLLISION_RADIUS = 20f // 플레이어가 코너를 자연스럽게 돌 수 있도록 살짝 작게 설정
private const val MAP_SPEED = 5f
private const val ENCOUNTER_DISTANCE_THRESHOLD = 350f
private const val ENCOUNTER_CHANCE = 0.4f
private val TRAILER_ICON_SIZE = 56.dp
// 📌 1단계: 타일 크기 및 맵 레이아웃(15x10) 정의
private const val TILE_SIZE = 60f
private const val MAP_COLS = 15
private const val MAP_ROWS = 10
// 0: 이동 가능 (잔디/바닥), 1: 이동 불가 (철제 벽/장애물)
private val MAP_LAYOUT = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 1, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1),
    intArrayOf(1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)
// 📌 1단계: 원(Circle)과 타일(AABB) 사각형 간의 충돌 검사 로직 구현
private fun checkCollision(x: Float, y: Float, radius: Float): Boolean {
    // 맵 경계선 충돌 체크
    if (x - radius < 0 || x + radius > MAP_W || y - radius < 0 || y + radius > MAP_H) {
        return true
    }
    // 캐릭터 주변에 해당하는 타일만 검사하여 성능 최적화
    val startCol = kotlin.math.max(0, ((x - radius) / TILE_SIZE).toInt())
    val endCol = kotlin.math.min(MAP_COLS - 1, ((x + radius) / TILE_SIZE).toInt())
    val startRow = kotlin.math.max(0, ((y - radius) / TILE_SIZE).toInt())
    val endRow = kotlin.math.min(MAP_ROWS - 1, ((y + radius) / TILE_SIZE).toInt())
    for (r in startRow..endRow) {
        for (c in startCol..endCol) {
            if (MAP_LAYOUT[r][c] == 1) {
                val tileLeft = c * TILE_SIZE
                val tileTop = r * TILE_SIZE
                val tileRight = tileLeft + TILE_SIZE
                val tileBottom = tileTop + TILE_SIZE
                // 타일 영역 내에서 캐릭터와 가장 가까운 지점 계산
                val closestX = x.coerceIn(tileLeft, tileRight)
                val closestY = y.coerceIn(tileTop, tileBottom)
                val dx = x - closestX
                val dy = y - closestY
                if (dx * dx + dy * dy < radius * radius) {
                    return true // 충돌 발생
                }
            }
        }
    }
    return false
}
@Composable
fun OverworldScreen(
    monsters: List<Monster>,
    playerImagePath: String?,
    onEncounter: (Monster) -> Unit
) {
    var trailerPos by remember { mutableStateOf(Offset(MAP_W / 2f, MAP_H / 2f)) }
    // 📌 초기 생성 위치를 이동 가능한 통로(2행 2열: x=90, y=90)로 조정
    var trailerPos by remember { mutableStateOf(Offset(90f, 90f)) }
    var joystickVector by remember { mutableStateOf(Offset.Zero) }
    var trailerVelocity by remember { mutableStateOf(Offset.Zero) }
    var traveledSinceLastCheck by remember { mutableStateOf(0f) }
    var encounterMonster by remember { mutableStateOf<Monster?>(null) }
    var isPaused by remember { mutableStateOf(false) }
    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(16)
            if (monsters.isNotEmpty()) {
                // 트레일러도 관성으로 이동 (트레일러답게 좀 더 미끄러지는 느낌)
                val targetVelocity = joystickVector * MAP_SPEED
                val inertiaFactor = 0.1f
                trailerVelocity = Offset(
                    trailerVelocity.x + (targetVelocity.x - trailerVelocity.x) * inertiaFactor,
                    trailerVelocity.y + (targetVelocity.y - trailerVelocity.y) * inertiaFactor
                )
                if (trailerVelocity.getDistance() > 0.05f) {
                    val dx = trailerVelocity.x
                    val dy = trailerVelocity.y
                    val newX = (trailerPos.x + dx).coerceIn(TRAILER_RADIUS, MAP_W - TRAILER_RADIUS)
                    val newY = (trailerPos.y + dy).coerceIn(TRAILER_RADIUS, MAP_H - TRAILER_RADIUS)
                    trailerPos = Offset(newX, newY)
                    traveledSinceLastCheck += kotlin.math.sqrt(dx * dx + dy * dy)
                    // 📌 1단계: X축과 Y축 이동을 각각 별도로 검사하여 슬라이딩(벽 미끄러짐) 구현
                    val nextX = (trailerPos.x + dx).coerceIn(TRAILER_RADIUS, MAP_W - TRAILER_RADIUS)
                    val canMoveX = !checkCollision(nextX, trailerPos.y, COLLISION_RADIUS)
                    val finalX = if (canMoveX) nextX else trailerPos.x
                    val nextY = (trailerPos.y + dy).coerceIn(TRAILER_RADIUS, MAP_H - TRAILER_RADIUS)
                    val canMoveY = !checkCollision(finalX, nextY, COLLISION_RADIUS)
                    val finalY = if (canMoveY) nextY else trailerPos.y
                    val actualDx = finalX - trailerPos.x
                    val actualDy = finalY - trailerPos.y
                    
                    trailerPos = Offset(finalX, finalY)
                    // 실제 움직인 거리만큼만 조우 판정에 반영
                    traveledSinceLastCheck += kotlin.math.sqrt(actualDx * actualDx + actualDy * actualDy)
                    if (traveledSinceLastCheck >= ENCOUNTER_DISTANCE_THRESHOLD) {
                        traveledSinceLastCheck = 0f
                        if (Random.nextFloat() < ENCOUNTER_CHANCE) {
                            encounterMonster = monsters.random()
                            isPaused = true
                        }
                    }
                }
            }
        } // 📌 while (!isPaused) 블록을 닫음
    } // 📌 LaunchedEffect(isPaused) 블록을 닫음
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "트레일러로 필드를 이동하세요",
                "트레일러로 필드를 이동하세요 (장애물을 돌아서 이동할 수 있습니다)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF2E3B2E))
                    .background(Color(0xFF1E261E))
            ) {
                val scaleX = maxWidth.value / MAP_W
                val scaleY = maxHeight.value / MAP_H
                // 📌 1단계: 기존의 얇은 그리드 라인 대신 타일맵 렌더링 적용
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSize = 60f
                    var gx = 0f
                    while (gx < MAP_W) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(gx * (size.width / MAP_W), 0f),
                            end = Offset(gx * (size.width / MAP_W), size.height)
                        )
                        gx += gridSize
                    val tileW = size.width / MAP_COLS
                    val tileH = size.height / MAP_ROWS
                    for (r in 0 until MAP_ROWS) {
                        for (c in 0 until MAP_COLS) {
                            // 장애물은 갈색 계열 철벽, 바닥은 스팀펑크풍 어두운 녹색 테마
                            val color = if (MAP_LAYOUT[r][c] == 1) {
                                Color(0xFF4A3B32) // 장애물 (벽)
                            } else {
                                Color(0xFF243024) // 이동 가능한 통로
                            }
                            drawRect(
                                color = color,
                                topLeft = Offset(c * tileW, r * tileH),
                                size = androidx.compose.ui.geometry.Size(tileW, tileH)
                            )
                            // 타일 구분을 위한 아주 얇은 테두리선
                            drawRect(
                                color = Color.White.copy(alpha = 0.04f),
                                topLeft = Offset(c * tileW, r * tileH),
                                size = androidx.compose.ui.geometry.Size(tileW, tileH),
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                    var gy = 0f
                    while (gy < MAP_H) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, gy * (size.height / MAP_H)),
                            end = Offset(size.width, gy * (size.height / MAP_H))
                        )
                        gy += gridSize
                    }
                }
                GameEntity(
    }
}

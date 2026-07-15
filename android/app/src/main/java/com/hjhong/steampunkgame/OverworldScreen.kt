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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random
private const val MAP_W = 900f
private const val MAP_H = 600f
private const val TRAILER_RADIUS = 24f
private const val MAP_SPEED = 5f
private const val ENCOUNTER_DISTANCE_THRESHOLD = 350f
private const val ENCOUNTER_CHANCE = 0.4f
private val TRAILER_ICON_SIZE = 56.dp
@Composable
fun OverworldScreen(
    monsters: List<Monster>,
    playerImagePath: String?,
    onEncounter: (Monster) -> Unit
) {
    var trailerPos by remember { mutableStateOf(Offset(MAP_W / 2f, MAP_H / 2f)) }
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
                                       if (traveledSinceLastCheck >= ENCOUNTER_DISTANCE_THRESHOLD) {
                        traveledSinceLastCheck = 0f
                        if (Random.nextFloat() < ENCOUNTER_CHANCE) {
                            encounterMonster = monsters.random()
                            isPaused = true
                        }
                    }
                }
            }
        } // 📌 추가: while (!isPaused) 루프 종료
    } // 📌 추가: LaunchedEffect(isPaused) 블록 종료
    
    Box(modifier = Modifier.fillMaxSize()) { // ⚠️ 이제 정상적인 Composable 영역으로 복귀
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "트레일러로 필드를 이동하세요",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF2E3B2E))
            ) {
                val scaleX = maxWidth.value / MAP_W
                val scaleY = maxHeight.value / MAP_H
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
                    imagePath = playerImagePath,
                    fallbackColor = Color(0xFFFFC107),
                    size = TRAILER_ICON_SIZE,
                    xDp = trailerPos.x * scaleX,
                    yDp = trailerPos.y * scaleY
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Joystick(onDirectionChange = { joystickVector = it })
            }
        }
        if (encounterMonster != null) {
            val monster = encounterMonster!!
            AlertDialog(
                onDismissRequest = {},
                title = { Text("몬스터 출현!") },
                text = { Text("${monster.name}이(가) 나타났습니다. 로봇에 탑승해 전투를 시작하시겠어요?") },
                confirmButton = {
                    TextButton(onClick = {
                        val m = encounterMonster
                        encounterMonster = null
                        isPaused = false
                        if (m != null) onEncounter(m)
                    }) { Text("전투 시작") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        encounterMonster = null
                        isPaused = false
                    }) { Text("도망") }
                }
            )
        }
    }
}

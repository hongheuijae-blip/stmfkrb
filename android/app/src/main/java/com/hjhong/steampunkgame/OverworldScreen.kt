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
private const val ENCOUNTER_CHANCE = 0.4f // 임계 거리 도달 시 조우할 확률

@Composable
fun OverworldScreen(
    monsters: List<Monster>,
    onEncounter: (Monster) -> Unit
) {
    var trailerPos by remember { mutableStateOf(Offset(MAP_W / 2f, MAP_H / 2f)) }
    var joystickVector by remember { mutableStateOf(Offset.Zero) }
    var traveledSinceLastCheck by remember { mutableStateOf(0f) }
    var encounterMonster by remember { mutableStateOf<Monster?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(16)

            if (joystickVector.getDistance() > 0.1f && monsters.isNotEmpty()) {
                val dx = joystickVector.x * MAP_SPEED
                val dy = joystickVector.y * MAP_SPEED
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
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "트레일러로 필드를 이동하세요",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF2E3B2E))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / MAP_W
                    val scaleY = size.height / MAP_H

                    // 간단한 격자 배경으로 이동감 표현
                    val gridSize = 60f
                    var gx = 0f
                    while (gx < MAP_W) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(gx * scaleX, 0f),
                            end = Offset(gx * scaleX, size.height)
                        )
                        gx += gridSize
                    }
                    var gy = 0f
                    while (gy < MAP_H) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, gy * scaleY),
                            end = Offset(size.width, gy * scaleY)
                        )
                        gy += gridSize
                    }

                    // 트레일러
                    drawCircle(
                        color = Color(0xFFFFC107),
                        radius = TRAILER_RADIUS * scaleX,
                        center = Offset(trailerPos.x * scaleX, trailerPos.y * scaleY)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Joystick(onDirectionChange = { joystickVector = it })
            }
        }

        if (encounterMonster != null && monsters.isEmpty().not()) {
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
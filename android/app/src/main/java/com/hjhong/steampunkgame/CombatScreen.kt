package com.hjhong.steampunkgame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*

private const val ARENA_W = 800f
private const val ARENA_H = 500f
private const val PLAYER_RADIUS = 28f
private const val MONSTER_RADIUS = 32f
private const val MOVE_SPEED = 6f
private const val ATTACK_RANGE = PLAYER_RADIUS + MONSTER_RADIUS + 10f

@Composable
fun CombatScreen(
    monster: Monster,
    playerAttack: Int,
    playerDefense: Int,
    onExit: () -> Unit
) {
    var playerPos by remember { mutableStateOf(Offset(150f, ARENA_H / 2f)) }
    var monsterPos by remember { mutableStateOf(Offset(ARENA_W - 150f, ARENA_H / 2f)) }
    var joystickVector by remember { mutableStateOf(Offset.Zero) }

    val basePlayerHp = 100 + playerDefense * 2
    var playerHp by remember { mutableStateOf(basePlayerHp) }
    var monsterHp by remember { mutableStateOf(monster.hp) }

    var attackCooldown by remember { mutableStateOf(false) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingText>()) }

    var result by remember { mutableStateOf<String?>(null) } // "win" | "lose" | null

    // 게임 루프 - 이동 + 몬스터 자동 반격
    LaunchedEffect(result) {
        var tickCount = 0
        while (result == null) {
            delay(16)
            tickCount++

            // 조이스틱 방향으로 이동 (아레나 범위 안에서)
            if (joystickVector.getDistance() > 0.1f) {
                val newX = (playerPos.x + joystickVector.x * MOVE_SPEED).coerceIn(PLAYER_RADIUS, ARENA_W - PLAYER_RADIUS)
                val newY = (playerPos.y + joystickVector.y * MOVE_SPEED).coerceIn(PLAYER_RADIUS, ARENA_H - PLAYER_RADIUS)
                playerPos = Offset(newX, newY)
            }

            // 몬스터가 플레이어 쪽으로 천천히 다가옴
            val dx = playerPos.x - monsterPos.x
            val dy = playerPos.y - monsterPos.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > ATTACK_RANGE) {
                val moveX = (dx / dist) * 2.5f
                val moveY = (dy / dist) * 2.5f
                monsterPos = Offset(
                    (monsterPos.x + moveX).coerceIn(MONSTER_RADIUS, ARENA_W - MONSTER_RADIUS),
                    (monsterPos.y + moveY).coerceIn(MONSTER_RADIUS, ARENA_H - MONSTER_RADIUS)
                )
            }

            // 몬스터 자동 공격 (약 1.5초마다, 사거리 안에 있을 때만)
            if (tickCount % 90 == 0 && dist <= ATTACK_RANGE) {
                val dmg = max(1, monster.attack - playerDefense / 2)
                playerHp = (playerHp - dmg).coerceAtLeast(0)
                floatingTexts = floatingTexts + FloatingText("-$dmg", playerPos, System.currentTimeMillis(), Color.Red)
                if (playerHp <= 0) result = "lose"
            }

            // 오래된 플로팅 텍스트 정리
            val now = System.currentTimeMillis()
            floatingTexts = floatingTexts.filter { now - it.createdAt < 800 }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 HP 바
            Column(modifier = Modifier.padding(16.dp)) {
                Text(monster.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("몬스터 HP: $monsterHp / ${monster.hp}")
                LinearProgressIndicator(
                    progress = { (monsterHp.toFloat() / monster.hp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("내 로봇 HP: $playerHp / $basePlayerHp")
                LinearProgressIndicator(
                    progress = { (playerHp.toFloat() / basePlayerHp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF2196F3)
                )
            }

            // 전투 아레나
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Color(0xFF1B1B1B))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / ARENA_W
                    val scaleY = size.height / ARENA_H

                    // 플레이어
                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = PLAYER_RADIUS * scaleX,
                        center = Offset(playerPos.x * scaleX, playerPos.y * scaleY)
                    )
                    // 몬스터
                    drawCircle(
                        color = Color(0xFFE53935),
                        radius = MONSTER_RADIUS * scaleX,
                        center = Offset(monsterPos.x * scaleX, monsterPos.y * scaleY)
                    )
                    // 사거리 표시 (플레이어 기준)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = ATTACK_RANGE * scaleX,
                        center = Offset(playerPos.x * scaleX, playerPos.y * scaleY),
                        style = Stroke(width = 2f)
                    )
                }

                // 데미지 팝업 텍스트
                floatingTexts.forEach { ft ->
                    val elapsed = System.currentTimeMillis() - ft.createdAt
                    val alpha = (1f - elapsed / 800f).coerceIn(0f, 1f)
                    val yOffset = -(elapsed / 800f) * 40f
                    Text(
                        ft.text,
                        color = ft.color.copy(alpha = alpha),
                        modifier = Modifier.offset(
                            x = (ft.pos.x / ARENA_W * 300).dp,
                            y = (ft.pos.y / ARENA_H * 200 + yOffset).dp
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 조작 UI - 조이스틱 + 공격 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Joystick(onDirectionChange = { joystickVector = it })

                Button(
                    onClick = {
                        if (!attackCooldown && result == null) {
                            val dx = playerPos.x - monsterPos.x
                            val dy = playerPos.y - monsterPos.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist <= ATTACK_RANGE) {
                                val dmg = max(1, playerAttack - monster.defense / 2)
                                monsterHp = (monsterHp - dmg).coerceAtLeast(0)
                                floatingTexts = floatingTexts + FloatingText("-$dmg", monsterPos, System.currentTimeMillis(), Color.Yellow)
                                if (monsterHp <= 0) result = "win"
                            }
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("공격", color = Color.White)
                }
            }
        }

        // 결과 다이얼로그
        if (result != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(if (result == "win") "승리!" else "패배...") },
                text = {
                    Text(
                        if (result == "win") "${monster.name}을(를) 물리쳤습니다."
                        else "로봇이 파괴되었습니다. 개조를 강화하고 다시 도전하세요."
                    )
                },
                confirmButton = {
                    TextButton(onClick = onExit) { Text("돌아가기") }
                }
            )
        }
    }
}

data class FloatingText(val text: String, val pos: Offset, val createdAt: Long, val color: Color)

@Composable
fun Joystick(onDirectionChange: (Offset) -> Unit) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 50f

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = knobOffset + dragAmount
                        val dist = newOffset.getDistance()
                        knobOffset = if (dist > maxRadius) {
                            newOffset * (maxRadius / dist)
                        } else {
                            newOffset
                        }
                        onDirectionChange(Offset(knobOffset.x / maxRadius, knobOffset.y / maxRadius))
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onDirectionChange(Offset.Zero)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = (knobOffset.x / 3).dp, y = (knobOffset.y / 3).dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f))
        )
    }
}
package com.hjhong.steampunkgame

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.*

private const val ARENA_W = 800f
private const val ARENA_H = 500f
private const val PLAYER_RADIUS = 28f
private const val MONSTER_RADIUS = 32f
private const val MOVE_SPEED = 6f
private const val ATTACK_RANGE = PLAYER_RADIUS + MONSTER_RADIUS + 10f

private val PLAYER_ICON_SIZE = 56.dp
private val MONSTER_ICON_SIZE = 64.dp

private enum class BoardingPhase { BOARDING, READY, PLAYING }

@Composable
fun CombatScreen(
    monster: Monster,
    playerAttack: Int,
    playerDefense: Int,
    playerImagePath: String?,
    onExit: () -> Unit
) {
    var boardingPhase by remember { mutableStateOf(BoardingPhase.BOARDING) }
    var boardingProgress by remember { mutableStateOf(0f) }
    var screenShakeTrigger by remember { mutableStateOf(0) }
    var screenShakeIntensity by remember { mutableStateOf(8f) }
    var monsterHitTrigger by remember { mutableStateOf(0) }
    var playerHitTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val steps = 32
        repeat(steps) { i ->
            boardingProgress = (i + 1) / steps.toFloat()
            delay(35)
        }
        boardingPhase = BoardingPhase.READY
        delay(500)
        boardingPhase = BoardingPhase.PLAYING
    }

    var playerPos by remember { mutableStateOf(Offset(150f, ARENA_H / 2f)) }
    var monsterPos by remember { mutableStateOf(Offset(ARENA_W - 150f, ARENA_H / 2f)) }
    var joystickVector by remember { mutableStateOf(Offset.Zero) }
    var playerVelocity by remember { mutableStateOf(Offset.Zero) }

    val basePlayerHp = 100 + playerDefense * 2
    var playerHp by remember { mutableStateOf(basePlayerHp) }
    var monsterHp by remember { mutableStateOf(monster.hp) }

    var attackCooldown by remember { mutableStateOf(false) }
    var floatingTexts by remember { mutableStateOf(listOf<FloatingText>()) }

    var result by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(result, boardingPhase) {
        if (boardingPhase != BoardingPhase.PLAYING) return@LaunchedEffect
        var tickCount = 0
        while (result == null) {
            delay(16)
            tickCount++

            val targetVelocity = joystickVector * MOVE_SPEED
            val inertiaFactor = 0.15f
            playerVelocity = Offset(
                playerVelocity.x + (targetVelocity.x - playerVelocity.x) * inertiaFactor,
                playerVelocity.y + (targetVelocity.y - playerVelocity.y) * inertiaFactor
            )
            if (playerVelocity.getDistance() > 0.05f) {
                val newX = (playerPos.x + playerVelocity.x).coerceIn(PLAYER_RADIUS, ARENA_W - PLAYER_RADIUS)
                val newY = (playerPos.y + playerVelocity.y).coerceIn(PLAYER_RADIUS, ARENA_H - PLAYER_RADIUS)
                playerPos = Offset(newX, newY)
            }

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

            if (tickCount % 90 == 0 && dist <= ATTACK_RANGE) {
                val dmg = max(1, monster.attack - playerDefense / 2)
                playerHp = (playerHp - dmg).coerceAtLeast(0)
                floatingTexts = floatingTexts + FloatingText("-$dmg", playerPos, System.currentTimeMillis(), Color.Red)
                playerHitTrigger++
                screenShakeIntensity = (if (dmg >= basePlayerHp / 3) 16f else 8f)
                screenShakeTrigger++
                if (playerHp <= 0) result = "lose"
            }

            val now = System.currentTimeMillis()
            floatingTexts = floatingTexts.filter { now - it.createdAt < 800 }
        }
    }

    val shakeOffset by animateScreenShake(trigger = screenShakeTrigger, intensity = screenShakeIntensity)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffset.x.dp, y = shakeOffset.y.dp)
            .background(Color(0xFF0A0A0A))
    ) {
        if (boardingPhase == BoardingPhase.PLAYING) {
            HudCornerFrame()
        }

        if (boardingPhase != BoardingPhase.BOARDING) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "TARGET LOCK · ${monster.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        HudGaugeBar(
                            label = "적 장갑",
                            current = monsterHp,
                            max = monster.hp,
                            barColor = Color(0xFFE53935)
                        )
                        Spacer(Modifier.height(10.dp))
                        HudGaugeBar(
                            label = "로봇 장갑",
                            current = playerHp,
                            max = basePlayerHp,
                            barColor = Color(0xFFFFC107)
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(Color(0xFF1B1B1B))
                    ) {
                        val scaleX = maxWidth.value / ARENA_W
                        val scaleY = maxHeight.value / ARENA_H

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = ATTACK_RANGE * (size.width / ARENA_W),
                                center = Offset(playerPos.x * (size.width / ARENA_W), playerPos.y * (size.height / ARENA_H)),
                                style = Stroke(width = 2f)
                            )
                        }

                        GameEntity(
                            imagePath = playerImagePath,
                            fallbackColor = Color(0xFF2196F3),
                            size = PLAYER_ICON_SIZE,
                            xDp = playerPos.x * scaleX,
                            yDp = playerPos.y * scaleY,
                            hitTrigger = playerHitTrigger,
                            tintColor = Color.Red
                        )

                        GameEntity(
                            imagePath = monster.imagePath,
                            fallbackColor = Color(0xFFE53935),
                            size = MONSTER_ICON_SIZE,
                            xDp = monsterPos.x * scaleX,
                            yDp = monsterPos.y * scaleY,
                            hitTrigger = monsterHitTrigger,
                            tintColor = Color.Yellow
                        )

                        floatingTexts.forEach { ft ->
                            val elapsed = System.currentTimeMillis() - ft.createdAt
                            val alpha = (1f - elapsed / 800f).coerceIn(0f, 1f)
                            val yOffset = -(elapsed / 800f) * 40f
                            Text(
                                ft.text,
                                color = ft.color.copy(alpha = alpha),
                                modifier = Modifier.offset(
                                    x = (ft.pos.x * scaleX).dp,
                                    y = (ft.pos.y * scaleY + yOffset).dp
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Joystick(onDirectionChange = { v ->
                            if (boardingPhase == BoardingPhase.PLAYING) joystickVector = v
                        })

                        Button(
                            onClick = {
                                if (boardingPhase == BoardingPhase.PLAYING && !attackCooldown && result == null) {
                                    val dx2 = playerPos.x - monsterPos.x
                                    val dy2 = playerPos.y - monsterPos.y
                                    val dist2 = sqrt(dx2 * dx2 + dy2 * dy2)
                                    if (dist2 <= ATTACK_RANGE) {
                                        val dmg = max(1, playerAttack - monster.defense / 2)
                                        monsterHp = (monsterHp - dmg).coerceAtLeast(0)
                                        floatingTexts = floatingTexts + FloatingText("-$dmg", monsterPos, System.currentTimeMillis(), Color.Yellow)
                                        monsterHitTrigger++
                                        screenShakeIntensity = (if (dmg >= monster.hp / 3) 14f else 6f)
                                        screenShakeTrigger++
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
            }
        }

        if (boardingPhase != BoardingPhase.PLAYING) {
            BoardingOverlay(
                phase = boardingPhase,
                progress = boardingProgress,
                playerImagePath = playerImagePath
            )
        }

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
                confirmButton = { TextButton(onClick = onExit) { Text("돌아가기") } }
            )
        }
    }
}

@Composable
private fun BoardingOverlay(
    phase: BoardingPhase,
    progress: Float,
    playerImagePath: String?
) {
    val slideOffset by animateFloatAsState(
        targetValue = if (phase == BoardingPhase.READY) 0f else 300f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "slide"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .offset(y = slideOffset.dp)
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!playerImagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = playerImagePath,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                if (phase == BoardingPhase.READY) "전투 개시!" else "로봇 기동 중...",
                color = Color(0xFFFFC107),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(220.dp).height(6.dp),
                color = Color(0xFFFFC107),
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun GameEntity(
    imagePath: String?,
    fallbackColor: Color,
    size: androidx.compose.ui.unit.Dp,
    xDp: Float,
    yDp: Float,
    hitTrigger: Int = 0,
    tintColor: Color = Color.Red
) {
    val half = size.value / 2f

    var punchScale by remember { mutableStateOf(1f) }
    var flashAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(hitTrigger) {
        if (hitTrigger == 0) return@LaunchedEffect
        punchScale = 1.35f
        flashAlpha = 0.7f
        delay(70)
        punchScale = 1f
        flashAlpha = 0f
    }

    val animatedScale by animateFloatAsState(
        targetValue = punchScale,
        animationSpec = tween(durationMillis = 90),
        label = "punch"
    )
    val animatedFlash by animateFloatAsState(
        targetValue = flashAlpha,
        animationSpec = tween(durationMillis = 90),
        label = "flash"
    )

    Box(
        modifier = Modifier
            .offset(x = (xDp - half).dp, y = (yDp - half).dp)
            .size(size)
    ) {
        if (!imagePath.isNullOrBlank()) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                    .clip(CircleShape)
                    .background(fallbackColor)
            )
        }
        if (animatedFlash > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = animatedFlash))
            )
        }
    }
}

@Composable
fun animateScreenShake(trigger: Int, intensity: Float): State<Offset> {
    val shake = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val steps = listOf(intensity, -intensity * 0.7f, intensity * 0.4f, 0f)
        steps.forEach { v ->
            shake.value = Offset(v, 0f)
            delay(30)
        }
        shake.value = Offset.Zero
    }
    return shake
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
                        knobOffset = if (dist > maxRadius) newOffset * (maxRadius / dist) else newOffset
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

@Composable
fun HudGaugeBar(label: String, current: Int, max: Int, barColor: Color) {
    val ratio = (current.toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                "$current / $max",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ratio)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun HudCornerFrame() {
    val bracketColor = Color(0xFFFFC107).copy(alpha = 0.5f)
    val bracketSize = 28.dp
    val thickness = 3.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
            Box(Modifier.size(bracketSize, thickness).background(bracketColor))
            Box(Modifier.size(thickness, bracketSize).background(bracketColor))
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Box(Modifier.align(Alignment.TopEnd).size(bracketSize, thickness).background(bracketColor))
            Box(Modifier.align(Alignment.TopEnd).size(thickness, bracketSize).background(bracketColor))
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)) {
            Box(Modifier.align(Alignment.BottomStart).size(bracketSize, thickness).background(bracketColor))
            Box(Modifier.align(Alignment.BottomStart).size(thickness, bracketSize).background(bracketColor))
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
            Box(Modifier.align(Alignment.BottomEnd).size(bracketSize, thickness).background(bracketColor))
            Box(Modifier.align(Alignment.BottomEnd).size(thickness, bracketSize).background(bracketColor))
        }
    }
}
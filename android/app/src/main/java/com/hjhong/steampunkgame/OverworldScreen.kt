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
private const val COLLISION_RADIUS = 20f
private const val MAP_SPEED = 5f
private const val ENCOUNTER_DISTANCE_THRESHOLD = 350f
private const val ENCOUNTER_CHANCE = 0.4f

private const val TILE_SIZE = 60f
private const val MAP_COLS = 15
private const val MAP_ROWS = 10

private val MAP_LAYOUT = arrayOf(
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
    intArrayOf(1,0,0,0,0,0,1,0,0,0,0,0,0,0,1),
    intArrayOf(1,0,1,1,0,0,1,0,1,1,1,0,1,0,1),
    intArrayOf(1,0,1,1,0,0,0,0,1,0,0,0,1,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,1,0,1,0,0,0,1),
    intArrayOf(1,0,1,1,1,1,0,0,0,0,1,1,1,0,1),
    intArrayOf(1,0,0,0,0,1,0,0,1,0,0,0,1,0,1),
    intArrayOf(1,0,1,1,0,1,0,0,1,1,1,0,1,0,1),
    intArrayOf(1,0,0,0,0,0,0,0,0,0,0,0,0,0,1),
    intArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
)

private fun checkCollision(x: Float, y: Float, radius: Float): Boolean {
    if (x - radius < 0 || x + radius > MAP_W || y - radius < 0 || y + radius > MAP_H) {
        return true
    }

    val startCol = ((x - radius) / TILE_SIZE).toInt().coerceAtLeast(0)
    val endCol = ((x + radius) / TILE_SIZE).toInt().coerceAtMost(MAP_COLS - 1)
    val startRow = ((y - radius) / TILE_SIZE).toInt().coerceAtLeast(0)
    val endRow = ((y + radius) / TILE_SIZE).toInt().coerceAtMost(MAP_ROWS - 1)

    for (r in startRow..endRow) {
        for (c in startCol..endCol) {
            if (MAP_LAYOUT[r][c] == 1) {
                val tileLeft = c * TILE_SIZE
                val tileTop = r * TILE_SIZE
                val tileRight = tileLeft + TILE_SIZE
                val tileBottom = tileTop + TILE_SIZE

                val closestX = x.coerceIn(tileLeft, tileRight)
                val closestY = y.coerceIn(tileTop, tileBottom)

                val dx = x - closestX
                val dy = y - closestY

                if (dx * dx + dy * dy < radius * radius) return true
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
                val targetVelocity = joystickVector * MAP_SPEED
                val inertiaFactor = 0.1f

                trailerVelocity = Offset(
                    trailerVelocity.x + (targetVelocity.x - trailerVelocity.x) * inertiaFactor,
                    trailerVelocity.y + (targetVelocity.y - trailerVelocity.y) * inertiaFactor
                )

                if (trailerVelocity.getDistance() > 0.05f) {
                    val dx = trailerVelocity.x
                    val dy = trailerVelocity.y

                    val nextX = (trailerPos.x + dx).coerceIn(TRAILER_RADIUS, MAP_W - TRAILER_RADIUS)
                    val nextY = (trailerPos.y + dy).coerceIn(TRAILER_RADIUS, MAP_H - TRAILER_RADIUS)

                    val canMoveX = !checkCollision(nextX, trailerPos.y, COLLISION_RADIUS)
                    val finalX = if (canMoveX) nextX else trailerPos.x

                    val canMoveY = !checkCollision(finalX, nextY, COLLISION_RADIUS)
                    val finalY = if (canMoveY) nextY else trailerPos.y

                    val actualDx = finalX - trailerPos.x
                    val actualDy = finalY - trailerPos.y

                    trailerPos = Offset(finalX, finalY)

                    traveledSinceLastCheck += kotlin.math.sqrt(actualDx * actualDx + actualDy * actualDy)

                    if (traveledSinceLastCheck >= ENCOUNTER_DISTANCE_THRESHOLD) {
                        traveledSinceLastCheck = 0f
                        if (Random.nextFloat() < ENCOUNTER_CHANCE) {
                            encounterMonster = monsters.random()
                            isPaused = true
                            onEncounter(encounterMonster!!)
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "트레일러로 필드를 이동하세요 (장애물을 돌아서 이동할 수 있습니다)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color(0xFF1E261E))
        ) {
            val scaleX = maxWidth.value / MAP_W
            val scaleY = maxHeight.value / MAP_H

            Canvas(modifier = Modifier.fillMaxSize()) {
                val tileW = size.width / MAP_COLS
                val tileH = size.height / MAP_ROWS

                for (r in 0 until MAP_ROWS) {
                    for (c in 0 until MAP_COLS) {
                        val color = if (MAP_LAYOUT[r][c] == 1) {
                            Color(0xFF4A3B32)
                        } else {
                            Color(0xFF243024)
                        }

                        drawRect(
                            color = color,
                            topLeft = Offset(c * tileW, r * tileH),
                            size = androidx.compose.ui.geometry.Size(tileW, tileH)
                        )

                        drawRect(
                            color = Color.White.copy(alpha = 0.04f),
                            topLeft = Offset(c * tileW, r * tileH),
                            size = androidx.compose.ui.geometry.Size(tileW, tileH),
                            style = Stroke(width = 1f)
                        )
                    }
                }

                drawCircle(
                    color = Color.Yellow,
                    radius = TRAILER_RADIUS * scaleX,
                    center = Offset(trailerPos.x * scaleX, trailerPos.y * scaleY)
                )
            }
        }
    }
}

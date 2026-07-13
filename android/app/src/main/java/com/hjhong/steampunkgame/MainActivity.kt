package com.hjhong.steampunkgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    private val viewModel: MonsterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val monsters by viewModel.monsters.collectAsState()
                val error by viewModel.error.collectAsState()

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when {
                        error != null -> Text("에러: $error")
                        monsters.isEmpty() -> Text("몬스터 데이터 없음 (로딩 중이거나 0건)")
                        else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            items(monsters) { m ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = m.imagePath,
                                            contentDescription = m.name,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(m.name, style = MaterialTheme.typography.titleMedium)
                                            Text("Lv.${m.level}  HP ${m.hp}  ATK ${m.attack}")
                                        }
                                    }

                                    if (m.scripture.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 76.dp, end = 8.dp)
                                        ) {
                                            Text(
                                                "\u201C${m.scripture}\u201D",
                                                fontStyle = FontStyle.Italic,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (m.scriptureRef.isNotBlank()) {
                                                Text(
                                                    "— ${m.scriptureRef}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(top = 10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
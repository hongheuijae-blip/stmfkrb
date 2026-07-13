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
    private val monsterViewModel: MonsterViewModel by viewModels()
    private val weaponViewModel: WeaponViewModel by viewModels()
    private val cityViewModel: CityViewModel by viewModels()
    private val questViewModel: QuestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("몬스터", "무기", "도시", "퀘스트")

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> MonsterScreen(monsterViewModel)
                            1 -> WeaponScreen(weaponViewModel)
                            2 -> CityScreen(cityViewModel)
                            3 -> QuestScreen(questViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonsterScreen(viewModel: MonsterViewModel) {
    val monsters by viewModel.monsters.collectAsState()
    val error by viewModel.error.collectAsState()

    EmptyableList(items = monsters, error = error, emptyText = "몬스터 데이터 없음") { m ->
        ItemCard(
            title = m.name,
            subtitle = "Lv.${m.level}  HP ${m.hp}  ATK ${m.attack}",
            imagePath = m.imagePath,
            scripture = m.scripture,
            scriptureRef = m.scriptureRef
        )
    }
}

@Composable
fun WeaponScreen(viewModel: WeaponViewModel) {
    val weapons by viewModel.weapons.collectAsState()
    val error by viewModel.error.collectAsState()

    EmptyableList(items = weapons, error = error, emptyText = "무기 데이터 없음") { w ->
        ItemCard(
            title = w.name,
            subtitle = "${w.type} · ${w.rarity} · ATK ${w.attack}",
            imagePath = w.imagePath,
            scripture = w.scripture,
            scriptureRef = w.scriptureRef,
            description = w.description
        )
    }
}

@Composable
fun CityScreen(viewModel: CityViewModel) {
    val cities by viewModel.cities.collectAsState()
    val error by viewModel.error.collectAsState()

    EmptyableList(items = cities, error = error, emptyText = "도시 데이터 없음") { c ->
        ItemCard(
            title = c.name,
            subtitle = "${c.region} · 인구 ${c.population}",
            imagePath = c.imagePath,
            scripture = c.scripture,
            scriptureRef = c.scriptureRef,
            description = c.description
        )
    }
}

@Composable
fun QuestScreen(viewModel: QuestViewModel) {
    val quests by viewModel.quests.collectAsState()
    val error by viewModel.error.collectAsState()

    EmptyableList(items = quests, error = error, emptyText = "퀘스트 데이터 없음") { q ->
        ItemCard(
            title = q.title,
            subtitle = "난이도 ${q.difficulty}  보상 EXP ${q.rewardExp}",
            imagePath = null,
            scripture = q.scripture,
            scriptureRef = q.scriptureRef,
            description = q.description
        )
    }
}

@Composable
fun <T> EmptyableList(
    items: List<T>,
    error: String?,
    emptyText: String,
    itemContent: @Composable (T) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            error != null -> Text("에러: $error")
            items.isEmpty() -> Text(emptyText)
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(items) { item -> itemContent(item) }
            }
        }
    }
}

@Composable
fun ItemCard(
    title: String,
    subtitle: String,
    imagePath: String?,
    scripture: String,
    scriptureRef: String,
    description: String = ""
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!imagePath.isNullOrBlank()) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = title,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = if (imagePath.isNullOrBlank()) 0.dp else 76.dp)
            )
        }

        if (scripture.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (imagePath.isNullOrBlank()) 0.dp else 76.dp, end = 8.dp)
            ) {
                Text(
                    "\u201C${scripture}\u201D",
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodySmall
                )
                if (scriptureRef.isNotBlank()) {
                    Text(
                        "— ${scriptureRef}",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(top = 10.dp))
    }
}
package com.hjhong.steampunkgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    private val monsterViewModel: MonsterViewModel by viewModels()
    private val weaponViewModel: WeaponViewModel by viewModels()
    private val cityViewModel: CityViewModel by viewModels()
    private val questViewModel: QuestViewModel by viewModels()
    private val robotViewModel: RobotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("몬스터", "무기", "도시", "퀘스트", "로봇")

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
                            4 -> RobotScreen(robotViewModel)
                        }
                    }
                }
            }
        }
    }
}

// ───────────── 몬스터 ─────────────

@Composable
fun MonsterScreen(viewModel: MonsterViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "list") {
        composable("list") {
            val monsters by viewModel.monsters.collectAsState()
            val error by viewModel.error.collectAsState()
            EmptyableList(items = monsters, error = error, emptyText = "몬스터 데이터 없음") { m ->
                ItemCard(
                    title = m.name,
                    subtitle = "Lv.${m.level}  HP ${m.hp}  ATK ${m.attack}",
                    imagePath = m.imagePath,
                    onClick = { navController.navigate("detail/${m.id}") }
                )
            }
        }
        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val monster = viewModel.monsters.collectAsState().value.find { it.id == id }
            if (monster != null) {
                DetailScreen(
                    title = monster.name,
                    imagePath = monster.imagePath,
                    stats = listOf(
                        "레벨" to "${monster.level}",
                        "HP" to "${monster.hp}",
                        "공격력" to "${monster.attack}",
                        "방어력" to "${monster.defense}",
                        "속성" to monster.element
                    ),
                    description = monster.lore,
                    scripture = monster.scripture,
                    scriptureRef = monster.scriptureRef,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ───────────── 무기 ─────────────

@Composable
fun WeaponScreen(viewModel: WeaponViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "list") {
        composable("list") {
            val weapons by viewModel.weapons.collectAsState()
            val error by viewModel.error.collectAsState()
            EmptyableList(items = weapons, error = error, emptyText = "무기 데이터 없음") { w ->
                ItemCard(
                    title = w.name,
                    subtitle = "${w.type} · ${w.rarity} · ATK ${w.attack}",
                    imagePath = w.imagePath,
                    onClick = { navController.navigate("detail/${w.id}") }
                )
            }
        }
        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val weapon = viewModel.weapons.collectAsState().value.find { it.id == id }
            if (weapon != null) {
                DetailScreen(
                    title = weapon.name,
                    imagePath = weapon.imagePath,
                    stats = listOf(
                        "종류" to weapon.type,
                        "희귀도" to weapon.rarity,
                        "공격력" to "${weapon.attack}"
                    ),
                    description = weapon.description,
                    scripture = weapon.scripture,
                    scriptureRef = weapon.scriptureRef,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ───────────── 도시 ─────────────

@Composable
fun CityScreen(viewModel: CityViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "list") {
        composable("list") {
            val cities by viewModel.cities.collectAsState()
            val error by viewModel.error.collectAsState()
            EmptyableList(items = cities, error = error, emptyText = "도시 데이터 없음") { c ->
                ItemCard(
                    title = c.name,
                    subtitle = "${c.region} · 인구 ${c.population}",
                    imagePath = c.imagePath,
                    onClick = { navController.navigate("detail/${c.id}") }
                )
            }
        }
        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val city = viewModel.cities.collectAsState().value.find { it.id == id }
            if (city != null) {
                DetailScreen(
                    title = city.name,
                    imagePath = city.imagePath,
                    stats = listOf(
                        "지역" to city.region,
                        "인구" to "${city.population}"
                    ),
                    description = city.description,
                    scripture = city.scripture,
                    scriptureRef = city.scriptureRef,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ───────────── 퀘스트 ─────────────

@Composable
fun QuestScreen(viewModel: QuestViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "list") {
        composable("list") {
            val quests by viewModel.quests.collectAsState()
            val error by viewModel.error.collectAsState()
            EmptyableList(items = quests, error = error, emptyText = "퀘스트 데이터 없음") { q ->
                ItemCard(
                    title = q.title,
                    subtitle = "난이도 ${q.difficulty}  보상 EXP ${q.rewardExp}",
                    imagePath = null,
                    onClick = { navController.navigate("detail/${q.id}") }
                )
            }
        }
        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val quest = viewModel.quests.collectAsState().value.find { it.id == id }
            if (quest != null) {
                DetailScreen(
                    title = quest.title,
                    imagePath = null,
                    stats = listOf(
                        "난이도" to "${quest.difficulty}",
                        "보상 EXP" to "${quest.rewardExp}"
                    ),
                    description = quest.description,
                    scripture = quest.scripture,
                    scriptureRef = quest.scriptureRef,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ───────────── 로봇 개조/장착 ─────────────

@Composable
fun RobotScreen(viewModel: RobotViewModel) {
    val parts by viewModel.parts.collectAsState()
    val equipped by viewModel.equipped.collectAsState()
    val error by viewModel.error.collectAsState()

    if (error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("에러: $error")
        }
        return
    }

    if (parts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로봇 파츠 데이터 없음")
        }
        return
    }

    val slots = parts.map { it.slot }.distinct()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("현재 로봇 스탯", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("총 공격력 보너스: +${viewModel.totalAttack()}")
                    Text("총 방어력 보너스: +${viewModel.totalDefense()}")
                }
            }
        }

        slots.forEach { slot ->
            item {
                Text(
                    "슬롯: $slot",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            val slotParts = parts.filter { it.slot == slot }
            items(slotParts) { part ->
                val isEquipped = equipped[slot] == part.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isEquipped) viewModel.unequip(slot) else viewModel.equip(slot, part.id)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (part.imagePath.isNotBlank()) {
                        AsyncImage(
                            model = part.imagePath,
                            contentDescription = part.name,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(part.name, style = MaterialTheme.typography.bodyLarge)
                        Text("ATK +${part.bonusAttack}  DEF +${part.bonusDefense}")
                        if (part.specialEffect.isNotBlank()) {
                            Text(part.specialEffect, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (isEquipped) {
                        Text("장착중", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
        }
    }
}

// ───────────── 공용 컴포넌트 ─────────────

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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
    ) {
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
        Divider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
fun DetailScreen(
    title: String,
    imagePath: String?,
    stats: List<Pair<String, String>>,
    description: String,
    scripture: String,
    scriptureRef: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
            }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(12.dp))

        if (!imagePath.isNullOrBlank()) {
            AsyncImage(
                model = imagePath,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        stats.forEach { (label, value) ->
            if (value.isNotBlank()) {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("$label: ", fontWeight = FontWeight.SemiBold)
                    Text(value)
                }
            }
        }

        if (description.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }

        if (scripture.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            Text(
                "\u201C${scripture}\u201D",
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyLarge
            )
            if (scriptureRef.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "— ${scriptureRef}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
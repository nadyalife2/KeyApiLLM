package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.AggregatorEntity
import com.example.database.MyApiEntity
import com.example.database.OwnAccountProviderEntity
import com.example.database.ReadyKeyEntity
import com.example.security.SecurityManager
import com.example.settings.SettingsManager
import com.example.viewmodel.FreeLlmHubViewModel
import com.example.viewmodel.Screens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Modifier.comicShadow(
    color: Color = Color(0xFF1E1E1E),
    offsetX: androidx.compose.ui.unit.Dp = 4.dp,
    offsetY: androidx.compose.ui.unit.Dp = 4.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 12.dp
) = this.drawBehind {
    val xOffset = offsetX.toPx()
    val yOffset = offsetY.toPx()
    val r = cornerRadius.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(xOffset, yOffset),
        size = androidx.compose.ui.geometry.Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
}

@Composable
fun PortalStitchDivider() {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF2EDD3E), Color(0xFF00E5FF), Color(0xFF2EDD3E))
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFF1E1E1E))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeLlmHubRoot(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    var rootWarningDismissed by remember { mutableStateOf(false) }

    // Dynamic screenshot protection & vault concealment in task switcher (FLAG_SECURE)
    val enableScreenshots = remember(context, viewModel.activeScreen) {
        SettingsManager.getEnableScreenshots(context)
    }
    val isSecretScreen = when (viewModel.activeScreen) {
        is Screens.MyApiDetail -> true
        is Screens.CreateEditMyApi -> true
        is Screens.BackupCenter -> true
        is Screens.ReadyKeyDetail -> true
        else -> false
    }
    val shouldBeSecure = !enableScreenshots || isSecretScreen

    DisposableEffect(shouldBeSecure) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            if (shouldBeSecure) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        onDispose {
            // Restore default safety when app exits or is updated
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Крипто Сейф",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    "AI LLM API Хаб",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                                if (viewModel.isSyncing) {
                                    Text(
                                        viewModel.syncStatusText,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        val screen = viewModel.activeScreen
                        when (screen) {
                            is Screens.ReadyKeysList -> {
                                TextButton(
                                    onClick = { viewModel.startSyncReadyKeys(context) },
                                    shape = RoundedCornerShape(0.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("appbar_sync_readykeys")
                                ) {
                                    Text("ОБНОВИТЬ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            is Screens.AggregatorsList -> {
                                TextButton(
                                    onClick = { viewModel.startSyncAggregators(context) },
                                    shape = RoundedCornerShape(0.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("appbar_sync_aggregators")
                                ) {
                                    Text("ОБНОВИТЬ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            is Screens.OwnAccountList -> {
                                TextButton(
                                    onClick = { viewModel.startSyncOwnAccount(context) },
                                    shape = RoundedCornerShape(0.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("appbar_sync_ownaccount")
                                ) {
                                    Text("ОБНОВИТЬ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                            else -> {}
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.navigateTo(Screens.SearchScreen) },
                            modifier = Modifier.testTag("appbar_search_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                        IconButton(
                            onClick = { viewModel.navigateTo(Screens.Settings) },
                            modifier = Modifier.testTag("appbar_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                PortalStitchDivider()
            }
        },
        bottomBar = {
            if (isRootScreen(viewModel.activeScreen)) {
                FreeLlmHubBottomBar(
                    activeScreen = viewModel.activeScreen,
                    onNavigate = { screen ->
                        viewModel.globalSearchQuery = ""
                        viewModel.filterProvider = ""
                        viewModel.filterCategory = "ALL"
                        viewModel.filterCardRequired = null
                        viewModel.navigateTo(screen)
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!rootWarningDismissed && SecurityManager.isDeviceRooted()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Устройство уязвимо",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Обнаружен Root-доступ!",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Это устройство рутировано. Локальные секреты Keystore могут быть скомпрометированы. Регулярно очищайте буфер обмена.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = { rootWarningDismissed = true }) {
                            Icon(Icons.Default.Check, contentDescription = "Скрыть", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val current = viewModel.activeScreen) {
                    is Screens.ReadyKeysList -> ReadyKeysListScreen(viewModel)
                    is Screens.ReadyKeyDetail -> ReadyKeyDetailScreen(current.id, viewModel)
                    is Screens.AggregatorsList -> AggregatorsListScreen(viewModel)
                    is Screens.AggregatorDetail -> AggregatorDetailScreen(current.id, viewModel)
                    is Screens.OwnAccountList -> OwnAccountListScreen(viewModel)
                    is Screens.OwnAccountDetail -> OwnAccountDetailScreen(current.id, viewModel)
                    is Screens.MyApisList -> MyApisListScreen(viewModel)
                    is Screens.MyApiDetail -> MyApiDetailScreen(current.id, viewModel)
                    is Screens.CreateEditMyApi -> CreateEditMyApiScreen(current, viewModel)
                    is Screens.ProjectsList -> ProjectsListScreen(viewModel)
                    is Screens.Settings -> SettingsScreen(viewModel)
                    is Screens.SyncHistory -> SyncHistoryScreen(viewModel)
                    is Screens.ParserWarnings -> ParserWarningsScreen(viewModel)
                    is Screens.BackupCenter -> BackupCenterScreen(viewModel)
                    is Screens.SearchScreen -> SearchScreen(viewModel)
                }

                if (isRootScreen(viewModel.activeScreen)) {
                    RickAndMortyCompanionsWidget()
                }
            }
        }
    }
}

private fun isRootScreen(screen: Screens): Boolean {
    return screen is Screens.ReadyKeysList ||
            screen is Screens.AggregatorsList ||
            screen is Screens.OwnAccountList ||
            screen is Screens.MyApisList
}

@Composable
fun lebedevTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFFFBFBFB), // Soft clean off-white container
    unfocusedContainerColor = Color(0xFFFFFFFF), // Pure white main canvas
    disabledContainerColor = Color(0xFFF5F5F5),
    focusedBorderColor = Color(0xFF2EDD3E), // Vibrant Neon Portal Green
    unfocusedBorderColor = Color(0xFF00E5FF), // Electric Schwifty Turquoise
    focusedLabelColor = Color(0xFF2EDD3E),
    unfocusedLabelColor = Color(0xFF00C8D7)
)

@Composable
fun FreeLlmHubBottomBar(activeScreen: Screens, onNavigate: (Screens) -> Unit) {
    Column {
        PortalStitchDivider()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1E1E1E),
                selectedTextColor = Color(0xFF2EDD3E),
                unselectedIconColor = Color(0xFF7E7E7E),
                unselectedTextColor = Color(0xFF7E7E7E),
                indicatorColor = Color(0xFF00E5FF).copy(alpha = 0.3f)
            )
            NavigationBarItem(
                selected = activeScreen is Screens.ReadyKeysList,
                onClick = { onNavigate(Screens.ReadyKeysList) },
                label = { Text("Общие ключи", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.List, contentDescription = "Общие ключи") },
                colors = itemColors,
                modifier = Modifier.testTag("nav_ready_keys")
            )
            NavigationBarItem(
                selected = activeScreen is Screens.AggregatorsList,
                onClick = { onNavigate(Screens.AggregatorsList) },
                label = { Text("Прокси", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Share, contentDescription = "Прокси") },
                colors = itemColors,
                modifier = Modifier.testTag("nav_aggregators")
            )
            NavigationBarItem(
                selected = activeScreen is Screens.OwnAccountList,
                onClick = { onNavigate(Screens.OwnAccountList) },
                label = { Text("Регистрация", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Info, contentDescription = "Регистрация") },
                colors = itemColors,
                modifier = Modifier.testTag("nav_own_account")
            )
            NavigationBarItem(
                selected = activeScreen is Screens.MyApisList,
                onClick = { onNavigate(Screens.MyApisList) },
                label = { Text("Мой Сейф", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Lock, contentDescription = "Мой Сейф") },
                colors = itemColors,
                modifier = Modifier.testTag("nav_my_apis")
            )
        }
    }
}

// ----------------------------------------------------
// SCREEN 1: ReadyKeysListScreen
// ----------------------------------------------------
@Composable
fun ReadyKeysListScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val keys by viewModel.readyKeys.collectAsState()

    val filteredKeys = remember(keys, viewModel.globalSearchQuery) {
        if (viewModel.globalSearchQuery.isBlank()) {
            keys
        } else {
            keys.filter {
                it.provider.contains(viewModel.globalSearchQuery, true) ||
                        it.models.any { model -> model.contains(viewModel.globalSearchQuery, true) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("База общих ключей", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Каталог готовых ключей, обновляемый сообществом", fontSize = 12.sp, color = Color.Gray)
        }

        OutlinedTextField(
            value = viewModel.globalSearchQuery,
            onValueChange = { viewModel.globalSearchQuery = it },
            placeholder = { Text("Поиск ключей по провайдерам или моделям...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            colors = lebedevTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("readykeys_search"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredKeys.isEmpty()) {
            EmptyListState("Ключи не найдены", "Попробуйте изменить параметры поиска или нажмите кнопку 'Обновить' выше.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredKeys) { key ->
                    ReadyKeyCard(key = key, onClick = {
                        viewModel.navigateTo(Screens.ReadyKeyDetail(key.id))
                    })
                }
            }
        }
    }
}

@Composable
fun ReadyKeyCard(key: ReadyKeyEntity, onClick: () -> Unit) {
    val statusShadowColor = when (key.healthStatus) {
        "HEALTHY" -> Color(0xFF2EDD3E) // Neon Portal Green
        "UNHEALTHY" -> Color(0xFFE10612) // Toxic Red
        else -> Color(0xFF00E5FF) // Electric Schwifty Turquoise
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp, end = 5.dp)
            .comicShadow(color = statusShadowColor)
            .clickable(onClick = onClick)
            .testTag("ready_key_card_${key.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Color(0xFF1E1E1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        key.provider,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Общий",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    key.endpoint ?: "Стандартный глобальный эндпоинт",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = "Лимиты", modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        key.budget ?: "Лимиты не установлены",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val healthLabel = when (key.healthStatus) {
                    "HEALTHY" -> "АКТИВЕН"
                    "UNHEALTHY" -> "НЕАКТИВЕН"
                    else -> "НЕ ПРОВЕРЕН"
                }
                Text(
                    healthLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = getHealthColor(key.healthStatus),
                    modifier = Modifier
                        .background(getHealthColor(key.healthStatus).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 2: ReadyKeyDetailScreen
// ----------------------------------------------------
@Composable
fun ReadyKeyDetailScreen(id: String, viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val keys by viewModel.readyKeys.collectAsState()
    val keyObj = keys.find { it.id == id }

    if (keyObj == null) {
        DetailPlaceholder(onBack = { viewModel.navigateBack() })
        return
    }

    var keyRevealed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.testTag("readykey_detail_back")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Детали публичного ключа", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(keyObj.provider, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Источник: ${keyObj.sourceRepo}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                Divider()

                Spacer(modifier = Modifier.height(12.dp))
                DetailRow("Сервер эндпоинта", keyObj.endpoint ?: "Стандартный URL")
                DetailRow("Лимиты квоты", keyObj.budget ?: "Без стабильных ограничений")
                DetailRow("RPM скорость", keyObj.rpm ?: "Н/Д")
                val freshnessStr = when (keyObj.freshnessStatus) {
                    "FRESH" -> "Свежий"
                    "STALE" -> "Старый"
                    "OUTDATED" -> "Устаревший"
                    else -> "Неизвестно"
                }
                DetailRow("Свежесть ключа", freshnessStr)
                val riskStr = when (keyObj.riskLevel) {
                    "LOW" -> "Низкий"
                    "MEDIUM" -> "Средний"
                    "HIGH" -> "Высокий"
                    else -> "Низкий"
                }
                DetailRow("Уровень риска", riskStr)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Учетные данные", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (keyRevealed) (keyObj.apiKeyEncryptedTemp ?: "") else keyObj.apiKeyMasked,
                    onValueChange = {},
                    readOnly = true,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { keyRevealed = !keyRevealed }) {
                            Text(if (keyRevealed) "Скрыть" else "Показать", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            SecurityManager.copyToClipboard(context, keyObj.apiKeyEncryptedTemp ?: "")
                            Toast.makeText(context, "Скопировано! Очистка буфера через 60 секунд для безопасности.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF1E1E1E)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1E1E)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp, end = 4.dp)
                            .comicShadow(color = Color(0xFF00E5FF))
                            .testTag("readykey_copy")
                    ) {
                        Text("Копировать", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val text = keyObj.apiKeyEncryptedTemp ?: ""
                            try {
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                val chooser = android.content.Intent.createChooser(shareIntent, "Поделиться ключом")
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                SecurityManager.copyToClipboard(context, text)
                                Toast.makeText(context, "Обмен недоступен. Ключ скопирован в буфер обмена.", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF1E1E1E)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1E1E)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp, end = 4.dp)
                            .comicShadow(color = Color(0xFF2EDD3E))
                            .testTag("readykey_share")
                    ) {
                        Text("Поделиться", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.testReadyKeyHealth(keyObj.id, context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF1E1E1E)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1E1E)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp, end = 4.dp)
                            .comicShadow(color = Color(0xFF00BFA5))
                            .testTag("readykey_check")
                    ) {
                        if (viewModel.activeCheckId == keyObj.id) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF1E1E1E), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Проверить", modifier = Modifier.size(16.dp), tint = Color(0xFF2EDD3E))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Проверить", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.importReadyKeyToVault(keyObj, context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF1E1E1E)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1E1E)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp, end = 4.dp)
                            .comicShadow(color = Color(0xFF2EDD3E))
                            .testTag("readykey_import")
                    ) {
                        Text("В Сейф", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Внимание: публичные ключи распределяются в сообществе бесплатно. Никогда не используйте эти ключи для критических или рабочих нагрузок.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

// ----------------------------------------------------
// SCREEN 3: AggregatorsListScreen
// ----------------------------------------------------
@Composable
fun AggregatorsListScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val proxies by viewModel.aggregators.collectAsState()

    val filtered = remember(proxies, viewModel.globalSearchQuery) {
        if (viewModel.globalSearchQuery.isBlank()) {
            proxies
        } else {
            proxies.filter {
                it.name.contains(viewModel.globalSearchQuery, true) ||
                        it.providersList.any { prov -> prov.contains(viewModel.globalSearchQuery, true) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("AI Прокси и Агрегаторы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Серверы, совместимые с OpenAI API структурой", fontSize = 12.sp, color = Color.Gray)
        }

        OutlinedTextField(
            value = viewModel.globalSearchQuery,
            onValueChange = { viewModel.globalSearchQuery = it },
            placeholder = { Text("Поиск прокси или провайдеров...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Фильтр") },
            colors = lebedevTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("aggregators_search"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyListState("Список прокси пуст", "Нажмите кнопку 'Обновить' выше для загрузки актуальных данных из сети сообщества.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered) { agg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp, end = 5.dp)
                            .comicShadow(color = Color(0xFF00E5FF))
                            .clickable { viewModel.navigateTo(Screens.AggregatorDetail(agg.id)) }
                            .testTag("agg_card_${agg.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(agg.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(8.dp))
                                if (agg.selfHosted) {
                                    Text(
                                        "Docker",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(agg.description ?: "", maxLines = 2, fontSize = 12.sp, overflow = TextOverflow.Ellipsis, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                agg.providersList.take(3).forEach { prov ->
                                    Text(
                                        prov,
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier
                                            .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 4: AggregatorDetailScreen
// ----------------------------------------------------
@Composable
fun AggregatorDetailScreen(id: String, viewModel: FreeLlmHubViewModel) {
    val proxies by viewModel.aggregators.collectAsState()
    val agg = proxies.find { it.id == id }

    if (agg == null) {
        DetailPlaceholder(onBack = { viewModel.navigateBack() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Характеристики прокси", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(agg.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Проект на GitHub: ${agg.githubUrl}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Параметры развертывания", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Базовый URL эндпоинта", agg.endpointBaseUrl ?: "Параметр self-hosted")
                DetailRow("Обязателен селф-хостинг", if (agg.selfHosted) "Да" else "Нет")
                DetailRow("Требуются свои ключи API", if (agg.requiresOwnKeys) "Да" else "Нет")
                DetailRow("Требуется запуск Docker", if (agg.dockerRequired) "Да" else "Нет")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Поддерживаемые провайдеры", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(agg.providersList.joinToString(", "), fontSize = 13.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (agg.endpointBaseUrl != null) {
            Button(
                onClick = {
                    viewModel.navigateTo(
                        Screens.CreateEditMyApi(
                            editingId = null,
                            prefillKey = "",
                            prefillProviderName = agg.name,
                            prefillEndpoint = agg.endpointBaseUrl
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Использовать прокси")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Импортировать эндпоинт в Сейф")
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 5: OwnAccountListScreen
// ----------------------------------------------------
@Composable
fun OwnAccountListScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val freeTiers by viewModel.ownAccountProviders.collectAsState()

    val filtered = remember(freeTiers, viewModel.globalSearchQuery) {
        if (viewModel.globalSearchQuery.isBlank()) {
            freeTiers
        } else {
            freeTiers.filter {
                it.providerName.contains(viewModel.globalSearchQuery, true) ||
                        it.modelsAvailable.any { m -> m.contains(viewModel.globalSearchQuery, true) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("Получение бесплатных ключей", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Способы официальной бесплатной регистрации", fontSize = 12.sp, color = Color.Gray)
        }

        OutlinedTextField(
            value = viewModel.globalSearchQuery,
            onValueChange = { viewModel.globalSearchQuery = it },
            placeholder = { Text("Поиск провайдеров или триалов...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
            colors = lebedevTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("ownaccount_search"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Инструкция по получению личных API ключей:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.Top) {
                    Text("• ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("Выберите любого официального провайдера из списка ниже (например, Google AI Studio с бесплатными лимитами);", fontSize = 11.sp, color = Color.Gray)
                }
                Row(modifier = Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.Top) {
                    Text("• ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("Перейдите по прямой ссылке, указанной на экране деталей провайдера, для быстрой регистрации и выпуска API ключа;", fontSize = 11.sp, color = Color.Gray)
                }
                Row(modifier = Modifier.padding(vertical = 1.dp), verticalAlignment = Alignment.Top) {
                    Text("• ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text("Скопируйте полученный ключ и вернитесь в Сейф для безопасного локального хранения и тестирования.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyListState("Объединение и очистка триалов...", "Нажмите кнопку 'Объединить' выше, чтобы загрузить инструкции по бесплатной регистрации ключей.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered) { item ->
                    val shadowColor = if (item.cardRequired) Color(0xFF00E5FF) else Color(0xFF2EDD3E)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 5.dp, end = 5.dp)
                            .comicShadow(color = shadowColor)
                            .clickable { viewModel.navigateTo(Screens.OwnAccountDetail(item.id)) }
                            .testTag("own_card_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.providerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (item.cardRequired) "Нужна карта" else "Без карты",
                                    fontSize = 9.sp,
                                    color = if (item.cardRequired) Color.Red else Color.Green,
                                    modifier = Modifier
                                        .background(if (item.cardRequired) Color.Red.copy(alpha = 0.1f) else Color.Green.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(item.freeTier ?: "Подробности о бесплатном тарифе", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Надежность: ${item.confidenceLevel}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 6: OwnAccountDetailScreen
// ----------------------------------------------------
@Composable
fun OwnAccountDetailScreen(id: String, viewModel: FreeLlmHubViewModel) {
    val freeTiers by viewModel.ownAccountProviders.collectAsState()
    val item = freeTiers.find { it.id == id }

    if (item == null) {
        DetailPlaceholder(onBack = { viewModel.navigateBack() })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Детали предложения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(item.providerName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Подтверждено в ${item.sourceCount} источниках списков", fontSize = 11.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Бесплатный лимит", item.freeTier ?: "Условия по умолчанию")
                DetailRow("Требуется платёжная карта", if (item.cardRequired) "Да (Обязательно)" else "Нет")
                DetailRow("Срок действия", item.duration ?: "Без ограничений")
                DetailRow("Оценка доверия сообщества", item.confidenceLevel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Прямая ссылка на регистрацию", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.registrationUrl ?: "https://platform.openai.com", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.navigateTo(
                    Screens.CreateEditMyApi(
                        editingId = null,
                        prefillKey = "",
                        prefillProviderName = item.providerName,
                        prefillEndpoint = ""
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить этот провайдер в Сейф")
        }
    }
}

// ----------------------------------------------------
// SCREEN 7: MyApisListScreen (THE PERSONAL VAULT)
// ----------------------------------------------------
@Composable
fun MyApisListScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val apis by viewModel.myApis.collectAsState()
    val projects by viewModel.projects.collectAsState()
    
    var selectedProjectFilter by remember { mutableStateOf<String?>(null) }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }

    val allTags = remember(apis) {
        apis.flatMap { it.tags }.distinct().filter { it.isNotBlank() }
    }

    val filteredList = remember(apis, viewModel.globalSearchQuery, selectedProjectFilter, selectedTagFilter) {
        var res = apis
        if (selectedProjectFilter != null) {
            res = res.filter { it.projectId == selectedProjectFilter }
        }
        if (selectedTagFilter != null) {
            res = res.filter { it.tags.contains(selectedTagFilter) }
        }
        if (viewModel.globalSearchQuery.isNotBlank()) {
            res = res.filter {
                it.providerName.contains(viewModel.globalSearchQuery, true) ||
                        it.tags.any { tag -> tag.contains(viewModel.globalSearchQuery, true) }
            }
        }
        res
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screens.CreateEditMyApi()) },
                modifier = Modifier.testTag("fab_add_api")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить API ключ")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Мой крипто-сейф", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("API ключи надежно шифруются локально на вашем устройстве", fontSize = 11.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (filteredList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                try {
                                    val sharedText = filteredList.joinToString("\n\n") { api ->
                                        val revealed = SecurityManager.decrypt(api.apiKeyEncrypted)
                                        val ep = api.baseUrl ?: "https://api.openai.com/v1"
                                        val tagsStr = if (api.tags.isNotEmpty()) " #${api.tags.joinToString(" #")}" else ""
                                        "Провайдер: ${api.providerName}\nКлюч: $revealed\nЭндпоинт: $ep$tagsStr"
                                    }
                                    try {
                                        val sendIntent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, sharedText)
                                            type = "text/plain"
                                        }
                                        val chooser = android.content.Intent.createChooser(sendIntent, "Поделиться ключами")
                                        context.startActivity(chooser)
                                    } catch (err: Exception) {
                                        SecurityManager.copyToClipboard(context, sharedText, "API Keys List")
                                        Toast.makeText(context, "Обмен недоступен. Весь список ключей скопирован в буфер обмена.", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Ошибка перед отправкой: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("vault_share_all_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться отфильтрованными ключами", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.navigateTo(Screens.BackupCenter) }) {
                        Icon(Icons.Default.Lock, contentDescription = "Резервные копии", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.globalSearchQuery,
                onValueChange = { viewModel.globalSearchQuery = it },
                placeholder = { Text("Поиск в Сейфе по названию или тегам...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                colors = lebedevTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("vault_search"),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedProjectFilter == null,
                    onClick = { selectedProjectFilter = null },
                    label = { Text("Все ключи") }
                )
                projects.forEach { proj ->
                    FilterChip(
                        selected = selectedProjectFilter == proj.id,
                        onClick = { selectedProjectFilter = proj.id },
                        label = { Text(proj.name) }
                    )
                }
                IconButton(
                    onClick = { viewModel.navigateTo(Screens.ProjectsList) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Управление проектами", tint = Color.Gray)
                }
            }

            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.List, contentDescription = "Теги", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text("Теги:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    FilterChip(
                        selected = selectedTagFilter == null,
                        onClick = { selectedTagFilter = null },
                        label = { Text("Все теги", fontSize = 10.sp) }
                    )
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTagFilter == tag,
                            onClick = { selectedTagFilter = tag },
                            label = { Text("#$tag", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                EmptyListState("Сейф пуст", "Добавьте ваш первый API ключ по кнопке '+' внизу экрана или импортируйте из общедоступного каталога.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredList) { api ->
                        val statusShadowColor = when (api.healthStatus) {
                            "HEALTHY" -> Color(0xFF2EDD3E) // Neon Portal Green
                            "UNHEALTHY" -> Color(0xFFE10612) // Toxic Red
                            else -> Color(0xFF00E5FF) // Electric Schwifty Turquoise
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 5.dp, end = 5.dp)
                                .comicShadow(color = statusShadowColor)
                                .clickable { viewModel.navigateTo(Screens.MyApiDetail(api.id)) }
                                .testTag("my_api_card_${api.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, Color(0xFF1E1E1E))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(api.providerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val projName = projects.find { it.id == api.projectId }?.name
                                        if (projName != null) {
                                            Text(
                                                projName,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .background(Color(0xFF6200EE), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        api.healthStatus,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getHealthColor(api.healthStatus)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(api.apiKeyMask, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Gray)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        api.tags.take(5).forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                    .clickable { 
                                                        selectedTagFilter = tag 
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "#$tag",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (viewModel.activeCheckId == api.id) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        IconButton(
                                            onClick = { viewModel.testMyApiHealth(api.id, context) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Test Endpoint", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 8: MyApiDetailScreen
// ----------------------------------------------------
@Composable
fun MyApiDetailScreen(id: String, viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val apis by viewModel.myApis.collectAsState()
    val api = apis.find { it.id == id }

    if (api == null) {
        DetailPlaceholder(onBack = { viewModel.navigateBack() })
        return
    }

    var keyRevealed by remember { mutableStateOf(false) }
    var rawPlaintextKey by remember { mutableStateOf("") }

    LaunchedEffect(keyRevealed, api) {
        if (keyRevealed && rawPlaintextKey.isEmpty()) {
            rawPlaintextKey = SecurityManager.decrypt(api.apiKeyEncrypted)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text("Параметры закрытой записи", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.navigateTo(Screens.CreateEditMyApi(editingId = id)) },
                modifier = Modifier.testTag("detail_edit_btn")
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Редактировать параметры", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(api.providerName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(api.dateAdded))
            Text("Сохранено $dateFmt", fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                api.keyType,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Сервер эндпоинта", api.baseUrl ?: "https://api.openai.com/v1")
                val healthLabel = when (api.healthStatus) {
                    "HEALTHY" -> "АКТИВЕН"
                    "UNHEALTHY" -> "НЕАКТИВЕН"
                    else -> "НЕ ПРОВЕРЕН"
                }
                DetailRow("Проверка состояния", healthLabel)
                DetailRow("HTTP статус-код", api.healthHttpCode?.toString() ?: "Проверки не выполнялись")
                DetailRow("Время задержки", if (api.healthLatencyMs != null) "${api.healthLatencyMs} мс" else "Н/Д")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Конфиденциальный ключ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (keyRevealed) rawPlaintextKey else api.apiKeyMask,
                    onValueChange = {},
                    readOnly = true,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { keyRevealed = !keyRevealed }) {
                            Text(if (keyRevealed) "Скрыть" else "Показать", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val revealed = SecurityManager.decrypt(api.apiKeyEncrypted)
                            SecurityManager.copyToClipboard(context, revealed)
                            Toast.makeText(context, "Ключ скопирован в буфер. Очистка через 60 секунд.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Копировать", fontSize = 11.sp, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val revealed = SecurityManager.decrypt(api.apiKeyEncrypted)
                            try {
                                val shareIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, revealed)
                                    type = "text/plain"
                                }
                                val chooser = android.content.Intent.createChooser(shareIntent, "Поделиться ключом")
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                SecurityManager.copyToClipboard(context, revealed)
                                Toast.makeText(context, "Обмен недоступен. Ключ скопирован в буфер.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Поделиться", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        if (!api.instructionsMarkdown.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Инструкции и заметки", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(api.instructionsMarkdown, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.deleteMyApi(api.id, context) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Удалить")
            }
            Button(
                onClick = { viewModel.testMyApiHealth(api.id, context) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Проверить статус")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Проверить API")
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 9: CreateEditMyApiScreen (FORM CARD)
// ----------------------------------------------------
@Composable
fun CreateEditMyApiScreen(route: Screens.CreateEditMyApi, viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val apis by viewModel.myApis.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val isEditing = route.editingId != null
    val targetApi = if (isEditing) apis.find { it.id == route.editingId } else null

    var providerName by remember { mutableStateOf(targetApi?.providerName ?: route.prefillProviderName ?: "") }
    var apiKeyRaw by remember { mutableStateOf(targetApi?.apiKeyMask ?: route.prefillKey ?: "") }
    var baseUrl by remember { mutableStateOf(targetApi?.baseUrl ?: route.prefillEndpoint ?: "") }
    var keyType by remember { mutableStateOf(targetApi?.keyType ?: "CHAT") }
    var instructions by remember { mutableStateOf(targetApi?.instructionsMarkdown ?: "") }
    var tagsInput by remember { mutableStateOf(targetApi?.tags?.joinToString(", ") ?: "") }
    var selectedProjectId by remember { mutableStateOf(targetApi?.projectId) }
    var activeTags by remember { mutableStateOf(targetApi?.tags?.filter { it.isNotBlank() }?.toSet() ?: emptySet()) }
    var customTagText by remember { mutableStateOf("") }

    // State for selected AI template
    var selectedAiType by remember { mutableStateOf(
        when {
            providerName.contains("Gemini", ignoreCase = true) || baseUrl.contains("googleapis", ignoreCase = true) -> "Google Gemini"
            providerName.contains("Claude", ignoreCase = true) || providerName.contains("Anthropic", ignoreCase = true) || baseUrl.contains("anthropic", ignoreCase = true) -> "Anthropic Claude"
            providerName.contains("DeepSeek", ignoreCase = true) || baseUrl.contains("deepseek", ignoreCase = true) -> "DeepSeek"
            providerName.contains("OpenAI", ignoreCase = true) || baseUrl.contains("openai", ignoreCase = true) -> "OpenAI"
            else -> "Другой"
        }
    )}

    // State for local dynamic health checking
    val coroutineScope = rememberCoroutineScope()
    var isCheckingLocally by remember { mutableStateOf(false) }
    var localCheckResult by remember { mutableStateOf<com.example.network.HealthCheckResult?>(null) }

    val currentRawKeyToTest = if (apiKeyRaw == targetApi?.apiKeyMask) {
        targetApi?.let { SecurityManager.decrypt(it.apiKeyEncrypted) } ?: apiKeyRaw
    } else {
        apiKeyRaw
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Отмена")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEditing) "Редактирование API ключа" else "Добавление API в Сейф", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                // Main Brutalist Card for creating your own key
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, end = 6.dp)
                        .comicShadow(color = Color(0xFF00E5FF)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = "Настройка", tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "КАРТОЧКА СОЗДАНИЯ И НАСТРОЙКИ КЛЮЧА",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. НАЗВАНИЕ
                        Text("1. Название Вашего API ключа", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = providerName,
                            onValueChange = { providerName = it },
                            placeholder = { Text("Например: My OpenAI API key, Gemini Lab") },
                            colors = lebedevTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_provider"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. КАКОЙ AI
                        Text("2. К какому AI относится этот ключ?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("OpenAI", "Google Gemini", "Anthropic Claude", "DeepSeek", "Другой").forEach { ai ->
                                val isSel = selectedAiType == ai
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedAiType = ai
                                        when (ai) {
                                            "OpenAI" -> {
                                                if (providerName.isBlank() || providerName.contains("api", ignoreCase = true)) {
                                                    providerName = "OpenAI API Key"
                                                }
                                                baseUrl = "https://api.openai.com/v1"
                                                keyType = "CHAT"
                                            }
                                            "Google Gemini" -> {
                                                if (providerName.isBlank() || providerName.contains("api", ignoreCase = true)) {
                                                    providerName = "Google Gemini Key"
                                                }
                                                baseUrl = "https://generativelanguage.googleapis.com"
                                                keyType = "MULTI"
                                            }
                                            "Anthropic Claude" -> {
                                                if (providerName.isBlank() || providerName.contains("api", ignoreCase = true)) {
                                                    providerName = "Anthropic Claude Key"
                                                }
                                                baseUrl = "https://api.anthropic.com"
                                                keyType = "CHAT"
                                            }
                                            "DeepSeek" -> {
                                                if (providerName.isBlank() || providerName.contains("api", ignoreCase = true)) {
                                                    providerName = "DeepSeek API Key"
                                                }
                                                baseUrl = "https://api.deepseek.com"
                                                keyType = "CHAT"
                                            }
                                        }
                                    },
                                    label = { Text(ai, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. КЛЮЧ
                        Text("3. Секретный API Ключ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKeyRaw,
                            onValueChange = { apiKeyRaw = it },
                            placeholder = { Text("Вставьте Ваш токен/ключ здесь (sk-...)") },
                            colors = lebedevTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_key"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Дополнительный базовый URL эндпоинта
                        Text("Базовый URL эндпоинта (Необязательно)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            placeholder = { Text("https://api.openai.com/v1") },
                            colors = lebedevTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. ПРОВЕРКА НА РАБОТОСПРАВНОСТЬ
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🔬 Проверка работоспособности", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E1E1E))
                                        Text("Протестировать ключ прямо сейчас", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            if (apiKeyRaw.isBlank()) {
                                                Toast.makeText(context, "Укажите API ключ для проверки!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isCheckingLocally = true
                                            localCheckResult = null
                                            
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                val targetEndpoint = baseUrl.ifBlank { "https://api.openai.com/v1" }
                                                val res = com.example.network.GitHubSyncService.verifyEndpointHealth(targetEndpoint, currentRawKeyToTest)
                                                
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    localCheckResult = res
                                                    isCheckingLocally = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFF1E1E1E)),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E1E1E)),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .widthIn(min = 120.dp)
                                    ) {
                                        if (isCheckingLocally) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFF1E1E1E), strokeWidth = 1.5.dp)
                                        } else {
                                            Text("Запустить тест", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                
                                if (isCheckingLocally) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Сканирование эндпоинта...", fontSize = 11.sp, color = Color.Gray)
                                }
                                
                                localCheckResult?.let { res ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val colorStatus = if (res.status == "HEALTHY") Color(0xFF2EDD3E) else Color(0xFFE10612)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(colorStatus, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (res.status == "HEALTHY") "АКТИВЕН (Интеграция работает)" else "НЕАКТИВЕН (Ошибка соединения)",
                                            color = colorStatus,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("HTTP Код: ${res.httpCode ?: "Нет ответа"} | Пинг: ${res.latencyMs} мс", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("Описание: ${res.message}", fontSize = 10.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                } ?: run {
                                    if (!isCheckingLocally) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Статус проверки: не запускалась", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Категория API:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    listOf("CHAT", "IMAGE", "EMBEDDING", "MULTI").forEach { type ->
                        FilterChip(
                            selected = keyType == type,
                            onClick = { keyType = type },
                            label = { Text(type, fontSize = 10.sp) }
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp, end = 4.dp)
                        .comicShadow(color = Color(0xFF00FFEA)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(2.dp, Color(0xFF1E1E1E))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "🏷️ ТЕГИ И СЦЕНАРИИ ИСПОЛЬЗОВАНИЯ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset tagging buttons
                        Text("Рекомендуемые теги (нажмите для добавления):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Drafting", "Coding", "Research").forEach { preset ->
                                val hasTag = activeTags.contains(preset)
                                FilterChip(
                                    selected = hasTag,
                                    onClick = {
                                        activeTags = if (hasTag) {
                                            activeTags - preset
                                        } else {
                                            activeTags + preset
                                        }
                                        tagsInput = activeTags.joinToString(", ")
                                    },
                                    label = { Text(preset, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom tag input field + add button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTagText,
                                onValueChange = { customTagText = it },
                                placeholder = { Text("Свой тег (например, Translate)", fontSize = 11.sp) },
                                colors = lebedevTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    val cleaned = customTagText.trim()
                                    if (cleaned.isNotEmpty()) {
                                        activeTags = activeTags + cleaned
                                        tagsInput = activeTags.joinToString(", ")
                                        customTagText = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF1E1E1E)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFEA), contentColor = Color(0xFF1E1E1E)),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить тег", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (activeTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Выбранные теги (нажмите для удаления):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Horizontal flow scroll of active tags with cross icon
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                activeTags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            activeTags = activeTags - tag
                                            tagsInput = activeTags.joinToString(", ")
                                        },
                                        label = { Text("#$tag", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Удалить тег",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text("Рабочий проект:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedProjectId == null,
                        onClick = { selectedProjectId = null },
                        label = { Text("Без проекта") }
                    )
                    projects.forEach { proj ->
                        FilterChip(
                            selected = selectedProjectId == proj.id,
                            onClick = { selectedProjectId = proj.id },
                            label = { Text(proj.name) }
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Инструкции или заметки (поддержка Markdown)") },
                    colors = lebedevTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (providerName.isBlank() || apiKeyRaw.isBlank()) {
                    Toast.makeText(context, "Пожалуйста, укажите Название провайдера и API ключ!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val tagList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                if (isEditing) {
                    viewModel.updateMyApi(
                        id = route.editingId!!,
                        providerName = providerName,
                        apiKeyRaw = apiKeyRaw,
                        baseUrl = baseUrl,
                        keyType = keyType,
                        instructions = instructions,
                        tags = tagList,
                        projectId = selectedProjectId,
                        isActive = true,
                        context = context
                    )
                } else {
                    viewModel.addMyApi(
                        providerName = providerName,
                        apiKeyRaw = apiKeyRaw,
                        baseUrl = baseUrl,
                        keyType = keyType,
                        instructions = instructions,
                        tags = tagList,
                        projectId = selectedProjectId,
                        context = context
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("form_submit"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Сохранить форму")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEditing) "Сохранить изменения" else "Надежно запечатать в Сейф")
        }
    }
}

// ----------------------------------------------------
// SCREEN 10: ProjectsListScreen
// ----------------------------------------------------
@Composable
fun ProjectsListScreen(viewModel: FreeLlmHubViewModel) {
    val projects by viewModel.projects.collectAsState()
    var newProjName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Управление проектами", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newProjName,
                onValueChange = { newProjName = it },
                placeholder = { Text("Название нового проекта...") },
                colors = lebedevTextFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("project_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newProjName.isNotBlank()) {
                        viewModel.createProject(newProjName, 0xFF6200EE.toInt())
                        newProjName = ""
                    }
                },
                modifier = Modifier.testTag("project_add_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить проект")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Мои рабочие проекты", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(projects) { proj ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(proj.colorLabel ?: 0xFF6200EE.toInt()))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(proj.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { viewModel.deleteProject(proj.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить проект", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 11: SettingsScreen
// ----------------------------------------------------
@Composable
fun SettingsScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    var pat by remember { mutableStateOf(SettingsManager.getGitHubPat(context)) }
    var clipboardClearSecs by remember { mutableStateOf(SettingsManager.getClipboardTimeout(context).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateToHome() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Домой")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Настройки безопасности", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Интеграция с GitHub", fontWeight = FontWeight.Bold)
                        Text("Добавьте персональный токен доступа (PAT) для обхода лимита запросов (ошибка 403) при обновлении каталогов.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pat,
                            onValueChange = {
                                pat = it
                                SettingsManager.setGitHubPat(context, it)
                            },
                            placeholder = { Text("Вставьте токен ghp_...") },
                            colors = lebedevTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_pat"),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Очистка буфера обмена", fontWeight = FontWeight.Bold)
                        Text("Через сколько секунд автоматически удалять скопированные секретные ключи из буфера обмена устройства.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = clipboardClearSecs,
                            onValueChange = {
                                clipboardClearSecs = it
                                val num = it.toIntOrNull() ?: 60
                                SettingsManager.setClipboardTimeout(context, num)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = lebedevTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Диагностика и утилиты", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screens.SyncHistory) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Журнал синхронизации")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(Screens.ParserWarnings) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ошибки и предупреждения парсера")
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Free LLM API Hub v1.0.0 (Локальный защищенный сейф)", fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

// ----------------------------------------------------
// SCREEN 12: SyncHistoryScreen
// ----------------------------------------------------
@Composable
fun SyncHistoryScreen(viewModel: FreeLlmHubViewModel) {
    val logs by viewModel.syncLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("История синхронизаций", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            EmptyListState("Нет логов синхронизации", "Записи появятся после первого обновления каталогов ключей.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val sectionName = when (log.section) {
                                    "READY_KEYS" -> "Общие Ключи"
                                    "PROXY" -> "Прокси"
                                    "OWN_TRIALS" -> "Триалы и Аккаунты"
                                    else -> log.section
                                }
                                Text(sectionName, fontWeight = FontWeight.Bold)
                                Text(
                                    if (log.status == "SUCCESS") "УСПЕШНО" else "ОШИБКА",
                                    color = if (log.status == "SUCCESS") Color.Green else Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val dateFmt = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(log.startedAt))
                            Text("Выполнено в $dateFmt", fontSize = 11.sp, color = Color.Gray)
                            Text("Новых записей: ${log.newItems} | Ошибки: ${log.errorMessage ?: "Нет"}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 13: ParserWarningsScreen
// ----------------------------------------------------
@Composable
fun ParserWarningsScreen(viewModel: FreeLlmHubViewModel) {
    val warnings by viewModel.parserWarnings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Диагностика парсера", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Эта телеметрия регистрирует неточности и несоответствия, обнаруженные при парсинге README таблиц в публичных репозиториях.", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (warnings.isEmpty()) {
            EmptyListState("Предупреждений нет", "Markdown таблицы репозиториев идеально совместимы с моделями парсеров.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(warnings) { w ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Проблема: ${w.warningType}", color = Color.Red, fontWeight = FontWeight.Bold)
                            Text("Репозиторий: ${w.sourceRepo} | Секция: ${w.section}", fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(w.message, style = MaterialTheme.typography.bodySmall)
                            if (w.rawFragment != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(w.rawFragment, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 14: BackupCenterScreen
// ----------------------------------------------------
@Composable
fun BackupCenterScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    var passphrase by remember { mutableStateOf("") }
    var inputPayload by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Резервное копирование", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Симметричный секретный пароль") },
            placeholder = { Text("Введите пароль для шифрования копии...") },
            colors = lebedevTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("backup_passphrase"),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Экспортировать резервную копию", fontWeight = FontWeight.Bold)
                Text("Шифрует и упаковывает ваши сохранённые API ключи и проекты.", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val payload = viewModel.exportMyApisBackup(passphrase, context)
                        if (payload != null) {
                            SecurityManager.copyToClipboard(context, payload, "Текст резервной копии")
                            Toast.makeText(context, "Зашифрованная резервная копия скопирована в буфер!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Скопировать текст резервной копии")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Импортировать резервную копию", fontWeight = FontWeight.Bold)
                Text("Вставьте зашифрованную резервную копию для восстановления ключей.", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputPayload,
                    onValueChange = { inputPayload = it },
                    placeholder = { Text("SECURE_LLM_BACKUP_V1:...") },
                    colors = lebedevTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("backup_import_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.importBackupPayload(inputPayload, passphrase, context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Расшифровать и восстановить")
                }
            }
        }
    }
}

// ----------------------------------------------------
// SCREEN 15: SearchScreen (DEEP FILTER SEARCH)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: FreeLlmHubViewModel) {
    val context = LocalContext.current
    val keys by viewModel.readyKeys.collectAsState()
    val proxies by viewModel.aggregators.collectAsState()
    val ownTiers by viewModel.ownAccountProviders.collectAsState()
    val myApis by viewModel.myApis.collectAsState()

    var query by remember { mutableStateOf("") }

    val filteredKeys = remember(keys, query) {
        if (query.isBlank()) emptyList() else keys.filter { it.provider.contains(query, true) || it.models.any { m -> m.contains(query, true) } }
    }
    val filteredProxies = remember(proxies, query) {
        if (query.isBlank()) emptyList() else proxies.filter { it.name.contains(query, true) || it.providersList.any { m -> m.contains(query, true) } }
    }
    val filteredOwn = remember(ownTiers, query) {
        if (query.isBlank()) emptyList() else ownTiers.filter { it.providerName.contains(query, true) }
    }
    val filteredVault = remember(myApis, query) {
        if (query.isBlank()) emptyList() else myApis.filter { it.providerName.contains(query, true) || it.tags.any { t -> t.contains(query, true) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Глобальный глубокий поиск", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Поиск по ключам, прокси, триалам или Сейфу...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Глубокий поиск") },
            colors = lebedevTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("global_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isBlank()) {
            EmptyListState("Готов к глобальному поиску", "Поиск сканирует все разделы и каталоги приложения в реальном времени.")
        } else if (filteredKeys.isEmpty() && filteredProxies.isEmpty() && filteredOwn.isEmpty() && filteredVault.isEmpty()) {
            EmptyListState("Совпадений не найдено", "Попробуйте изменить запрос или использовать ключевые слова провайдера.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (filteredVault.isNotEmpty()) {
                    item { Text("Личный Сейф (${filteredVault.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                    items(filteredVault) { api ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screens.MyApiDetail(api.id)) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        ) {
                            Text(api.providerName, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (filteredKeys.isNotEmpty()) {
                    item { Text("Каталог общих ключей (${filteredKeys.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp) }
                    items(filteredKeys) { key ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screens.ReadyKeyDetail(key.id)) }
                        ) {
                            Text(key.provider, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                if (filteredProxies.isNotEmpty()) {
                    item { Text("Мульти-провайдерные прокси (${filteredProxies.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp) }
                    items(filteredProxies) { agg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screens.AggregatorDetail(agg.id)) }
                        ) {
                            Text(agg.name, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                if (filteredOwn.isNotEmpty()) {
                    item { Text("Бесплатные триалы и регистрации (${filteredOwn.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp) }
                    items(filteredOwn) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screens.OwnAccountDetail(item.id)) }
                        ) {
                            Text(item.providerName, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// GLOBAL HELPER COMPOSABLES
@Composable
fun EmptyListState(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(body, fontSize = 12.sp, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DetailPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Запись не найдена")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack) { Text("Назад") }
        }
    }
}

@Composable
fun DetailRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.DarkGray)
        Text(value, fontSize = 13.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun getHealthColor(status: String): Color {
    return when (status) {
        "HEALTHY" -> Color.Green
        "UNHEALTHY" -> Color.Red
        else -> Color.Gray
    }
}

// -------------------------------------------------------------------------
// RICK & MORTY ANIMATED COMPANIONS (MR. POOPYBUTTHOLE & SQUANCHY)
// -------------------------------------------------------------------------
@Composable
fun MrPoopybuttholeDrawing(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val outlineColor = Color(0xFF1E1E1E)
        val furColor = Color(0xFFBD8555)
        val snoutColor = Color(0xFF9E693D)
        val coatColor = Color(0xFFFFFFFF)
        val strokeWidth = 5f

        // 1. White Lab Coat at the bottom
        val coatPath = Path().apply {
            moveTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.15f, h * 0.65f)
            lineTo(w * 0.35f, h * 0.55f)
            lineTo(w * 0.50f, h * 0.62f)
            lineTo(w * 0.65f, h * 0.55f)
            lineTo(w * 0.85f, h * 0.65f)
            lineTo(w * 0.85f, h * 0.95f)
            close()
        }
        drawPath(coatPath, color = coatColor)
        drawPath(coatPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // Lab coat collar/lapels
        val collarLeft = Path().apply {
            moveTo(w * 0.35f, h * 0.55f)
            lineTo(w * 0.40f, h * 0.80f)
            lineTo(w * 0.50f, h * 0.62f)
        }
        val collarRight = Path().apply {
            moveTo(w * 0.65f, h * 0.55f)
            lineTo(w * 0.60f, h * 0.80f)
            lineTo(w * 0.50f, h * 0.62f)
        }
        drawPath(collarLeft, color = coatColor)
        drawPath(collarLeft, color = outlineColor, style = Stroke(width = strokeWidth - 1f))
        drawPath(collarRight, color = coatColor)
        drawPath(collarRight, color = outlineColor, style = Stroke(width = strokeWidth - 1f))

        // Small pocket on pocket area (right side of coat from our view)
        drawRoundRect(
            color = coatColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.72f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.72f),
            size = androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(width = 3f)
        )

        // 2. Brown Capybara Head
        drawRoundRect(
            color = furColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.20f),
            size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.20f, w * 0.20f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.20f),
            size = androidx.compose.ui.geometry.Size(w * 0.50f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.20f, w * 0.20f),
            style = Stroke(width = strokeWidth)
        )

        // 3. Round Ears
        // Left ear
        drawCircle(
            color = snoutColor,
            center = androidx.compose.ui.geometry.Offset(w * 0.27f, h * 0.17f),
            radius = w * 0.08f
        )
        drawCircle(
            color = outlineColor,
            center = androidx.compose.ui.geometry.Offset(w * 0.27f, h * 0.17f),
            radius = w * 0.08f,
            style = Stroke(width = 3.5f)
        )
        // Right ear
        drawCircle(
            color = snoutColor,
            center = androidx.compose.ui.geometry.Offset(w * 0.73f, h * 0.17f),
            radius = w * 0.08f
        )
        drawCircle(
            color = outlineColor,
            center = androidx.compose.ui.geometry.Offset(w * 0.73f, h * 0.17f),
            radius = w * 0.08f,
            style = Stroke(width = 3.5f)
        )

        // 4. Snout / Nose Area
        drawRoundRect(
            color = snoutColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.42f),
            size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.42f),
            size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
            style = Stroke(width = strokeWidth)
        )

        // Nose tip (dark nostrils)
        drawCircle(
            color = outlineColor,
            center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.48f),
            radius = w * 0.05f
        )

        // 5. Classic quirky circular scientist eyes with swirly pupil
        val leftEyeCenter = androidx.compose.ui.geometry.Offset(w * 0.40f, h * 0.32f)
        val rightEyeCenter = androidx.compose.ui.geometry.Offset(w * 0.60f, h * 0.32f)
        val eyeRadius = w * 0.09f

        // Left Eye
        drawCircle(color = Color.White, center = leftEyeCenter, radius = eyeRadius)
        drawCircle(color = outlineColor, center = leftEyeCenter, radius = eyeRadius, style = Stroke(width = 3f))
        val leftPupilPath = Path().apply {
            moveTo(leftEyeCenter.x - 3f, leftEyeCenter.y - 3f)
            quadraticBezierTo(leftEyeCenter.x + 3f, leftEyeCenter.y - 3f, leftEyeCenter.x + 3f, leftEyeCenter.y + 2f)
            quadraticBezierTo(leftEyeCenter.x - 3f, leftEyeCenter.y + 3f, leftEyeCenter.x - 4f, leftEyeCenter.y)
        }
        drawPath(leftPupilPath, color = outlineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // Right Eye
        drawCircle(color = Color.White, center = rightEyeCenter, radius = eyeRadius)
        drawCircle(color = outlineColor, center = rightEyeCenter, radius = eyeRadius, style = Stroke(width = 3f))
        val rightPupilPath = Path().apply {
            moveTo(rightEyeCenter.x - 3f, rightEyeCenter.y - 3f)
            quadraticBezierTo(rightEyeCenter.x + 3f, rightEyeCenter.y - 3f, rightEyeCenter.x + 3f, rightEyeCenter.y + 2f)
            quadraticBezierTo(rightEyeCenter.x - 3f, rightEyeCenter.y + 3f, rightEyeCenter.x - 4f, rightEyeCenter.y)
        }
        drawPath(rightPupilPath, color = outlineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

@Composable
fun SquanchyDrawing(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val outlineColor = Color(0xFF1E1E1E)
        val metalColor = Color(0xFF90A4AE) // Sci-fi metal gray
        val portalGreen = Color(0xFF2EDD3E)
        val furColor = Color(0xFFBD8555)
        val snoutColor = Color(0xFF9E693D)
        val strokeWidth = 4.5f

        // 1. Draw Spaceship UFO Base (Dish oval) at the bottom
        val saucerPath = Path().apply {
            moveTo(w * 0.05f, h * 0.70f)
            quadraticBezierTo(w * 0.5f, h * 0.53f, w * 0.95f, h * 0.70f)
            quadraticBezierTo(w * 0.5f, h * 0.98f, w * 0.05f, h * 0.70f)
        }
        drawPath(saucerPath, color = metalColor)
        drawPath(saucerPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // UFO Panel details (bolts/lights)
        drawCircle(color = outlineColor, center = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.78f), radius = 3f)
        drawCircle(color = outlineColor, center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.81f), radius = 3f)
        drawCircle(color = outlineColor, center = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.78f), radius = 3f)

        // Glowing green navigation design element on spaceship
        val vortexPath = Path().apply {
            moveTo(w * 0.40f, h * 0.74f)
            lineTo(w * 0.60f, h * 0.74f)
            lineTo(w * 0.57f, h * 0.80f)
            lineTo(w * 0.43f, h * 0.80f)
            close()
        }
        drawPath(vortexPath, color = portalGreen)
        drawPath(vortexPath, color = outlineColor, style = Stroke(width = 2.5f))

        // 2. Draw Capybara Pilot riding the UFO
        drawRoundRect(
            color = furColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15f, 15f),
            style = Stroke(width = strokeWidth)
        )

        // Helmet/Pilot cap
        val capPath = Path().apply {
            moveTo(w * 0.30f, h * 0.33f)
            quadraticBezierTo(w * 0.50f, h * 0.22f, w * 0.70f, h * 0.33f)
            quadraticBezierTo(w * 0.50f, h * 0.31f, w * 0.30f, h * 0.33f)
        }
        drawPath(capPath, color = snoutColor)
        drawPath(capPath, color = outlineColor, style = Stroke(width = 3.5f))

        // Ears peeking out of helmet
        drawCircle(color = snoutColor, center = androidx.compose.ui.geometry.Offset(w * 0.29f, h * 0.35f), radius = w * 0.06f)
        drawCircle(color = outlineColor, center = androidx.compose.ui.geometry.Offset(w * 0.29f, h * 0.35f), radius = w * 0.06f, style = Stroke(width = 3f))
        drawCircle(color = snoutColor, center = androidx.compose.ui.geometry.Offset(w * 0.71f, h * 0.35f), radius = w * 0.06f)
        drawCircle(color = outlineColor, center = androidx.compose.ui.geometry.Offset(w * 0.71f, h * 0.35f), radius = w * 0.06f, style = Stroke(width = 3f))

        // 3. Cute caramel muzzle of Pilot
        drawRoundRect(
            color = snoutColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.48f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.48f),
            size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            style = Stroke(width = strokeWidth)
        )

        drawCircle(color = outlineColor, center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.54f), radius = 4f)

        // 4. Pilot eyes
        val leftEyeCenter = androidx.compose.ui.geometry.Offset(w * 0.41f, h * 0.42f)
        val rightEyeCenter = androidx.compose.ui.geometry.Offset(w * 0.59f, h * 0.42f)
        val eyeRadius = w * 0.07f

        // Goggle straps
        drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(w * 0.30f, h * 0.42f), end = leftEyeCenter, strokeWidth = 4f)
        drawLine(color = outlineColor, start = androidx.compose.ui.geometry.Offset(w * 0.70f, h * 0.42f), end = rightEyeCenter, strokeWidth = 4f)

        drawCircle(color = Color.White, center = leftEyeCenter, radius = eyeRadius)
        drawCircle(color = outlineColor, center = leftEyeCenter, radius = eyeRadius, style = Stroke(width = 2.5f))
        drawCircle(color = Color.Black, center = leftEyeCenter, radius = 3f)

        drawCircle(color = Color.White, center = rightEyeCenter, radius = eyeRadius)
        drawCircle(color = outlineColor, center = rightEyeCenter, radius = eyeRadius, style = Stroke(width = 2.5f))
        drawCircle(color = Color.Black, center = rightEyeCenter, radius = 3f)

        // 5. Holding a cute portal ray gun
        val gunPath = Path().apply {
            moveTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.53f)
            lineTo(w * 0.38f, h * 0.60f)
            lineTo(w * 0.25f, h * 0.60f)
            close()
        }
        drawPath(gunPath, color = Color.White)
        drawPath(gunPath, color = outlineColor, style = Stroke(width = 3f))

        // Portal fluid chamber on gun top (neon)
        drawRoundRect(
            color = portalGreen,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.09f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.26f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.09f, h * 0.08f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(width = 2.5f)
        )
    }
}

@Composable
fun RickAndMortyCompanionsWidget() {
    var isExpanded by remember { mutableStateOf(false) }
    val capyScientistQuotes = remember {
        listOf(
            "Привет, коллега! Лабораторная капибара-учёный на связи!",
            "Тройное шифрование сейфа выполнено успешно. Полная капи-безопасность!",
            "Квантовое состояние API-ключей проверено. Все системы стабильны!",
            "Эй, давай закроем утечку портальной жидкости в секторе C-137!",
            "Мой ИИ-анализатор показывает высокую активность порталов!",
            "Капибары никогда не спят, когда нужно охранять ваши секретные ключи!"
        )
    }
    val capyPilotQuotes = remember {
        listOf(
            "Ворп! Полетели сквозь червоточину за новыми эндпоинтами!",
            "Заряжаю портальную пушку! Цель: бесплатные модели Gemini!",
            "Атака Federation-Громфломитов успешно отражена кораблём Капи-НЛО!",
            "Космический радар чист! Путь к шифрованным базам данных открыт!",
            "Нажми ещё раз, и мы сделаем мёртвую петлю в космосе!",
            "Квантовый прыжок завершен! Все интеграции работают идеально!"
        )
    }

    var bubbleText by remember { mutableStateOf("Привет! Нажми на Капи-Учёного или Капи-Пилота для синхронизации!") }
    var activeSpeaker by remember { mutableStateOf("none") } // "poopy" or "squanchy" or "none"

    // Infinite float animation for bobbing
    val infiniteTransition = rememberInfiniteTransition(label = "RM_Companion")
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    // Touch click jump animation for Capy Scientist
    var poopyJumpState by remember { mutableStateOf(0f) }
    val poopyJumpAnim by animateFloatAsState(
        targetValue = poopyJumpState,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { if (it > 0f) poopyJumpState = 0f },
        label = "poopyJump"
    )

    // Touch click wiggle for Capy Pilot
    var squanchyRotateState by remember { mutableStateOf(0f) }
    val squanchyRotateAnim by animateFloatAsState(
        targetValue = squanchyRotateState,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { if (it != 0f) squanchyRotateState = 0f },
        label = "squanchyWiggle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (!isExpanded) {
            // COLLAPSED: Floating Portal Bubble that pulsates!
            val portalScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "portalPulse"
            )

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer(scaleX = portalScale, scaleY = portalScale)
                    .clickable { isExpanded = true }
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0xFF2EDD3E), CircleShape) // Neon Green
                    .border(4.dp, Color(0xFF00E5FF), CircleShape) // Neon Turquoise inside (overlay border)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Portal Canvas Drawing (Spinning spirals)
                val portalRotate by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "portalRotation"
                )

                Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(rotationZ = portalRotate)) {
                    val r = size.width / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    for (i in 0..3) {
                        val startAngle = i * 90f
                        drawArc(
                            color = Color(0xFF2EDD3E),
                            startAngle = startAngle,
                            sweepAngle = 45f,
                            useCenter = false,
                            style = Stroke(width = 4f)
                        )
                        drawArc(
                            color = Color(0xFF00E5FF),
                            startAngle = startAngle + 45f,
                            sweepAngle = 45f,
                            useCenter = false,
                            style = Stroke(width = 3f)
                        )
                    }
                }

                Text("КАПИ", color = Color(0xFF496800), fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        } else {
            // EXPANDED: Full white background card with green and turquoise borders
            Box(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(3.dp, Color(0xFF2EDD3E), RoundedCornerShape(16.dp)) // Neon Green Outer
                    .padding(3.dp)
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(13.dp)) // Turquoise Inner
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title and Close Button Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2EDD3E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Капи-Компаньоны",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }

                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Свернуть",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Text bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            val speakerLabel = when (activeSpeaker) {
                                "poopy" -> "Капи-Учёный:"
                                "squanchy" -> "Капи-Пилот:"
                                else -> "Система Капи-Лаб:"
                            }
                            val labelColor = when (activeSpeaker) {
                                "poopy" -> Color(0xFFBD8555)
                                "squanchy" -> Color(0xFF00E5FF)
                                else -> Color.Gray
                            }

                            Text(
                                speakerLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = labelColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                bubbleText,
                                fontSize = 11.sp,
                                color = Color.Black,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Characters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Character 1: Capy Scientist
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(115.dp)
                                .graphicsLayer(
                                    translationY = bobbingOffset + poopyJumpAnim,
                                    scaleY = if (poopyJumpAnim < 0f) 0.9f else 1.0f
                                )
                                .clickable {
                                    poopyJumpState = -35f
                                    bubbleText = capyScientistQuotes.random()
                                    activeSpeaker = "poopy"
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                MrPoopybuttholeDrawing(
                                    modifier = Modifier
                                        .width(55.dp)
                                        .height(95.dp)
                                )
                                Text(
                                    "Капи-Учёный",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Character 2: Capy Pilot
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(115.dp)
                                .graphicsLayer(
                                    translationY = bobbingOffset * 0.8f,
                                    rotationZ = squanchyRotateAnim
                                )
                                .clickable {
                                    squanchyRotateState = if (squanchyRotateState == 0.0f) 15f else 0f
                                    bubbleText = capyPilotQuotes.random()
                                    activeSpeaker = "squanchy"
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                SquanchyDrawing(
                                    modifier = Modifier
                                        .width(65.dp)
                                        .height(95.dp)
                                )
                                Text(
                                    "Капи-Пилот",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Нажми на персонажа, чтобы активировать озвучку!",
                        fontSize = 9.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

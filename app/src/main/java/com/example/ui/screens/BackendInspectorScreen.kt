package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.CricketApiService
import com.example.backend.FirestoreSchema
import com.example.backend.SecurityRulesDefinition
import com.example.backend.StandardCricketApiService
import com.example.model.CricketMatchFixture
import com.example.repository.Egle11Repository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackendInspectorScreen(
    repository: Egle11Repository
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Firestore Schema", "Firebase DB Connect", "Security Rules", "Cloud Functions", "Cricket API")

    val matches by repository.matches.collectAsState()
    val players by repository.players.collectAsState()
    val teams by repository.teams.collectAsState()
    val contests by repository.contests.collectAsState()
    val contestEntries by repository.contestEntries.collectAsState()
    val wallets by repository.wallets.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val withdrawals by repository.withdrawals.collectAsState()
    val winners by repository.winners.collectAsState()
    val notifications by repository.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EgleNavyDark)
    ) {
        // Tab Selector
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = EgleNavySurface,
            contentColor = EgleGoldPrimary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EgleGoldPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) EgleGoldPrimary else EgleTextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> FirestoreSchemaTab(
                counts = mapOf(
                    FirestoreSchema.COL_MATCHES to matches.size,
                    FirestoreSchema.COL_PLAYERS to players.size,
                    FirestoreSchema.COL_TEAMS to teams.size,
                    FirestoreSchema.COL_CONTESTS to contests.size,
                    FirestoreSchema.COL_CONTEST_ENTRIES to contestEntries.size,
                    FirestoreSchema.COL_WALLETS to wallets.size,
                    FirestoreSchema.COL_TRANSACTIONS to transactions.size,
                    FirestoreSchema.COL_WITHDRAWALS to withdrawals.size,
                    FirestoreSchema.COL_WINNERS to winners.size,
                    FirestoreSchema.COL_NOTIFICATIONS to notifications.size,
                    FirestoreSchema.COL_USERS to 2,
                    FirestoreSchema.COL_APP_SETTINGS to 1
                )
            )
            1 -> FirebaseDbConnectInspectorTab(repository)
            2 -> SecurityRulesTab()
            3 -> CloudFunctionsTab()
            4 -> CricketApiTab()
        }
    }
}

@Composable
fun FirestoreSchemaTab(counts: Map<String, Int>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Clean & Scalable Database Structure",
                        color = EgleGoldPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "12 production collections designed for Egle11 fantasy gaming with strict client isolation, atomic transactions, and zero pan/aadhaar storage.",
                        color = EgleTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(FirestoreSchema.ALL_COLLECTIONS) { (collectionName, description) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collection_$collectionName"),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EgleNavyBorder, Color.Transparent)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EgleNavyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (collectionName) {
                                FirestoreSchema.COL_USERS -> Icons.Default.People
                                FirestoreSchema.COL_MATCHES -> Icons.Default.SportsCricket
                                FirestoreSchema.COL_PLAYERS -> Icons.Default.Person
                                FirestoreSchema.COL_TEAMS -> Icons.Default.Groups
                                FirestoreSchema.COL_CONTESTS -> Icons.Default.EmojiEvents
                                FirestoreSchema.COL_CONTEST_ENTRIES -> Icons.Default.ConfirmationNumber
                                FirestoreSchema.COL_WALLETS -> Icons.Default.AccountBalanceWallet
                                FirestoreSchema.COL_TRANSACTIONS -> Icons.Default.ReceiptLong
                                FirestoreSchema.COL_WITHDRAWALS -> Icons.Default.CurrencyRupee
                                FirestoreSchema.COL_WINNERS -> Icons.Default.MilitaryTech
                                FirestoreSchema.COL_NOTIFICATIONS -> Icons.Default.Notifications
                                else -> Icons.Default.Settings
                            },
                            contentDescription = null,
                            tint = EgleGoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "/$collectionName",
                                color = EgleTextPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = EgleNavyCard,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${counts[collectionName] ?: 0} docs",
                                    color = EgleCyanAccent,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            color = EgleTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FirebaseDbConnectInspectorTab(repository: Egle11Repository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val config by repository.firebaseDbManager.config.collectAsState()
    val isSyncing by repository.firebaseDbManager.isSyncing.collectAsState()
    val syncProgress by repository.firebaseDbManager.syncProgress.collectAsState()

    var inputProjectId by remember { mutableStateOf(config.projectId) }
    var inputApiKey by remember { mutableStateOf(config.apiKey) }
    var inputAppId by remember { mutableStateOf(config.applicationId) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleGoldPrimary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("FIREBASE CLOUD FIRESTORE INTEGRATION", color = EgleGoldPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Direct bridge between applet backend & Firebase Cloud Database", color = EgleTextSecondary, fontSize = 11.sp)
                        }

                        Surface(
                            color = if (config.isConnected) EgleGreenSuccess.copy(alpha = 0.2f) else EgleGoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (config.isConnected) "ONLINE" else "LOCAL READY",
                                color = if (config.isConnected) EgleGreenSuccess else EgleGoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Current Engine: ${config.activeEngine}",
                        color = EgleTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Last Status: ${config.lastStatusMessage}",
                        color = EgleTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = EgleNavyBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Live Real-Time Cloud Sync", color = EgleTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = config.isLiveSyncEnabled,
                            onCheckedChange = { repository.toggleFirebaseLiveSync(it) }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADD / CONNECT FIREBASE PROJECT", color = EgleCyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputProjectId,
                        onValueChange = { inputProjectId = it },
                        label = { Text("Firebase Project ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        label = { Text("Web API Key (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputAppId,
                        onValueChange = { inputAppId = it },
                        label = { Text("Application ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isConnecting = true
                                feedbackMessage = null
                                coroutineScope.launch {
                                    val result = repository.configureFirebaseDb(
                                        context = context,
                                        projectId = inputProjectId,
                                        apiKey = inputApiKey,
                                        appId = inputAppId
                                    )
                                    isConnecting = false
                                    feedbackMessage = result.getOrElse { it.message ?: "Failed" }
                                }
                            },
                            enabled = !isConnecting && inputProjectId.isNotBlank(),
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isConnecting) "Connecting..." else "Connect Firebase", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                feedbackMessage = null
                                coroutineScope.launch {
                                    val result = repository.testFirebaseConnection()
                                    isTesting = false
                                    feedbackMessage = result.getOrElse { it.message ?: "Ping failed" }
                                }
                            },
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EgleCyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isTesting) "Testing..." else "Test Connection", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    feedbackMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            color = if (msg.contains("failed", ignoreCase = true) || msg.contains("error", ignoreCase = true))
                                EgleRedAlert
                            else
                                EgleGreenSuccess,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYNC 12 COLLECTIONS TO FIRESTORE", color = EgleCyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Push matches, players, contests, teams, and transactions to live Cloud Firestore.", color = EgleTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isSyncing) {
                        Column {
                            LinearProgressIndicator(
                                progress = { syncProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = EgleGoldPrimary,
                                trackColor = EgleNavyBorder
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pushing documents... ${(syncProgress * 100).toInt()}%", color = EgleGoldPrimary, fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = repository.pushAllToFirebase()
                                    feedbackMessage = res.getOrElse { it.message ?: "Push error" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Push All Collections to Firebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EgleGoldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Firestore Schema JSON", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        val jsonSchema = repository.firebaseDbManager.generateExportSchemaJson()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exported Firestore Schema", color = EgleGoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Surface(
                    color = EgleNavyDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                ) {
                    Text(
                        text = jsonSchema,
                        color = EgleCyanAccent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(jsonSchema))
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary)
                ) {
                    Text("Copy", color = EgleNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = EgleTextSecondary)
                }
            },
            containerColor = EgleNavySurface
        )
    }
}

@Composable
fun SecurityRulesTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Role-Based Access Control (RBAC)",
                        color = EgleGoldPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Evaluated at Firestore layer. Normal users cannot modify wallet balances, contest prizes, results, or match fixtures.",
                        color = EgleTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "SECURITY MANDATES AUDIT",
                color = EgleGoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        items(SecurityRulesDefinition.AUDIT_ITEMS) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EgleGreenSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.title,
                            color = EgleTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = EgleGreenSuccess.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.status,
                                color = EgleGreenSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = item.requirement, color = EgleGoldLight, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.detail, color = EgleTextSecondary, fontSize = 11.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FIRESTORE.RULES DEPLOYMENT SOURCE",
                color = EgleGoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030814)),
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EgleNavyBorder, Color.Transparent)))
            ) {
                Text(
                    text = SecurityRulesDefinition.RULES_SOURCE,
                    color = Color(0xFF80D8FF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun CloudFunctionsTab() {
    val endpoints = listOf(
        Triple(
            "joinContestAtomic",
            "Validates contest status & spots. Verifies wallet funds in an atomic Firestore transaction. Deducts entry fee, increments filled spots, creates entry, and writes immutable transaction ledger.",
            "Callable HTTPS (Authenticated)"
        ),
        Triple(
            "createDepositOrder",
            "Client sends deposit amount & gateway (PhonePe/Paytm). Server accesses private merchant keys in secret manager, generates signed order token. Never exposes credentials to client.",
            "Callable HTTPS (Authenticated)"
        ),
        Triple(
            "paymentWebhookHandler",
            "Public HTTPS webhook invoked by PhonePe/Paytm gateway. Verifies SHA256 checksum signature, credits user's deposit wallet, marks transaction SUCCESS, and sends notification.",
            "HTTPS Webhook (Signature verified)"
        ),
        Triple(
            "submitWithdrawalRequest",
            "User submits withdrawal to registered UPI ID. Validates winnings balance >= min threshold (₹100). Holds funds and creates withdrawal document.",
            "Callable HTTPS (Authenticated)"
        ),
        Triple(
            "adminReviewWithdrawal",
            "Admin approves, completes, or rejects withdrawal. If rejected, funds are atomically refunded back to user's winnings wallet with remarks.",
            "Callable HTTPS (Admin Only)"
        ),
        Triple(
            "settleContestPrizes",
            "Computes match points, sorts leaderboard, credits prize money atomically into winners' wallets, creates records in /winners and notifies users.",
            "Callable HTTPS (Admin Only)"
        ),
        Triple(
            "validateAndSaveTeamName",
            "Validates team name uniqueness across whole database (case-insensitive) and restricts renames to a maximum of 1 edit per team.",
            "Callable HTTPS (Authenticated)"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Trusted Backend Cloud Functions",
                        color = EgleGoldPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Financial calculations, prize distributions, and atomic balance changes are locked to trusted server logic located in /backend/functions/.",
                        color = EgleTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(endpoints) { (name, desc, type) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = EgleCyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = EgleTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = EgleNavyCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = type,
                                color = EgleGoldPrimary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = desc, color = EgleTextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun CricketApiTab() {
    val service: CricketApiService = remember { StandardCricketApiService() }
    var fixtures by remember { mutableStateOf<List<CricketMatchFixture>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        fixtures = service.getUpcomingFixtures()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Decoupled Cricket Data Layer",
                        color = EgleGoldPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sports fixtures, squads, ball-by-ball commentary and scorecards are isolated behind CricketApiService so third-party sports APIs never interact directly with wallet/payment ledgers.",
                        color = EgleTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "INCOMING SPORTS API FIXTURES",
                color = EgleGoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        items(fixtures) { fix ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fix.seriesName,
                            color = EgleCyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = EgleNavyCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = fix.matchFormat,
                                color = EgleTextPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${fix.team1.name} vs ${fix.team2.name}",
                        color = EgleTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = fix.venue,
                        color = EgleTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "External ID: ${fix.externalMatchId}",
                            color = EgleTextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val squad = service.getMatchSquad(fix.externalMatchId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EgleNavyCard),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Sync Squad", color = EgleGoldPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

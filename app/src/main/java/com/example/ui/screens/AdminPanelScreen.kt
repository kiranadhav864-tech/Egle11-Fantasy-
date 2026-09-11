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
import androidx.compose.ui.draw.scale
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
import com.example.backend.FirestoreSchema
import com.example.model.*
import com.example.repository.AuthRepository
import com.example.repository.Egle11Repository
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    repository: Egle11Repository,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()

    // Security Gate check
    if (currentUser?.role != UserRole.ADMIN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EgleNavyDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EgleRedAlert, Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = EgleRedAlert, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ACCESS DENIED: ADMIN ROLE REQUIRED", color = EgleRedAlert, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You are currently logged in as a normal user. Normal users have zero permission to access Admin collections or functions.",
                        color = EgleTextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authRepository.switchRoleForTesting(UserRole.ADMIN) },
                        colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Switch to Admin Role", color = EgleNavyDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    var selectedAdminSection by remember { mutableIntStateOf(0) }
    val sections = listOf("Matches", "Players", "Contests", "Withdrawals", "Settlements", "Firebase DB", "Settings")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EgleNavyDark)
    ) {
        // Admin Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedAdminSection,
            containerColor = EgleNavySurface,
            contentColor = EgleGoldPrimary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedAdminSection]),
                    color = EgleGoldPrimary
                )
            }
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = selectedAdminSection == index,
                    onClick = { selectedAdminSection = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedAdminSection == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedAdminSection == index) EgleGoldPrimary else EgleTextSecondary
                        )
                    }
                )
            }
        }

        when (selectedAdminSection) {
            0 -> AdminMatchesSection(repository)
            1 -> AdminPlayersSection(repository)
            2 -> AdminContestsSection(repository)
            3 -> AdminWithdrawalsSection(repository)
            4 -> AdminSettlementsSection(repository)
            5 -> AdminFirebaseDbSection(repository)
            6 -> AdminSettingsSection(repository, onNavigateToFirebaseDb = { selectedAdminSection = 5 })
        }
    }
}

// -------------------------------------------------------------
// 1. MATCHES SECTION
// -------------------------------------------------------------
@Composable
fun AdminMatchesSection(repository: Egle11Repository) {
    val matches by repository.matches.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var team1Short by remember { mutableStateOf("") }
    var team1Name by remember { mutableStateOf("") }
    var team2Short by remember { mutableStateOf("") }
    var team2Name by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Manage Matches", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Add, edit, publish/unpublish, set match status", color = EgleTextSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("admin_add_match_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Match", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            items(matches) { match ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${match.team1Short} vs ${match.team2Short}",
                                color = EgleTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = when (match.status) {
                                    MatchStatus.LIVE -> EgleRedAlert.copy(alpha = 0.2f)
                                    MatchStatus.COMPLETED -> EgleGreenSuccess.copy(alpha = 0.2f)
                                    else -> EgleGoldPrimary.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = match.status.name,
                                    color = when (match.status) {
                                        MatchStatus.LIVE -> EgleRedAlert
                                        MatchStatus.COMPLETED -> EgleGreenSuccess
                                        else -> EgleGoldPrimary
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = match.title, color = EgleTextSecondary, fontSize = 12.sp)
                        Text(text = "Venue: ${match.venue}", color = EgleTextMuted, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Published:", color = EgleTextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = match.isPublished,
                                    onCheckedChange = { repository.toggleMatchPublish(match.matchId) },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (match.status == MatchStatus.UPCOMING) {
                                    Button(
                                        onClick = { repository.setMatchStatus(match.matchId, MatchStatus.LIVE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EgleRedAlert),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Go Live", fontSize = 10.sp, color = Color.White)
                                    }
                                } else if (match.status == MatchStatus.LIVE) {
                                    Button(
                                        onClick = { repository.setMatchStatus(match.matchId, MatchStatus.COMPLETED) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Complete", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Cricket Match", color = EgleTextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Match Title") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = team1Short, onValueChange = { team1Short = it }, label = { Text("T1 Short (MUM)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = team2Short, onValueChange = { team2Short = it }, label = { Text("T2 Short (CHE)") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = team1Name, onValueChange = { team1Name = it }, label = { Text("Team 1 Full Name") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = team2Name, onValueChange = { team2Name = it }, label = { Text("Team 2 Full Name") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Stadium / Venue") }, singleLine = true)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && team1Short.isNotBlank() && team2Short.isNotBlank()) {
                                repository.addMatch(
                                    Match(
                                        title = title,
                                        team1Short = team1Short.uppercase(),
                                        team1Name = team1Name.ifBlank { team1Short },
                                        team2Short = team2Short.uppercase(),
                                        team2Name = team2Name.ifBlank { team2Short },
                                        venue = venue.ifBlank { "National Stadium" },
                                        status = MatchStatus.UPCOMING,
                                        isPublished = true
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary)
                    ) {
                        Text("Save Match", color = EgleNavyDark)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = EgleTextSecondary) }
                }
            )
        }
    }
}

// -------------------------------------------------------------
// 2. PLAYERS SECTION
// -------------------------------------------------------------
@Composable
fun AdminPlayersSection(repository: Egle11Repository) {
    val players by repository.players.collectAsState()
    val matches by repository.matches.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var teamCode by remember { mutableStateOf("MUM") }
    var role by remember { mutableStateOf(PlayerRole.BAT) }
    var credits by remember { mutableStateOf("8.5") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Players Pool (${players.size})", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Set role (WK, BAT, AR, BOWL), credits & availability", color = EgleTextSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Player", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(players) { player ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EgleNavyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.role.name,
                            color = EgleCyanAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = player.name, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "${player.teamCode} • ${player.credits} Cr • Match: ${player.matchId}", color = EgleTextSecondary, fontSize = 11.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (player.isAvailable) "Playing" else "Benched", color = if (player.isAvailable) EgleGreenSuccess else EgleRedAlert, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = player.isAvailable,
                            onCheckedChange = { repository.togglePlayerAvailability(player.playerId) },
                            modifier = Modifier.scale(0.75f)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Cricket Player", color = EgleTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Player Name") }, singleLine = true)
                    OutlinedTextField(value = teamCode, onValueChange = { teamCode = it }, label = { Text("Team Code (e.g. IND)") }, singleLine = true)
                    OutlinedTextField(value = credits, onValueChange = { credits = it }, label = { Text("Credits (e.g. 8.5)") }, singleLine = true)
                    Text("Role:", color = EgleTextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PlayerRole.values().forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            repository.addPlayer(
                                Player(
                                    name = name,
                                    teamCode = teamCode.uppercase(),
                                    role = role,
                                    credits = credits.toDoubleOrNull() ?: 8.5,
                                    isAvailable = true,
                                    matchId = matches.firstOrNull()?.matchId ?: "M_IPL_01"
                                )
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary)
                ) {
                    Text("Add", color = EgleNavyDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 3. CONTESTS SECTION
// -------------------------------------------------------------
@Composable
fun AdminContestsSection(repository: Egle11Repository) {
    val contests by repository.contests.collectAsState()
    val matches by repository.matches.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var entryFee by remember { mutableStateOf("49") }
    var totalSpots by remember { mutableStateOf("100") }
    var prizePool by remember { mutableStateOf("4000") }
    var firstPrize by remember { mutableStateOf("1500") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Contests Management", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Create, edit, set prize pools, publish/unpublish", color = EgleTextSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Contest", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(contests) { contest ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(contest.title, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = EgleNavyCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "₹${contest.entryFee.toInt()} Entry",
                                color = EgleGoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Prize Pool: ₹${contest.prizePool.toInt()} • 1st Prize: ₹${contest.firstPrize.toInt()} • Spots: ${contest.filledSpots}/${contest.totalSpots}",
                        color = EgleTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Published:", color = EgleTextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = contest.isPublished,
                                onCheckedChange = { repository.toggleContestPublish(contest.contestId) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        Surface(
                            color = if (contest.status == ContestStatus.COMPLETED) EgleGreenSuccess.copy(alpha = 0.2f) else EgleGoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = contest.status.name,
                                color = if (contest.status == ContestStatus.COMPLETED) EgleGreenSuccess else EgleGoldPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Fantasy Contest", color = EgleTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Contest Title") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = entryFee, onValueChange = { entryFee = it }, label = { Text("Entry Fee (₹)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = totalSpots, onValueChange = { totalSpots = it }, label = { Text("Total Spots") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = prizePool, onValueChange = { prizePool = it }, label = { Text("Prize Pool (₹)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = firstPrize, onValueChange = { firstPrize = it }, label = { Text("1st Prize (₹)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val fee = entryFee.toDoubleOrNull() ?: 49.0
                            val pool = prizePool.toDoubleOrNull() ?: 4000.0
                            val spots = totalSpots.toIntOrNull() ?: 100
                            repository.createContest(
                                Contest(
                                    matchId = matches.firstOrNull()?.matchId ?: "M_IPL_01",
                                    title = title,
                                    entryFee = fee,
                                    totalSpots = spots,
                                    filledSpots = 0,
                                    prizePool = pool,
                                    firstPrize = firstPrize.toDoubleOrNull() ?: (pool * 0.4),
                                    status = ContestStatus.UPCOMING,
                                    isPublished = true
                                )
                            )
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary)
                ) {
                    Text("Create", color = EgleNavyDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 4. WITHDRAWALS SECTION
// -------------------------------------------------------------
@Composable
fun AdminWithdrawalsSection(repository: Egle11Repository) {
    val withdrawals by repository.withdrawals.collectAsState()
    var selectedWithdrawal by remember { mutableStateOf<Withdrawal?>(null) }
    var reviewRemarks by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Withdrawal Requests (${withdrawals.size})", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Review, approve, reject (with wallet refund), or mark completed via UPI", color = EgleTextSecondary, fontSize = 11.sp)
            }
        }

        if (withdrawals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
                ) {
                    Text("No withdrawal requests submitted yet.", color = EgleTextSecondary, modifier = Modifier.padding(16.dp))
                }
            }
        }

        items(withdrawals) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹${item.amount.toInt()}", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = when (item.status) {
                                WithdrawalStatus.COMPLETED -> EgleGreenSuccess.copy(alpha = 0.2f)
                                WithdrawalStatus.APPROVED -> EgleCyanAccent.copy(alpha = 0.2f)
                                WithdrawalStatus.REJECTED -> EgleRedAlert.copy(alpha = 0.2f)
                                else -> EgleOrangePending.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.status.displayName,
                                color = when (item.status) {
                                    WithdrawalStatus.COMPLETED -> EgleGreenSuccess
                                    WithdrawalStatus.APPROVED -> EgleCyanAccent
                                    WithdrawalStatus.REJECTED -> EgleRedAlert
                                    else -> EgleOrangePending
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("User: ${item.userName} (${item.userMobile})", color = EgleTextPrimary, fontSize = 12.sp)
                    Text("Registered UPI ID: ${item.upiId}", color = EgleCyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Status Note: ${item.remarks}", color = EgleTextSecondary, fontSize = 11.sp)

                    if (item.status == WithdrawalStatus.PENDING || item.status == WithdrawalStatus.APPROVED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (item.status == WithdrawalStatus.PENDING) {
                                Button(
                                    onClick = { repository.adminReviewWithdrawal(item.withdrawalId, "APPROVE", "Verified UPI and winnings balance") },
                                    colors = ButtonDefaults.buttonColors(containerColor = EgleCyanAccent),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Approve", color = Color(0xFF002026), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { repository.adminReviewWithdrawal(item.withdrawalId, "COMPLETE", "Disbursed successfully via Bank UPI Payout API") },
                                colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Mark Paid (Complete)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { repository.adminReviewWithdrawal(item.withdrawalId, "REJECT", "UPI handle rejected by bank or account flagged") },
                                colors = ButtonDefaults.buttonColors(containerColor = EgleRedAlert),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Reject & Refund", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. SETTLEMENTS & WINNERS SECTION
// -------------------------------------------------------------
@Composable
fun AdminSettlementsSection(repository: Egle11Repository) {
    val contests by repository.contests.collectAsState()
    val winners by repository.winners.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Prize Settlement & Winners Ledger", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Calculate rankings and credit winnings atomically into users' wallets", color = EgleTextSecondary, fontSize = 11.sp)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ACTIVE CONTESTS PENDING SETTLEMENT", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    contests.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, color = EgleTextPrimary, fontSize = 13.sp)
                                Text("Pool: ₹${c.prizePool.toInt()} • Status: ${c.status}", color = EgleTextSecondary, fontSize = 11.sp)
                            }
                            if (c.status != ContestStatus.COMPLETED) {
                                Button(
                                    onClick = { repository.settleContestWinners(c.contestId, c.matchId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Settle Contest", color = EgleNavyDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("Settled", color = EgleGreenSuccess, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("WINNERS RECORD ARCHIVE (${winners.size})", color = EgleGoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        items(winners) { w ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EgleGoldDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#${w.rank}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(w.teamName, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Contest: ${w.contestId} • Points: ${w.pointsScored.toInt()}", color = EgleTextSecondary, fontSize = 11.sp)
                    }
                    Text("₹${w.prizeAmount.toInt()}", color = EgleGreenSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. FIREBASE DATABASE SECTION
// -------------------------------------------------------------
@Composable
fun AdminFirebaseDbSection(repository: Egle11Repository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val config by repository.firebaseDbManager.config.collectAsState()
    val isSyncing by repository.firebaseDbManager.isSyncing.collectAsState()
    val syncProgress by repository.firebaseDbManager.syncProgress.collectAsState()

    val matches by repository.matches.collectAsState()
    val players by repository.players.collectAsState()
    val teams by repository.teams.collectAsState()
    val contests by repository.contests.collectAsState()
    val entries by repository.contestEntries.collectAsState()
    val wallets by repository.wallets.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val withdrawals by repository.withdrawals.collectAsState()
    val winners by repository.winners.collectAsState()
    val notifications by repository.notifications.collectAsState()

    var inputProjectId by remember { mutableStateOf(config.projectId) }
    var inputApiKey by remember { mutableStateOf(config.apiKey) }
    var inputAppId by remember { mutableStateOf(config.applicationId) }
    var inputDbUrl by remember { mutableStateOf(config.databaseUrl) }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Firebase Cloud Database", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Connect, configure & sync Cloud Firestore backend", color = EgleTextSecondary, fontSize = 11.sp)
                    }

                    Surface(
                        color = if (config.isConnected) EgleGreenSuccess.copy(alpha = 0.2f) else EgleGoldPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (config.isConnected) EgleGreenSuccess else EgleGoldPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (config.isConnected) EgleGreenSuccess else EgleGoldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (config.isConnected) "FIRESTORE ONLINE" else "LOCAL DB CACHE",
                                color = if (config.isConnected) EgleGreenSuccess else EgleGoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Engine Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE DATABASE ENGINE", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = config.activeEngine,
                        color = EgleTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Status: ${config.lastStatusMessage}",
                        color = EgleTextSecondary,
                        fontSize = 11.sp
                    )

                    if (config.lastSyncTimestamp > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedDate = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(config.lastSyncTimestamp))
                        Text("Last Synced: $formattedDate", color = EgleGreenSuccess, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = EgleNavyBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Real-Time Cloud Sync", color = EgleTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Auto-replicate edits directly to Firestore", color = EgleTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = config.isLiveSyncEnabled,
                            onCheckedChange = { repository.toggleFirebaseLiveSync(it) }
                        )
                    }
                }
            }
        }

        // Configuration Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FIREBASE CREDENTIALS CONFIGURATION", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                inputProjectId = "egle11-cricket-fantasy"
                                inputApiKey = "AIzaSyBv9xK7lM0nPqRsTuVwXyZ123456789"
                                inputAppId = "com.aistudio.egle11.fntsy"
                                inputDbUrl = "https://egle11-cricket-fantasy.firebaseio.com"
                            }
                        ) {
                            Text("Fill Defaults", color = EgleGoldPrimary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = inputProjectId,
                        onValueChange = { inputProjectId = it },
                        label = { Text("Firebase Project ID *") },
                        placeholder = { Text("e.g. egle11-cricket-fantasy") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        label = { Text("Firebase Web API Key (Optional)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputAppId,
                        onValueChange = { inputAppId = it },
                        label = { Text("Application ID / Package Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputDbUrl,
                        onValueChange = { inputDbUrl = it },
                        label = { Text("Database URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        appId = inputAppId,
                                        dbUrl = inputDbUrl
                                    )
                                    isConnecting = false
                                    feedbackMessage = result.getOrElse { it.message ?: "Failed to connect" }
                                }
                            },
                            enabled = !isConnecting && inputProjectId.isNotBlank(),
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EgleNavyDark, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecting...", color = EgleNavyDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect Firebase", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                feedbackMessage = null
                                coroutineScope.launch {
                                    val result = repository.testFirebaseConnection()
                                    isTestingConnection = false
                                    feedbackMessage = result.getOrElse { it.message ?: "Ping check failed" }
                                }
                            },
                            enabled = !isTestingConnection,
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EgleCyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EgleCyanAccent, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pinging...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    feedbackMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (msg.contains("failed", ignoreCase = true) || msg.contains("error", ignoreCase = true))
                                EgleRedAlert.copy(alpha = 0.15f)
                            else
                                EgleGreenSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = if (msg.contains("failed", ignoreCase = true) || msg.contains("error", ignoreCase = true))
                                    EgleRedAlert
                                else
                                    EgleGreenSuccess,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Database Sync Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DATABASE ACTIONS & CLOUD MIGRATION", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Upload and sync all 12 collections (fixtures, players, contests, wallets, transactions) into Cloud Firestore with one click.",
                        color = EgleTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSyncing) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pushing Collections to Firestore...", color = EgleGoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${(syncProgress * 100).toInt()}%", color = EgleGoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { syncProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = EgleGoldPrimary,
                                trackColor = EgleNavyBorder
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val result = repository.pushAllToFirebase()
                                    feedbackMessage = result.getOrElse { it.message ?: "Push failed" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Push All 12 Collections to Cloud Firestore", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EgleGoldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View / Export Firestore Schema JSON", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Collections Live Health Table
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("12 FIRESTORE COLLECTIONS REGISTRY", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    val collectionsList = listOf(
                        FirestoreSchema.COL_USERS to 2,
                        FirestoreSchema.COL_MATCHES to matches.size,
                        FirestoreSchema.COL_PLAYERS to players.size,
                        FirestoreSchema.COL_CONTESTS to contests.size,
                        FirestoreSchema.COL_TEAMS to teams.size,
                        FirestoreSchema.COL_CONTEST_ENTRIES to entries.size,
                        FirestoreSchema.COL_WALLETS to wallets.size,
                        FirestoreSchema.COL_TRANSACTIONS to transactions.size,
                        FirestoreSchema.COL_WITHDRAWALS to withdrawals.size,
                        FirestoreSchema.COL_WINNERS to winners.size,
                        FirestoreSchema.COL_NOTIFICATIONS to notifications.size,
                        FirestoreSchema.COL_APP_SETTINGS to 1
                    )

                    collectionsList.forEachIndexed { index, (colName, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (config.isConnected) EgleGreenSuccess else EgleGoldPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = colName,
                                    color = EgleTextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                color = EgleNavyDark,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "$count docs",
                                    color = EgleGoldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (index < collectionsList.size - 1) {
                            Divider(color = EgleNavyBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    // Export Schema JSON Dialog
    if (showExportDialog) {
        val jsonSchema = repository.firebaseDbManager.generateExportSchemaJson()
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("Firestore Schema JSON", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text("Standard schema definition for Firebase Console / CLI import:", color = EgleTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = EgleNavyDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    ) {
                        Text(
                            text = jsonSchema,
                            color = EgleCyanAccent,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
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
                    Text("Copy JSON", color = EgleNavyDark, fontWeight = FontWeight.Bold)
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

// -------------------------------------------------------------
// 7. SETTINGS SECTION
// -------------------------------------------------------------
@Composable
fun AdminSettingsSection(
    repository: Egle11Repository,
    onNavigateToFirebaseDb: () -> Unit = {}
) {
    val settings by repository.appSettings.collectAsState()
    val firebaseConfig by repository.firebaseDbManager.config.collectAsState()

    var minWth by remember { mutableStateOf(settings.minWithdrawalAmount.toString()) }
    var maxWth by remember { mutableStateOf(settings.maxWithdrawalAmount.toString()) }
    var phonePeActive by remember { mutableStateOf(settings.isPhonePeEnabled) }
    var paytmActive by remember { mutableStateOf(settings.isPaytmEnabled) }
    var maintenance by remember { mutableStateOf(settings.isMaintenanceMode) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Egle11 Global Backend Settings", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Configure payment channels, limits, and maintenance state", color = EgleTextSecondary, fontSize = 11.sp)
            }
        }

        // Firebase Cloud Database Quick Option Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, EgleNavyBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("FIREBASE CLOUD DATABASE", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (firebaseConfig.isConnected) "Connected: ${firebaseConfig.projectId}" else "Engine: Local DB Cache (Ready to connect)",
                                color = if (firebaseConfig.isConnected) EgleGreenSuccess else EgleGoldPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onNavigateToFirebaseDb,
                            colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Manage Firebase", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WITHDRAWAL LIMITS", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = minWth,
                        onValueChange = { minWth = it },
                        label = { Text("Min Withdrawal Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxWth,
                        onValueChange = { maxWth = it },
                        label = { Text("Max Withdrawal Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PAYMENT GATEWAY CONFIGURATION", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("PhonePe UPI & Gateway", color = EgleTextPrimary, fontSize = 13.sp)
                            Text("Server-to-Server Order Creation", color = EgleTextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = phonePeActive, onCheckedChange = { phonePeActive = it })
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = EgleNavyBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Paytm UPI & Wallet", color = EgleTextPrimary, fontSize = 13.sp)
                            Text("Merchant Checksum API", color = EgleTextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = paytmActive, onCheckedChange = { paytmActive = it })
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SYSTEM MAINTENANCE", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Maintenance Mode", color = EgleTextPrimary, fontSize = 13.sp)
                            Text("Temporarily pause user contest entries", color = EgleTextSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = maintenance, onCheckedChange = { maintenance = it })
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    repository.updateAppSettings(
                        settings.copy(
                            minWithdrawalAmount = minWth.toDoubleOrNull() ?: 100.0,
                            maxWithdrawalAmount = maxWth.toDoubleOrNull() ?: 50000.0,
                            isPhonePeEnabled = phonePeActive,
                            isPaytmEnabled = paytmActive,
                            isMaintenanceMode = maintenance
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Configuration", color = EgleNavyDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

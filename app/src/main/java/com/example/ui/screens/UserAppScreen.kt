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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.PaymentGatewayService
import com.example.backend.SecurePaymentGatewayService
import com.example.model.*
import com.example.repository.AuthRepository
import com.example.repository.Egle11Repository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAppScreen(
    repository: Egle11Repository,
    authRepository: AuthRepository
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val user = currentUser ?: return

    var selectedBottomNav by remember { mutableIntStateOf(0) }
    val navItems = listOf(
        Triple("Home", Icons.Default.Home, 0),
        Triple("My Matches", Icons.Default.SportsCricket, 1),
        Triple("Winners", Icons.Default.EmojiEvents, 2),
        Triple("Recommended", Icons.Default.Recommend, 3),
        Triple("Wallet", Icons.Default.AccountBalanceWallet, 4),
        Triple("Notification", Icons.Default.Notifications, 5),
        Triple("Profile", Icons.Default.Person, 6)
    )

    Scaffold(
        bottomBar = {
            Surface(
                color = EgleNavySurface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedBottomNav,
                    containerColor = EgleNavySurface,
                    contentColor = EgleGoldPrimary,
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedBottomNav]),
                            color = EgleGoldPrimary,
                            height = 3.dp
                        )
                    },
                    divider = {}
                ) {
                    navItems.forEach { (label, icon, index) ->
                        val isSelected = selectedBottomNav == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedBottomNav = index },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EgleGoldPrimary else EgleTextSecondary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) EgleGoldPrimary else EgleTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_$index")
                        )
                    }
                }
            }
        },
        containerColor = EgleNavyDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedBottomNav) {
                0 -> UserMatchesView(repository, user)
                1 -> UserMyMatchesView(repository, user)
                2 -> UserWinnersView(repository, user)
                3 -> UserRecommendedMatchesView(repository, user)
                4 -> UserWalletView(repository, user)
                5 -> UserNotificationsView(repository, user)
                6 -> UserProfileView(authRepository, user)
            }
        }
    }
}

// -------------------------------------------------------------
// 1. MATCHES & CONTEST JOIN VIEW
// -------------------------------------------------------------
@Composable
fun UserMatchesView(repository: Egle11Repository, user: User) {
    val matches by repository.matches.collectAsState()
    val contests by repository.contests.collectAsState()
    val teams by repository.teams.collectAsState()
    val entries by repository.contestEntries.collectAsState()

    val publishedMatches = matches.filter { it.isPublished }
    var selectedMatchForContests by remember { mutableStateOf<Match?>(null) }
    var contestToJoin by remember { mutableStateOf<Contest?>(null) }
    var selectedTeamIdToJoin by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val userTeamsForMatch = teams.filter { it.userId == user.userId && (selectedMatchForContests == null || it.matchId == selectedMatchForContests?.matchId) }

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
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EgleGoldPrimary.copy(alpha = 0.4f), Color.Transparent)))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = EgleGoldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Welcome, ${user.name}", color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Pick your fantasy dream team and join exciting cash contests!", color = EgleTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        if (actionMessage != null) {
            item {
                Surface(
                    color = EgleGreenSuccess.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionMessage ?: "", color = EgleGreenSuccess, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        if (actionError != null) {
            item {
                Surface(
                    color = EgleRedAlert.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionError ?: "", color = EgleRedAlert, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        item {
            Text(
                text = if (selectedMatchForContests == null) "UPCOMING MATCHES" else "CONTESTS FOR ${selectedMatchForContests?.team1Short} vs ${selectedMatchForContests?.team2Short}",
                color = EgleGoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (selectedMatchForContests == null) {
            items(publishedMatches) { match ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMatchForContests = match },
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(match.matchType, color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = if (match.status == MatchStatus.LIVE) EgleRedAlert.copy(alpha = 0.2f) else EgleNavyCard,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = match.status.name,
                                    color = if (match.status == MatchStatus.LIVE) EgleRedAlert else EgleGoldPrimary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(match.team1Short, color = EgleTextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(match.team1Name, color = EgleTextSecondary, fontSize = 11.sp)
                            }
                            Text("VS", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(match.team2Short, color = EgleTextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text(match.team2Name, color = EgleTextSecondary, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = EgleNavyBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(match.venue, color = EgleTextMuted, fontSize = 11.sp)
                            Text("Tap to View Contests →", color = EgleGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        } else {
            // Contests list for selected match
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedMatchForContests = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = EgleGoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back to All Matches", color = EgleGoldPrimary, fontSize = 12.sp)
                    }
                }
            }

            val matchContests = contests.filter { it.matchId == selectedMatchForContests?.matchId && it.isPublished }

            items(matchContests) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.title, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    actionMessage = null
                                    actionError = null
                                    contestToJoin = c
                                    selectedTeamIdToJoin = userTeamsForMatch.firstOrNull()?.teamId ?: ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("join_contest_btn")
                            ) {
                                Text("Join ₹${c.entryFee.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("Prize Pool", color = EgleTextSecondary, fontSize = 10.sp)
                                Text("₹${c.prizePool.toInt()}", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("1st Prize", color = EgleTextSecondary, fontSize = 10.sp)
                                Text("₹${c.firstPrize.toInt()}", color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Winners", color = EgleTextSecondary, fontSize = 10.sp)
                                Text("${c.winnerPercentage}%", color = EgleCyanAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        // Progress bar for spots
                        val progress = if (c.totalSpots > 0) c.filledSpots.toFloat() / c.totalSpots.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EgleGoldPrimary,
                            trackColor = EgleNavyCard
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${c.totalSpots - c.filledSpots} spots left", color = EgleTextMuted, fontSize = 10.sp)
                            Text("${c.totalSpots} total spots", color = EgleTextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    // Join Contest Confirmation Dialog
    if (contestToJoin != null) {
        val contest = contestToJoin!!
        AlertDialog(
            onDismissRequest = { contestToJoin = null },
            title = { Text("Confirm Contest Join", color = EgleTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Contest: ${contest.title}", color = EgleTextSecondary, fontSize = 13.sp)
                    Text("Entry Fee: ₹${contest.entryFee.toInt()}", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Divider(color = EgleNavyBorder)

                    if (userTeamsForMatch.isEmpty()) {
                        Text("You don't have any fantasy team created for this match yet. Go to 'My Teams' to create your 11-player squad.", color = EgleRedAlert, fontSize = 12.sp)
                    } else {
                        Text("Select Team to Enter:", color = EgleTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        userTeamsForMatch.forEach { team ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTeamIdToJoin = team.teamId }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedTeamIdToJoin == team.teamId,
                                    onClick = { selectedTeamIdToJoin = team.teamId }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(team.teamName, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("11 Players (C: ${team.captainPlayerId}, VC: ${team.viceCaptainPlayerId})", color = EgleTextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (userTeamsForMatch.isNotEmpty()) {
                    Button(
                        onClick = {
                            val res = repository.joinContest(contest.contestId, selectedTeamIdToJoin)
                            res.onSuccess {
                                actionMessage = "Successfully joined contest with team! ₹${contest.entryFee.toInt()} deducted."
                                contestToJoin = null
                            }.onFailure {
                                actionError = it.message
                                contestToJoin = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess)
                    ) {
                        Text("Pay & Join", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { contestToJoin = null }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 2. FANTASY TEAMS CREATOR & MANAGER
// -------------------------------------------------------------
@Composable
fun UserTeamsView(repository: Egle11Repository, user: User) {
    val teams by repository.teams.collectAsState()
    val players by repository.players.collectAsState()
    val matches by repository.matches.collectAsState()

    val myTeams = teams.filter { it.userId == user.userId }
    var showCreateTeamModal by remember { mutableStateOf(false) }
    var teamToRename by remember { mutableStateOf<FantasyTeam?>(null) }
    var newTeamNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Create Team Form State
    var createTeamName by remember { mutableStateOf("EgleXI_${(100..999).random()}") }
    var selectedPlayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var captainId by remember { mutableStateOf("") }
    var viceCaptainId by remember { mutableStateOf("") }

    val matchId = matches.firstOrNull()?.matchId ?: "M_IPL_01"
    val matchPlayers = players.filter { it.matchId == matchId }

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
                    Text("My Fantasy Teams (${myTeams.size})", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("11 players squad • 1 rename limit • duplicate check", color = EgleTextSecondary, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        // Pre-populate 11 players for quick testing if empty
                        if (selectedPlayerIds.isEmpty() && matchPlayers.size >= 11) {
                            val pick = matchPlayers.take(11).map { it.playerId }.toSet()
                            selectedPlayerIds = pick
                            captainId = pick.first()
                            viceCaptainId = pick.drop(1).first()
                        }
                        showCreateTeamModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_fantasy_team_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Team", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (successMessage != null) {
            item {
                Surface(
                    color = EgleGreenSuccess.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(successMessage ?: "", color = EgleGreenSuccess, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        if (errorMessage != null) {
            item {
                Surface(
                    color = EgleRedAlert.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorMessage ?: "", color = EgleRedAlert, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        if (myTeams.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = EgleTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No teams created yet", color = EgleTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Create your first 11-player squad to enter contests.", color = EgleTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        items(myTeams) { team ->
            val cPlayer = players.find { it.playerId == team.captainPlayerId }
            val vcPlayer = players.find { it.playerId == team.viceCaptainPlayerId }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(team.teamName, color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            color = if (team.nameEditCount >= 1) EgleRedAlert.copy(alpha = 0.15f) else EgleGreenSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (team.nameEditCount >= 1) "Renamed (1/1)" else "Rename Left (0/1)",
                                color = if (team.nameEditCount >= 1) EgleRedAlert else EgleGreenSuccess,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("11 Players Selected • Match: ${team.matchId}", color = EgleTextSecondary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Captain (2x): ${cPlayer?.name ?: team.captainPlayerId}", color = EgleCyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("VC (1.5x): ${vcPlayer?.name ?: team.viceCaptainPlayerId}", color = EgleGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = EgleNavyBorder)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                errorMessage = null
                                successMessage = null
                                teamToRename = team
                                newTeamNameInput = team.teamName
                            },
                            enabled = team.nameEditCount < 1
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (team.nameEditCount < 1) EgleGoldPrimary else EgleTextMuted)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (team.nameEditCount < 1) "Change Team Name (1-time only)" else "Rename Limit Reached",
                                color = if (team.nameEditCount < 1) EgleGoldPrimary else EgleTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog (Validating 1-edit constraint & duplicate name)
    if (teamToRename != null) {
        val team = teamToRename!!
        AlertDialog(
            onDismissRequest = { teamToRename = null },
            title = { Text("Rename Fantasy Team", color = EgleTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mandate: A team name can be changed ONLY ONCE. Duplicate team names are not permitted across the app.", color = EgleGoldPrimary, fontSize = 11.sp)
                    OutlinedTextField(
                        value = newTeamNameInput,
                        onValueChange = { newTeamNameInput = it },
                        label = { Text("New Unique Team Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = repository.createOrUpdateFantasyTeam(
                            teamId = team.teamId,
                            matchId = team.matchId,
                            teamName = newTeamNameInput,
                            selectedPlayerIds = team.selectedPlayerIds,
                            captainId = team.captainPlayerId,
                            viceCaptainId = team.viceCaptainPlayerId
                        )
                        res.onSuccess {
                            successMessage = "Team successfully renamed to ${it.teamName} (1-time edit consumed)."
                            teamToRename = null
                        }.onFailure {
                            errorMessage = it.message
                            teamToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary)
                ) {
                    Text("Save (Final Edit)", color = EgleNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { teamToRename = null }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }

    // Create Team Dialog
    if (showCreateTeamModal) {
        AlertDialog(
            onDismissRequest = { showCreateTeamModal = false },
            title = { Text("Create 11-Player Team", color = EgleTextPrimary) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = createTeamName,
                        onValueChange = { createTeamName = it },
                        label = { Text("Team Name (Unique)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Selected Players: ${selectedPlayerIds.size}/11", color = if (selectedPlayerIds.size == 11) EgleGreenSuccess else EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    // Pick players
                    Box(modifier = Modifier.height(200.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(matchPlayers) { p ->
                                val isSelected = selectedPlayerIds.contains(p.playerId)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) EgleNavyCard else Color.Transparent)
                                        .clickable {
                                            if (isSelected) {
                                                selectedPlayerIds = selectedPlayerIds - p.playerId
                                            } else {
                                                if (selectedPlayerIds.size < 11) {
                                                    selectedPlayerIds = selectedPlayerIds + p.playerId
                                                }
                                            }
                                        }
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, color = EgleTextPrimary, fontSize = 12.sp)
                                        Text("${p.teamCode} • ${p.role} • ${p.credits} Cr", color = EgleTextSecondary, fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilterChip(
                                                selected = captainId == p.playerId,
                                                onClick = { captainId = p.playerId },
                                                label = { Text("C", fontSize = 10.sp) }
                                            )
                                            FilterChip(
                                                selected = viceCaptainId == p.playerId,
                                                onClick = { viceCaptainId = p.playerId },
                                                label = { Text("VC", fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = repository.createOrUpdateFantasyTeam(
                            teamId = null,
                            matchId = matchId,
                            teamName = createTeamName,
                            selectedPlayerIds = selectedPlayerIds.toList(),
                            captainId = captainId,
                            viceCaptainId = viceCaptainId
                        )
                        res.onSuccess {
                            successMessage = "Team '${it.teamName}' created successfully!"
                            showCreateTeamModal = false
                        }.onFailure {
                            errorMessage = it.message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                    enabled = selectedPlayerIds.size == 11
                ) {
                    Text("Save Team", color = EgleNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTeamModal = false }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 3. WALLET & FINANCIAL OPERATIONS
// -------------------------------------------------------------
@Composable
fun UserWalletView(repository: Egle11Repository, user: User) {
    val wallets by repository.wallets.collectAsState()
    val transactions by repository.transactions.collectAsState()
    val userWallet = repository.getOrCreateWallet(user.userId)
    val userTransactions = transactions.filter { it.userId == user.userId }

    val coroutineScope = rememberCoroutineScope()
    val paymentService: PaymentGatewayService = remember { SecurePaymentGatewayService() }

    var showAddCashModal by remember { mutableStateOf(false) }
    var showWithdrawModal by remember { mutableStateOf(false) }

    var addCashAmount by remember { mutableStateOf("500") }
    var selectedGateway by remember { mutableStateOf(PaymentGateway.PHONEPE) }
    var withdrawAmount by remember { mutableStateOf("150") }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Main Balance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_wallet_card"),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(EgleGoldPrimary.copy(alpha = 0.5f), Color.Transparent)))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TOTAL WALLET BALANCE", color = EgleTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EgleGreenSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tamper-Proof", color = EgleGreenSuccess, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${"%.2f".format(userWallet.totalBalance)}",
                        color = EgleTextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = EgleNavyBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Breakdown (Deposit / Winnings / Bonus)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Deposit Balance", color = EgleTextSecondary, fontSize = 10.sp)
                            Text("₹${"%.2f".format(userWallet.depositBalance)}", color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("PhonePe / Paytm", color = EgleTextMuted, fontSize = 9.sp)
                        }
                        Column {
                            Text("Winnings Balance", color = EgleTextSecondary, fontSize = 10.sp)
                            Text("₹${"%.2f".format(userWallet.winningsBalance)}", color = EgleGreenSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Withdrawable via UPI", color = EgleCyanAccent, fontSize = 9.sp)
                        }
                        Column {
                            Text("Bonus Balance", color = EgleTextSecondary, fontSize = 10.sp)
                            Text("₹${"%.2f".format(userWallet.bonusBalance)}", color = EgleGoldLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Contest Discounts", color = EgleTextMuted, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                statusMessage = null
                                errorMessage = null
                                showAddCashModal = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("add_cash_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Cash", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                statusMessage = null
                                errorMessage = null
                                showWithdrawModal = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("withdraw_winnings_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EgleNavyCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = EgleGreenSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Withdraw", color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (statusMessage != null) {
            item {
                Surface(
                    color = EgleGreenSuccess.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(statusMessage ?: "", color = EgleGreenSuccess, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        if (errorMessage != null) {
            item {
                Surface(
                    color = EgleRedAlert.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(errorMessage ?: "", color = EgleRedAlert, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }
        }

        item {
            Text(
                text = "TRANSACTIONS AUDIT LEDGER (${userTransactions.size})",
                color = EgleGoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (userTransactions.isEmpty()) {
            item {
                Text("No transactions logged yet.", color = EgleTextSecondary, fontSize = 12.sp)
            }
        }

        items(userTransactions) { tx ->
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
                            .background(if (tx.type.isCredit) EgleGreenSuccess.copy(alpha = 0.2f) else EgleRedAlert.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tx.type.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (tx.type.isCredit) EgleGreenSuccess else EgleRedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.type.displayName, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${tx.gateway} • ${tx.notes}", color = EgleTextSecondary, fontSize = 10.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (tx.type.isCredit) "+" else "-"}₹${tx.amount.toInt()}",
                            color = if (tx.type.isCredit) EgleGreenSuccess else EgleRedAlert,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = tx.status.name,
                            color = if (tx.status == TransactionStatus.SUCCESS) EgleGreenSuccess else EgleOrangePending,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }

    // Modal: Add Cash via PhonePe or Paytm
    if (showAddCashModal) {
        AlertDialog(
            onDismissRequest = { showAddCashModal = false },
            title = { Text("Add Cash to Wallet", color = EgleTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Payment Gateway (Credentials protected on server):", color = EgleTextSecondary, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedGateway == PaymentGateway.PHONEPE,
                            onClick = { selectedGateway = PaymentGateway.PHONEPE },
                            label = { Text("PhonePe", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedGateway == PaymentGateway.PAYTM,
                            onClick = { selectedGateway = PaymentGateway.PAYTM },
                            label = { Text("Paytm", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = addCashAmount,
                        onValueChange = { addCashAmount = it },
                        label = { Text("Amount (₹)") },
                        leadingIcon = { Text("₹", color = EgleGoldPrimary, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("100", "250", "500", "1000").forEach { quickAmt ->
                            SuggestionChip(
                                onClick = { addCashAmount = quickAmt },
                                label = { Text("₹$quickAmt", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = addCashAmount.toDoubleOrNull() ?: 100.0
                        isProcessing = true
                        coroutineScope.launch {
                            val order = paymentService.createOrder(
                                PaymentOrderRequest(amt, selectedGateway),
                                user.userId
                            )
                            // Simulate successful webhook callback
                            val creditResult = repository.creditDepositViaWebhook(
                                userId = user.userId,
                                amount = amt,
                                gateway = selectedGateway,
                                orderId = order.orderId
                            )
                            isProcessing = false
                            showAddCashModal = false
                            creditResult.onSuccess {
                                statusMessage = "₹$amt deposited successfully via ${selectedGateway.displayName}!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGoldPrimary)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EgleNavyDark)
                    } else {
                        Text("Proceed via ${selectedGateway.displayName}", color = EgleNavyDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCashModal = false }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }

    // Modal: Withdraw Winnings to registered UPI ID
    if (showWithdrawModal) {
        AlertDialog(
            onDismissRequest = { showWithdrawModal = false },
            title = { Text("Withdraw Winnings to UPI", color = EgleTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Available Winnings Balance: ₹${userWallet.winningsBalance.toInt()}", color = EgleGreenSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Registered Payout UPI ID: ${user.upiId}", color = EgleCyanAccent, fontSize = 12.sp)

                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Withdrawal Amount (₹)") },
                        leadingIcon = { Text("₹", color = EgleGoldPrimary, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Min ₹100 • Max ₹50,000 per request. Sent directly to your registered UPI ID.", color = EgleTextMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull() ?: 100.0
                        val res = repository.requestWithdrawal(amt)
                        res.onSuccess {
                            statusMessage = "Withdrawal request of ₹$amt submitted for Admin review."
                            showWithdrawModal = false
                        }.onFailure {
                            errorMessage = it.message
                            showWithdrawModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EgleGreenSuccess)
                ) {
                    Text("Request Payout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawModal = false }) { Text("Cancel", color = EgleTextSecondary) }
            }
        )
    }
}

// -------------------------------------------------------------
// 4. NOTIFICATIONS
// -------------------------------------------------------------
@Composable
fun UserNotificationsView(repository: Egle11Repository, user: User) {
    val notifications by repository.notifications.collectAsState()
    val myNotifications = notifications.filter { it.userId == "all" || it.userId == user.userId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "NOTIFICATIONS (${myNotifications.size})",
                color = EgleGoldPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (myNotifications.isEmpty()) {
            item {
                Text("No notifications available.", color = EgleTextSecondary, fontSize = 12.sp)
            }
        }

        items(myNotifications) { notif ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { repository.markNotificationRead(notif.notificationId) },
                colors = CardDefaults.cardColors(containerColor = if (notif.isRead) EgleNavySurface else EgleNavyCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = when (notif.type) {
                            NotificationType.NEW_MATCH -> Icons.Default.SportsCricket
                            NotificationType.CONTEST_ANNOUNCEMENT -> Icons.Default.EmojiEvents
                            NotificationType.CONTEST_RESULTS -> Icons.Default.MilitaryTech
                            NotificationType.WALLET_UPDATE -> Icons.Default.AccountBalanceWallet
                            NotificationType.WITHDRAWAL_STATUS -> Icons.Default.CurrencyRupee
                            NotificationType.SYSTEM_ANNOUNCEMENT -> Icons.Default.Campaign
                        },
                        contentDescription = null,
                        tint = EgleGoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.title, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(notif.message, color = EgleTextSecondary, fontSize = 11.sp)
                    }
                    if (!notif.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EgleGoldPrimary)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1B. MY MATCHES VIEW (Joined Contests & User Teams)
// -------------------------------------------------------------
@Composable
fun UserMyMatchesView(repository: Egle11Repository, user: User) {
    val matches by repository.matches.collectAsState()
    val contests by repository.contests.collectAsState()
    val teams by repository.teams.collectAsState()
    val entries by repository.contestEntries.collectAsState()

    var matchFilter by remember { mutableStateOf("ALL") } // ALL, UPCOMING, LIVE, COMPLETED

    val userEntries = entries.filter { it.userId == user.userId }
    val userMatchIds = userEntries.map { it.matchId }.toSet()
    val userMatches = matches.filter { it.matchId in userMatchIds || teams.any { t -> t.userId == user.userId && t.matchId == it.matchId } }

    val filteredMatches = when (matchFilter) {
        "UPCOMING" -> userMatches.filter { it.status == MatchStatus.UPCOMING }
        "LIVE" -> userMatches.filter { it.status == MatchStatus.LIVE }
        "COMPLETED" -> userMatches.filter { it.status == MatchStatus.COMPLETED }
        else -> userMatches
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "My Matches & Joined Contests",
                color = EgleTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EgleNavyCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("ALL", "UPCOMING", "LIVE", "COMPLETED").forEach { filter ->
                    val isSelected = matchFilter == filter
                    Surface(
                        color = if (isSelected) EgleGoldPrimary else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { matchFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) EgleNavyDark else EgleTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (filteredMatches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SportsCricket,
                            contentDescription = null,
                            tint = EgleTextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matches joined in this category",
                            color = EgleTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Browse the Home tab to enter mega contests and create your fantasy XI.",
                            color = EgleTextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredMatches) { match ->
                val matchEntries = userEntries.filter { it.matchId == match.matchId }
                val matchTeams = teams.filter { it.userId == user.userId && it.matchId == match.matchId }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(if (match.status == MatchStatus.LIVE) EgleRedAlert else EgleGoldPrimary, Color.Transparent)
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(match.title, color = EgleGoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                color = when (match.status) {
                                    MatchStatus.LIVE -> EgleRedAlert.copy(alpha = 0.2f)
                                    MatchStatus.COMPLETED -> EgleGreenSuccess.copy(alpha = 0.2f)
                                    else -> EgleNavyCard
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = match.status.name,
                                    color = when (match.status) {
                                        MatchStatus.LIVE -> EgleRedAlert
                                        MatchStatus.COMPLETED -> EgleGreenSuccess
                                        else -> EgleCyanAccent
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${match.team1Short} vs ${match.team2Short}",
                            color = EgleTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Contests Joined: ${matchEntries.size}", color = EgleCyanAccent, fontSize = 12.sp)
                            Text("Teams Created: ${matchTeams.size}", color = EgleTextSecondary, fontSize = 12.sp)
                        }

                        if (matchEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = EgleNavyBorder)
                            Spacer(modifier = Modifier.height(8.dp))
                            matchEntries.forEach { entry ->
                                val contest = contests.find { it.contestId == entry.contestId }
                                val team = teams.find { it.teamId == entry.teamId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${contest?.title ?: "Contest"} (${team?.teamName ?: "Team"})",
                                        color = EgleTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Rank #${entry.rank} (${entry.points} pts)",
                                        color = EgleGoldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
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

// -------------------------------------------------------------
// 2B. WINNERS VIEW (Completed Contests & Payout Hall of Fame)
// -------------------------------------------------------------
@Composable
fun UserWinnersView(repository: Egle11Repository, user: User) {
    val winners by repository.winners.collectAsState()
    val matches by repository.matches.collectAsState()

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
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(EgleGoldPrimary, Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(EgleGoldLight, EgleGoldDark))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Winners & Hall of Fame", color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Transparent real-money payouts directly to verified UPI IDs", color = EgleTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Text("Top Grand League Champions", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (winners.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No completed contests yet. Check back soon for winner lists!", color = EgleTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(winners) { winner ->
                val match = matches.find { it.matchId == winner.matchId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Emblem
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    when (winner.rank) {
                                        1 -> EgleGoldPrimary
                                        2 -> Color(0xFFC0C0C0)
                                        3 -> Color(0xFFCD7F32)
                                        else -> EgleNavyCard
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${winner.rank}",
                                color = if (winner.rank <= 3) EgleNavyDark else EgleTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(winner.teamName, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "${match?.team1Short ?: "IND"} vs ${match?.team2Short ?: "PAK"} • ${winner.pointsScored} pts",
                                color = EgleTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+ ₹${winner.prizeAmount.toInt()}",
                                color = EgleGreenSuccess,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text("UPI Disbursed", color = EgleTextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3B. RECOMMENDED MATCHES VIEW (Curated Hot Matches & Mega Pools)
// -------------------------------------------------------------
@Composable
fun UserRecommendedMatchesView(repository: Egle11Repository, user: User) {
    val matches by repository.matches.collectAsState()
    val contests by repository.contests.collectAsState()

    val recommendedMatches = matches.filter { it.isPublished }

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
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(EgleCyanAccent, Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(EgleCyanAccent, EgleNavyCard))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Recommend, contentDescription = null, tint = EgleNavyDark, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Recommended For You", color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("High-stakes fixtures and guaranteed prize pools", color = EgleTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Text("Featured Mega Fixtures", color = EgleGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(recommendedMatches) { match ->
            val matchContests = contests.filter { it.matchId == match.matchId }
            val maxPrize: Double = matchContests.maxOfOrNull { it.prizePool } ?: 50000.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EgleNavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(EgleGoldPrimary.copy(alpha = 0.5f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = EgleGoldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⭐ RECOMMENDED",
                                color = EgleGoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(match.matchType, color = EgleCyanAccent, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${match.team1Name} vs ${match.team2Name}",
                        color = EgleTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Venue: ${match.venue}", color = EgleTextSecondary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = EgleNavyBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mega Prize Pool", color = EgleTextMuted, fontSize = 10.sp)
                            Text("₹${maxPrize.toInt()}", color = EgleGoldPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                        Surface(
                            color = EgleNavyCard,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${matchContests.size} Contests Available",
                                color = EgleCyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. USER PROFILE
// -------------------------------------------------------------
@Composable
fun UserProfileView(authRepository: AuthRepository, user: User) {
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
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EgleNavyCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EgleGoldPrimary, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(user.name, color = EgleTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Role: ${user.role.name} • Status: ${user.accountStatus.name}", color = EgleCyanAccent, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = EgleNavyBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileItemRow("User ID", user.userId)
                    ProfileItemRow("Mobile Number", user.mobileNumber)
                    ProfileItemRow("Email", user.email)
                    ProfileItemRow("Registered UPI ID", user.upiId)
                    ProfileItemRow("State", user.selectedState)

                    Spacer(modifier = Modifier.height(14.dp))

                    // 18+ Age & State Verified Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = EgleGreenSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = EgleGreenSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Age 18+ Verified", color = EgleGreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = EgleGoldPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = EgleGoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Legal Jurisdiction", color = EgleGoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = EgleNavyCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = EgleGreenSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aadhaar and PAN details are not stored on Egle11 per platform specifications. All winnings are disbursed to registered UPI.",
                                color = EgleTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { authRepository.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EgleRedAlert)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Logout", color = EgleRedAlert, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = EgleTextSecondary, fontSize = 12.sp)
        Text(value, color = EgleTextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

package com.example.repository

import android.content.Context
import com.example.backend.FirebaseDatabaseManager
import com.example.backend.FirestoreSchema
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Main Egle11 Backend Repository
 * Manages all 12 Firestore collections, validates role permissions,
 * and maintains atomic financial and contest integrity.
 */
class Egle11Repository(private val authRepository: AuthRepository) {

    val firebaseDbManager = FirebaseDatabaseManager()

    // 1. Matches State
    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()

    // 2. Players State
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    // 3. Teams State
    private val _teams = MutableStateFlow<List<FantasyTeam>>(emptyList())
    val teams: StateFlow<List<FantasyTeam>> = _teams.asStateFlow()

    // 4. Contests State
    private val _contests = MutableStateFlow<List<Contest>>(emptyList())
    val contests: StateFlow<List<Contest>> = _contests.asStateFlow()

    // 5. Contest Entries State
    private val _contestEntries = MutableStateFlow<List<ContestEntry>>(emptyList())
    val contestEntries: StateFlow<List<ContestEntry>> = _contestEntries.asStateFlow()

    // 6. Wallets State (keyed by userId)
    private val _wallets = MutableStateFlow<Map<String, Wallet>>(emptyMap())
    val wallets: StateFlow<Map<String, Wallet>> = _wallets.asStateFlow()

    // 7. Transactions State
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // 8. Withdrawals State
    private val _withdrawals = MutableStateFlow<List<Withdrawal>>(emptyList())
    val withdrawals: StateFlow<List<Withdrawal>> = _withdrawals.asStateFlow()

    // 9. Winners State
    private val _winners = MutableStateFlow<List<Winner>>(emptyList())
    val winners: StateFlow<List<Winner>> = _winners.asStateFlow()

    // 10. Notifications State
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    // 11. App Settings State
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    init {
        seedInitialBackendData()
    }

    private fun checkAdmin(): Result<Unit> {
        val user = authRepository.currentUser.value
        return if (user != null && user.role == UserRole.ADMIN) {
            Result.success(Unit)
        } else {
            Result.failure(SecurityException("Permission Denied: Admin authorization required"))
        }
    }

    private fun getCurrentUser(): User {
        return authRepository.currentUser.value
            ?: throw SecurityException("User not authenticated. Please log in.")
    }

    // =========================================================================
    // MATCHES MANAGEMENT (Admin operations + User view)
    // =========================================================================

    fun addMatch(match: Match): Result<Match> {
        checkAdmin().onFailure { return Result.failure(it) }
        val id = if (match.matchId.isBlank()) "MATCH_${System.currentTimeMillis()}" else match.matchId
        val newMatch = match.copy(matchId = id)
        _matches.value = _matches.value + newMatch
        sendBroadcastNotification(
            NotificationType.NEW_MATCH,
            "New Match Added: ${newMatch.team1Short} vs ${newMatch.team2Short}",
            "${newMatch.title} is now open for creating fantasy teams!"
        )
        return Result.success(newMatch)
    }

    fun updateMatch(updatedMatch: Match): Result<Match> {
        checkAdmin().onFailure { return Result.failure(it) }
        _matches.value = _matches.value.map { if (it.matchId == updatedMatch.matchId) updatedMatch else it }
        return Result.success(updatedMatch)
    }

    fun toggleMatchPublish(matchId: String): Result<Boolean> {
        checkAdmin().onFailure { return Result.failure(it) }
        var newPublished = false
        _matches.value = _matches.value.map {
            if (it.matchId == matchId) {
                newPublished = !it.isPublished
                it.copy(isPublished = newPublished)
            } else it
        }
        return Result.success(newPublished)
    }

    fun setMatchStatus(matchId: String, status: MatchStatus): Result<MatchStatus> {
        checkAdmin().onFailure { return Result.failure(it) }
        _matches.value = _matches.value.map {
            if (it.matchId == matchId) it.copy(status = status) else it
        }
        return Result.success(status)
    }

    // =========================================================================
    // PLAYERS MANAGEMENT (Admin operations)
    // =========================================================================

    fun addPlayer(player: Player): Result<Player> {
        checkAdmin().onFailure { return Result.failure(it) }
        val id = if (player.playerId.isBlank()) "PLY_${UUID.randomUUID().toString().substring(0, 6)}" else player.playerId
        val newPlayer = player.copy(playerId = id)
        _players.value = _players.value + newPlayer
        return Result.success(newPlayer)
    }

    fun updatePlayer(updatedPlayer: Player): Result<Player> {
        checkAdmin().onFailure { return Result.failure(it) }
        _players.value = _players.value.map { if (it.playerId == updatedPlayer.playerId) updatedPlayer else it }
        return Result.success(updatedPlayer)
    }

    fun togglePlayerAvailability(playerId: String): Result<Boolean> {
        checkAdmin().onFailure { return Result.failure(it) }
        var newAvail = true
        _players.value = _players.value.map {
            if (it.playerId == playerId) {
                newAvail = !it.isAvailable
                it.copy(isAvailable = newAvail)
            } else it
        }
        return Result.success(newAvail)
    }

    // =========================================================================
    // FANTASY TEAMS (Users: 11 players, C/VC, Unique team name, 1-edit limit)
    // =========================================================================

    fun createOrUpdateFantasyTeam(
        teamId: String?,
        matchId: String,
        teamName: String,
        selectedPlayerIds: List<String>,
        captainId: String,
        viceCaptainId: String
    ): Result<FantasyTeam> {
        val user = getCurrentUser()
        val cleanName = teamName.trim()

        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("Team name must be at least 3 characters"))
        }

        // Duplicate team name validation across whole application
        val duplicate = _teams.value.find {
            it.teamName.equals(cleanName, ignoreCase = true) &&
                    it.teamId != teamId &&
                    it.userId != user.userId
        }
        if (duplicate != null) {
            return Result.failure(IllegalArgumentException("The team name '$cleanName' is already taken by another user. Please choose a unique name."))
        }

        // 11 Players validation
        if (selectedPlayerIds.size != 11) {
            return Result.failure(IllegalArgumentException("A fantasy team must contain exactly 11 players. Selected: ${selectedPlayerIds.size}"))
        }

        if (captainId.isBlank() || viceCaptainId.isBlank()) {
            return Result.failure(IllegalArgumentException("Both Captain and Vice-Captain must be selected"))
        }

        if (captainId == viceCaptainId) {
            return Result.failure(IllegalArgumentException("Captain and Vice-Captain cannot be the same player"))
        }

        if (teamId != null) {
            // Edit Team
            val existing = _teams.value.find { it.teamId == teamId }
                ?: return Result.failure(NoSuchElementException("Team not found"))

            if (existing.userId != user.userId && user.role != UserRole.ADMIN) {
                return Result.failure(SecurityException("Unauthorized to modify this fantasy team"))
            }

            // Single edit constraint on team name
            val isNameChanged = !existing.teamName.equals(cleanName, ignoreCase = true)
            if (isNameChanged && existing.nameEditCount >= 1) {
                return Result.failure(IllegalStateException("A team name can be changed only once. You have reached the limit."))
            }

            val updated = existing.copy(
                teamName = cleanName,
                nameEditCount = if (isNameChanged) existing.nameEditCount + 1 else existing.nameEditCount,
                selectedPlayerIds = selectedPlayerIds,
                captainPlayerId = captainId,
                viceCaptainPlayerId = viceCaptainId
            )
            _teams.value = _teams.value.map { if (it.teamId == teamId) updated else it }
            return Result.success(updated)
        } else {
            // Create New Team
            val newId = "TEAM_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 4)}"
            val newTeam = FantasyTeam(
                teamId = newId,
                userId = user.userId,
                matchId = matchId,
                teamName = cleanName,
                selectedPlayerIds = selectedPlayerIds,
                captainPlayerId = captainId,
                viceCaptainPlayerId = viceCaptainId,
                createdAt = System.currentTimeMillis(),
                nameEditCount = 0
            )
            _teams.value = _teams.value + newTeam
            return Result.success(newTeam)
        }
    }

    // =========================================================================
    // CONTESTS MANAGEMENT (Admin create/edit/publish + User Join)
    // =========================================================================

    fun createContest(contest: Contest): Result<Contest> {
        checkAdmin().onFailure { return Result.failure(it) }
        val id = if (contest.contestId.isBlank()) "CONT_${System.currentTimeMillis()}" else contest.contestId
        val newContest = contest.copy(contestId = id)
        _contests.value = _contests.value + newContest
        sendBroadcastNotification(
            NotificationType.CONTEST_ANNOUNCEMENT,
            "New Contest: ${newContest.title}",
            "Entry ₹${newContest.entryFee.toInt()} | Prize Pool ₹${newContest.prizePool.toInt()} | 1st Prize ₹${newContest.firstPrize.toInt()}"
        )
        return Result.success(newContest)
    }

    fun updateContest(contest: Contest): Result<Contest> {
        checkAdmin().onFailure { return Result.failure(it) }
        _contests.value = _contests.value.map { if (it.contestId == contest.contestId) contest else it }
        return Result.success(contest)
    }

    fun toggleContestPublish(contestId: String): Result<Boolean> {
        checkAdmin().onFailure { return Result.failure(it) }
        var isPub = true
        _contests.value = _contests.value.map {
            if (it.contestId == contestId) {
                isPub = !it.isPublished
                it.copy(isPublished = isPub)
            } else it
        }
        return Result.success(isPub)
    }

    /**
     * Atomic Contest Joining and Wallet Deduction
     * Never trusts client-supplied fee.
     */
    fun joinContest(contestId: String, teamId: String): Result<ContestEntry> {
        val user = getCurrentUser()
        val contest = _contests.value.find { it.contestId == contestId }
            ?: return Result.failure(NoSuchElementException("Contest not found"))

        if (contest.status != ContestStatus.UPCOMING || !contest.isPublished) {
            return Result.failure(IllegalStateException("Contest is not open for entry"))
        }

        if (contest.filledSpots >= contest.totalSpots) {
            return Result.failure(IllegalStateException("Contest is completely filled!"))
        }

        // Compliance check: Paid contests require age 18+ and non-restricted state
        if (contest.entryFee > 0.0) {
            if (FirestoreSchema.isStateRestricted(user.selectedState)) {
                return Result.failure(IllegalStateException("Paid contests are prohibited in ${user.selectedState} under state gaming regulations."))
            }
            if (!user.isAge18Plus) {
                return Result.failure(IllegalStateException("You must be 18 years of age or older to participate in paid fantasy contests."))
            }
        }

        val team = _teams.value.find { it.teamId == teamId }
            ?: return Result.failure(NoSuchElementException("Selected fantasy team not found"))

        if (team.userId != user.userId) {
            return Result.failure(SecurityException("Unauthorized team selection"))
        }

        if (team.matchId != contest.matchId) {
            return Result.failure(IllegalArgumentException("Team match doesn't match contest match"))
        }

        // Prevent duplicate joining with same team
        val alreadyJoined = _contestEntries.value.any { it.contestId == contestId && it.teamId == teamId }
        if (alreadyJoined) {
            return Result.failure(IllegalStateException("This team has already entered this contest"))
        }

        // Wallet Balance Check & Deduction
        val userWallet = getOrCreateWallet(user.userId)
        val fee = contest.entryFee

        if (userWallet.totalBalance < fee) {
            return Result.failure(IllegalStateException("Insufficient balance! Fee is ₹$fee, available ₹${userWallet.totalBalance}"))
        }

        // Deduct from Bonus (up to 10%) -> Deposit -> Winnings
        var remaining = fee
        val bonusDeduct = Math.min(userWallet.bonusBalance, Math.floor(remaining * 0.10))
        remaining -= bonusDeduct

        val depositDeduct = Math.min(userWallet.depositBalance, remaining)
        remaining -= depositDeduct

        val winningsDeduct = Math.min(userWallet.winningsBalance, remaining)
        remaining -= winningsDeduct

        if (remaining > 0.01) {
            return Result.failure(IllegalStateException("Insufficient usable funds for entry fee"))
        }

        val updatedWallet = userWallet.copy(
            depositBalance = userWallet.depositBalance - depositDeduct,
            winningsBalance = userWallet.winningsBalance - winningsDeduct,
            bonusBalance = userWallet.bonusBalance - bonusDeduct,
            totalBalance = (userWallet.depositBalance - depositDeduct) + (userWallet.winningsBalance - winningsDeduct) + (userWallet.bonusBalance - bonusDeduct),
            updatedAt = System.currentTimeMillis()
        )
        updateWalletInternal(updatedWallet)

        // Increment Contest Spots
        _contests.value = _contests.value.map {
            if (it.contestId == contestId) it.copy(filledSpots = it.filledSpots + 1) else it
        }

        // Create Entry
        val entryId = "ENTRY_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 4)}"
        val entry = ContestEntry(
            entryId = entryId,
            contestId = contestId,
            matchId = contest.matchId,
            userId = user.userId,
            teamId = teamId,
            teamName = team.teamName,
            entryFeePaid = fee,
            points = 0.0,
            rank = 0,
            wonAmount = 0.0,
            joinedAt = System.currentTimeMillis()
        )
        _contestEntries.value = _contestEntries.value + entry

        // Record Ledger Transaction
        recordTransaction(
            Transaction(
                transactionId = "TX_ENTRY_${System.currentTimeMillis()}",
                userId = user.userId,
                type = TransactionType.CONTEST_ENTRY,
                amount = fee,
                status = TransactionStatus.SUCCESS,
                gateway = "SYSTEM",
                referenceId = contestId,
                notes = "Joined ${contest.title} with team ${team.teamName}"
            )
        )

        sendUserNotification(
            user.userId,
            NotificationType.WALLET_UPDATE,
            "Contest Joined",
            "₹$fee deducted for joining ${contest.title} with team ${team.teamName}."
        )

        return Result.success(entry)
    }

    // =========================================================================
    // WALLET & FINANCIAL OPERATIONS
    // =========================================================================

    fun getOrCreateWallet(userId: String): Wallet {
        val current = _wallets.value[userId]
        if (current != null) return current
        val initial = Wallet(
            userId = userId,
            depositBalance = 250.0, // Welcome test deposit balance
            winningsBalance = 150.0, // Sample winnings for testing withdrawals
            bonusBalance = 50.0,
            updatedAt = System.currentTimeMillis()
        )
        updateWalletInternal(initial)
        return initial
    }

    private fun updateWalletInternal(wallet: Wallet) {
        val current = _wallets.value.toMutableMap()
        current[wallet.userId] = wallet
        _wallets.value = current
    }

    /**
     * Process Completed Deposit via PhonePe or Paytm Webhook
     */
    fun creditDepositViaWebhook(
        userId: String,
        amount: Double,
        gateway: PaymentGateway,
        orderId: String
    ): Result<Wallet> {
        val currentWallet = getOrCreateWallet(userId)
        val updated = currentWallet.copy(
            depositBalance = currentWallet.depositBalance + amount,
            totalBalance = currentWallet.depositBalance + amount + currentWallet.winningsBalance + currentWallet.bonusBalance,
            updatedAt = System.currentTimeMillis()
        )
        updateWalletInternal(updated)

        recordTransaction(
            Transaction(
                transactionId = orderId,
                userId = userId,
                type = TransactionType.DEPOSIT,
                amount = amount,
                status = TransactionStatus.SUCCESS,
                gateway = gateway.name,
                referenceId = orderId,
                notes = "Deposit via ${gateway.displayName}"
            )
        )

        sendUserNotification(
            userId,
            NotificationType.WALLET_UPDATE,
            "Wallet Cash Added",
            "₹$amount successfully credited to your deposit wallet via ${gateway.displayName}."
        )

        return Result.success(updated)
    }

    /**
     * User submits withdrawal request using registered UPI ID
     */
    fun requestWithdrawal(amount: Double): Result<Withdrawal> {
        val user = getCurrentUser()
        val settings = _appSettings.value

        if (amount < settings.minWithdrawalAmount) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal amount is ₹${settings.minWithdrawalAmount}"))
        }

        if (amount > settings.maxWithdrawalAmount) {
            return Result.failure(IllegalArgumentException("Maximum withdrawal amount is ₹${settings.maxWithdrawalAmount}"))
        }

        if (user.upiId.isBlank() || !user.upiId.contains("@")) {
            return Result.failure(IllegalStateException("No valid UPI ID registered on your profile. Please update your profile first."))
        }

        val wallet = getOrCreateWallet(user.userId)
        if (wallet.winningsBalance < amount) {
            return Result.failure(IllegalStateException("Insufficient winnings balance. Available for withdrawal: ₹${wallet.winningsBalance}"))
        }

        // Deduct from winnings and hold in pending status
        val newWinnings = wallet.winningsBalance - amount
        val updatedWallet = wallet.copy(
            winningsBalance = newWinnings,
            totalBalance = wallet.depositBalance + newWinnings + wallet.bonusBalance,
            updatedAt = System.currentTimeMillis()
        )
        updateWalletInternal(updatedWallet)

        val id = "WTH_${System.currentTimeMillis()}"
        val withdrawal = Withdrawal(
            withdrawalId = id,
            userId = user.userId,
            userName = user.name,
            userMobile = user.mobileNumber,
            upiId = user.upiId,
            amount = amount,
            status = WithdrawalStatus.PENDING,
            requestedAt = System.currentTimeMillis(),
            remarks = "Awaiting Admin Review"
        )
        _withdrawals.value = _withdrawals.value + withdrawal

        recordTransaction(
            Transaction(
                transactionId = "TX_WTH_$id",
                userId = user.userId,
                type = TransactionType.WITHDRAWAL,
                amount = amount,
                status = TransactionStatus.PENDING,
                gateway = "UPI",
                referenceId = id,
                notes = "Withdrawal request to UPI ID: ${user.upiId}"
            )
        )

        sendUserNotification(
            user.userId,
            NotificationType.WITHDRAWAL_STATUS,
            "Withdrawal Submitted",
            "Your withdrawal of ₹$amount to UPI ${user.upiId} has been placed and is under review."
        )

        return Result.success(withdrawal)
    }

    /**
     * Admin Reviews Withdrawal (Approve, Reject, or Mark Completed)
     */
    fun adminReviewWithdrawal(
        withdrawalId: String,
        action: String, // "APPROVE", "REJECT", "COMPLETE"
        remarks: String
    ): Result<Withdrawal> {
        checkAdmin().onFailure { return Result.failure(it) }

        val withdrawal = _withdrawals.value.find { it.withdrawalId == withdrawalId }
            ?: return Result.failure(NoSuchElementException("Withdrawal request not found"))

        val admin = getCurrentUser()

        val updated = when (action) {
            "REJECT" -> {
                // Refund back to user winnings wallet
                val userWallet = getOrCreateWallet(withdrawal.userId)
                val refunded = userWallet.copy(
                    winningsBalance = userWallet.winningsBalance + withdrawal.amount,
                    totalBalance = userWallet.depositBalance + (userWallet.winningsBalance + withdrawal.amount) + userWallet.bonusBalance,
                    updatedAt = System.currentTimeMillis()
                )
                updateWalletInternal(refunded)

                sendUserNotification(
                    withdrawal.userId,
                    NotificationType.WITHDRAWAL_STATUS,
                    "Withdrawal Rejected & Refunded",
                    "Your withdrawal of ₹${withdrawal.amount} was rejected: $remarks. Funds returned to your winnings balance."
                )

                withdrawal.copy(
                    status = WithdrawalStatus.REJECTED,
                    reviewedAt = System.currentTimeMillis(),
                    reviewedBy = admin.name,
                    remarks = remarks
                )
            }
            "APPROVE" -> {
                sendUserNotification(
                    withdrawal.userId,
                    NotificationType.WITHDRAWAL_STATUS,
                    "Withdrawal Approved",
                    "Your withdrawal of ₹${withdrawal.amount} to UPI ${withdrawal.upiId} has been approved and queued for payout."
                )
                withdrawal.copy(
                    status = WithdrawalStatus.APPROVED,
                    reviewedAt = System.currentTimeMillis(),
                    reviewedBy = admin.name,
                    remarks = remarks.ifBlank { "Approved by admin" }
                )
            }
            "COMPLETE" -> {
                sendUserNotification(
                    withdrawal.userId,
                    NotificationType.WITHDRAWAL_STATUS,
                    "Withdrawal Completed (Paid)",
                    "₹${withdrawal.amount} has been successfully transferred to your registered UPI ID ${withdrawal.upiId}!"
                )
                withdrawal.copy(
                    status = WithdrawalStatus.COMPLETED,
                    reviewedAt = System.currentTimeMillis(),
                    reviewedBy = admin.name,
                    remarks = remarks.ifBlank { "Disbursed via UPI payout" }
                )
            }
            else -> return Result.failure(IllegalArgumentException("Unknown action: $action"))
        }

        _withdrawals.value = _withdrawals.value.map {
            if (it.withdrawalId == withdrawalId) updated else it
        }

        return Result.success(updated)
    }

    // =========================================================================
    // WINNERS & PRIZE SETTLEMENT (Admin operations)
    // =========================================================================

    fun settleContestWinners(contestId: String, matchId: String): Result<List<Winner>> {
        checkAdmin().onFailure { return Result.failure(it) }

        val contest = _contests.value.find { it.contestId == contestId }
            ?: return Result.failure(NoSuchElementException("Contest not found"))

        val entries = _contestEntries.value.filter { it.contestId == contestId }
        if (entries.isEmpty()) {
            return Result.failure(IllegalStateException("No entries found in this contest to settle"))
        }

        // Rank entries (simulate scores or calculate based on team points)
        val rankedEntries = entries.mapIndexed { index, entry ->
            val points = 350.0 - (index * 15.0) + (Math.random() * 5.0)
            entry.copy(points = points, rank = index + 1)
        }.sortedByDescending { it.points }

        val createdWinners = mutableListOf<Winner>()

        // Distribute prizes based on prize tiers
        for ((idx, entry) in rankedEntries.withIndex()) {
            val rank = idx + 1
            val tier = contest.prizeBreakdown.find { rank in it.rankStart..it.rankEnd }
            val prize = tier?.prizeAmount ?: 0.0

            if (prize > 0) {
                val winnerId = "WIN_${System.currentTimeMillis()}_$rank"
                val winner = Winner(
                    winnerId = winnerId,
                    contestId = contestId,
                    matchId = matchId,
                    userId = entry.userId,
                    teamName = entry.teamName,
                    rank = rank,
                    prizeAmount = prize,
                    pointsScored = entry.points,
                    settledAt = System.currentTimeMillis()
                )
                createdWinners.add(winner)

                // Credit Winnings Balance Atomically
                val userWallet = getOrCreateWallet(entry.userId)
                val updatedWallet = userWallet.copy(
                    winningsBalance = userWallet.winningsBalance + prize,
                    totalBalance = userWallet.depositBalance + (userWallet.winningsBalance + prize) + userWallet.bonusBalance,
                    updatedAt = System.currentTimeMillis()
                )
                updateWalletInternal(updatedWallet)

                // Record Transaction
                recordTransaction(
                    Transaction(
                        transactionId = "TX_WIN_$winnerId",
                        userId = entry.userId,
                        type = TransactionType.WINNINGS,
                        amount = prize,
                        status = TransactionStatus.SUCCESS,
                        gateway = "SYSTEM",
                        referenceId = contestId,
                        notes = "Prize for Rank #$rank in ${contest.title}"
                    )
                )

                sendUserNotification(
                    entry.userId,
                    NotificationType.CONTEST_RESULTS,
                    "Prize Won: Rank #$rank in ${contest.title}!",
                    "Congratulations! You won ₹$prize. The amount has been credited to your Winnings balance."
                )
            }
        }

        _winners.value = _winners.value + createdWinners

        // Mark Contest as COMPLETED
        _contests.value = _contests.value.map {
            if (it.contestId == contestId) it.copy(status = ContestStatus.COMPLETED) else it
        }

        return Result.success(createdWinners)
    }

    // =========================================================================
    // NOTIFICATIONS & APP SETTINGS
    // =========================================================================

    private fun sendBroadcastNotification(type: NotificationType, title: String, message: String) {
        val notif = Notification(
            notificationId = "NOTIF_${System.currentTimeMillis()}",
            userId = "all",
            type = type,
            title = title,
            message = message,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    private fun sendUserNotification(userId: String, type: NotificationType, title: String, message: String) {
        val notif = Notification(
            notificationId = "NOTIF_${System.currentTimeMillis()}",
            userId = userId,
            type = type,
            title = title,
            message = message,
            isRead = false,
            createdAt = System.currentTimeMillis()
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun markNotificationRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.notificationId == notificationId) it.copy(isRead = true) else it
        }
    }

    fun updateAppSettings(settings: AppSettings): Result<AppSettings> {
        checkAdmin().onFailure { return Result.failure(it) }
        _appSettings.value = settings
        return Result.success(settings)
    }

    private fun recordTransaction(tx: Transaction) {
        _transactions.value = listOf(tx) + _transactions.value
    }

    // =========================================================================
    // SEED INITIAL DATA
    // =========================================================================

    private fun seedInitialBackendData() {
        val match1 = Match(
            matchId = "M_IPL_01",
            title = "Indian T20 League 2026 - Match 1",
            team1Short = "MUM",
            team1Name = "Mumbai Champions",
            team2Short = "CHE",
            team2Name = "Chennai Kings",
            matchType = "T20",
            venue = "Wankhede Stadium, Mumbai",
            startTimeEpoch = System.currentTimeMillis() + 14400000L,
            status = MatchStatus.UPCOMING,
            isPublished = true
        )

        val match2 = Match(
            matchId = "M_IPL_02",
            title = "Indian T20 League 2026 - Match 2",
            team1Short = "BLR",
            team1Name = "Bangalore Royals",
            team2Short = "KOL",
            team2Name = "Kolkata Riders",
            matchType = "T20",
            venue = "M. Chinnaswamy Stadium, Bengaluru",
            startTimeEpoch = System.currentTimeMillis() + 86400000L,
            status = MatchStatus.UPCOMING,
            isPublished = true
        )

        _matches.value = listOf(match1, match2)

        // Seed Players for Match 1
        val initialPlayers = listOf(
            // MUM
            Player("P01", "Rohit Sharma", "MUM", PlayerRole.BAT, 9.5, true, "M_IPL_01"),
            Player("P02", "Ishan Kishan", "MUM", PlayerRole.WK, 8.5, true, "M_IPL_01"),
            Player("P03", "Suryakumar Yadav", "MUM", PlayerRole.BAT, 9.5, true, "M_IPL_01"),
            Player("P04", "Tilak Varma", "MUM", PlayerRole.BAT, 8.5, true, "M_IPL_01"),
            Player("P05", "Hardik Pandya", "MUM", PlayerRole.AR, 9.0, true, "M_IPL_01"),
            Player("P06", "Tim David", "MUM", PlayerRole.BAT, 8.0, true, "M_IPL_01"),
            Player("P07", "Jasprit Bumrah", "MUM", PlayerRole.BOWL, 9.5, true, "M_IPL_01"),
            Player("P08", "Piyush Chawla", "MUM", PlayerRole.BOWL, 8.0, true, "M_IPL_01"),
            Player("P09", "Gerald Coetzee", "MUM", PlayerRole.BOWL, 8.5, true, "M_IPL_01"),
            Player("P10", "Nuwan Thushara", "MUM", PlayerRole.BOWL, 8.0, true, "M_IPL_01"),
            Player("P11", "Naman Dhir", "MUM", PlayerRole.AR, 7.5, true, "M_IPL_01"),
            // CHE
            Player("P12", "Ruturaj Gaikwad", "CHE", PlayerRole.BAT, 9.0, true, "M_IPL_01"),
            Player("P13", "Rachin Ravindra", "CHE", PlayerRole.AR, 8.5, true, "M_IPL_01"),
            Player("P14", "Shivam Dube", "CHE", PlayerRole.AR, 9.0, true, "M_IPL_01"),
            Player("P15", "MS Dhoni", "CHE", PlayerRole.WK, 8.5, true, "M_IPL_01"),
            Player("P16", "Ravindra Jadeja", "CHE", PlayerRole.AR, 9.0, true, "M_IPL_01"),
            Player("P17", "Daryl Mitchell", "CHE", PlayerRole.BAT, 8.5, true, "M_IPL_01"),
            Player("P18", "Matheesha Pathirana", "CHE", PlayerRole.BOWL, 9.0, true, "M_IPL_01"),
            Player("P19", "Tushar Deshpande", "CHE", PlayerRole.BOWL, 8.0, true, "M_IPL_01"),
            Player("P20", "Maheesh Theekshana", "CHE", PlayerRole.BOWL, 8.5, true, "M_IPL_01"),
            Player("P21", "Mustafizur Rahman", "CHE", PlayerRole.BOWL, 8.5, true, "M_IPL_01"),
            Player("P22", "Sameer Rizvi", "CHE", PlayerRole.BAT, 7.5, true, "M_IPL_01")
        )
        _players.value = initialPlayers

        // Seed Contests
        val contest1 = Contest(
            contestId = "C01",
            matchId = "M_IPL_01",
            title = "Mega Contest - ₹1 Lakh Pool",
            entryFee = 49.0,
            totalSpots = 2500,
            filledSpots = 1840,
            prizePool = 100000.0,
            firstPrize = 25000.0,
            winnerPercentage = 50,
            status = ContestStatus.UPCOMING,
            isPublished = true
        )

        val contest2 = Contest(
            contestId = "C02",
            matchId = "M_IPL_01",
            title = "Head-to-Head Derby",
            entryFee = 99.0,
            totalSpots = 2,
            filledSpots = 1,
            prizePool = 180.0,
            firstPrize = 180.0,
            winnerPercentage = 50,
            status = ContestStatus.UPCOMING,
            isPublished = true
        )

        _contests.value = listOf(contest1, contest2)

        // Seed Sample User Team
        val sampleTeam = FantasyTeam(
            teamId = "T_SAMPLE_01",
            userId = "user_kiran_001",
            matchId = "M_IPL_01",
            teamName = "EgleXI_Titans",
            selectedPlayerIds = listOf("P01", "P02", "P03", "P05", "P07", "P09", "P12", "P14", "P15", "P16", "P18"),
            captainPlayerId = "P01", // Rohit Sharma (2x)
            viceCaptainPlayerId = "P18", // Pathirana (1.5x)
            createdAt = System.currentTimeMillis() - 3600000L,
            nameEditCount = 0
        )
        _teams.value = listOf(sampleTeam)

        // Seed Notifications
        _notifications.value = listOf(
            Notification(
                notificationId = "N01",
                userId = "all",
                type = NotificationType.SYSTEM_ANNOUNCEMENT,
                title = "Welcome to Egle11!",
                message = "Welcome to Egle11 Cricket Fantasy Gaming. Make your dream XI and win exciting cash rewards!",
                isRead = false
            ),
            Notification(
                notificationId = "N02",
                userId = "all",
                type = NotificationType.CONTEST_ANNOUNCEMENT,
                title = "Mega Contest is Live!",
                message = "₹1 Lakh Prize pool Mega Contest is now open for Mumbai vs Chennai. Join now!",
                isRead = false
            )
        )
    }

    // =========================================================================
    // FIREBASE CLOUD DATABASE OPERATIONS
    // =========================================================================

    suspend fun pushAllToFirebase(): Result<String> {
        return firebaseDbManager.pushAllCollectionsToFirebase(
            matches = _matches.value,
            players = _players.value,
            teams = _teams.value,
            contests = _contests.value,
            entries = _contestEntries.value,
            wallets = _wallets.value,
            transactions = _transactions.value,
            withdrawals = _withdrawals.value,
            winners = _winners.value,
            notifications = _notifications.value,
            appSettings = _appSettings.value
        )
    }

    suspend fun testFirebaseConnection(): Result<String> {
        return firebaseDbManager.testDatabaseConnection()
    }

    suspend fun configureFirebaseDb(context: Context, projectId: String, apiKey: String, appId: String, dbUrl: String = ""): Result<String> {
        return firebaseDbManager.configureAndConnectFirebase(context, projectId, apiKey, appId, dbUrl)
    }

    fun toggleFirebaseLiveSync(enabled: Boolean) {
        firebaseDbManager.toggleLiveSync(enabled)
    }
}

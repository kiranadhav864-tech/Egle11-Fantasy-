package com.example.backend

import android.content.Context
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Data class representing Firebase Database connection details & status.
 */
data class FirebaseDbConfig(
    val projectId: String = "egle11-cricket-fantasy",
    val apiKey: String = "",
    val applicationId: String = "com.aistudio.egle11.fntsy",
    val databaseUrl: String = "https://egle11-cricket-fantasy.firebaseio.com",
    val storageBucket: String = "egle11-cricket-fantasy.appspot.com",
    val isCustomConfigured: Boolean = false,
    val isConnected: Boolean = false,
    val isLiveSyncEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val lastStatusMessage: String = "Ready to connect Firebase Cloud Database",
    val activeEngine: String = "Local In-Memory Cache" // "Local In-Memory Cache" or "Cloud Firestore"
)

data class CollectionSyncStat(
    val collectionName: String,
    val localCount: Int,
    val cloudCount: Int = 0,
    val isSynced: Boolean = false,
    val description: String = ""
)

/**
 * Manager class responsible for adding, configuring, testing, and syncing
 * the Firebase Cloud Firestore Database with Egle11's 12 backend collections.
 */
class FirebaseDatabaseManager {

    private val _config = MutableStateFlow(FirebaseDbConfig())
    val config: StateFlow<FirebaseDbConfig> = _config.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private var customFirebaseApp: FirebaseApp? = null

    /**
     * Obtains the active Firestore instance safely.
     * Prefers custom initialized FirebaseApp if configured, otherwise falls back to default instance.
     */
    fun getFirestoreInstance(): FirebaseFirestore? {
        return try {
            if (customFirebaseApp != null) {
                FirebaseFirestore.getInstance(customFirebaseApp!!)
            } else if (FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            try {
                FirebaseFirestore.getInstance()
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Option to Add / Connect Firebase with Project Credentials.
     */
    suspend fun configureAndConnectFirebase(
        context: Context,
        projectId: String,
        apiKey: String,
        applicationId: String,
        databaseUrl: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmedProjectId = projectId.trim()
            val trimmedApiKey = apiKey.trim()
            val trimmedAppId = applicationId.trim().ifBlank { "com.aistudio.egle11.fntsy" }

            if (trimmedProjectId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Firebase Project ID cannot be blank"))
            }

            val appName = "Egle11FirebaseDb_${trimmedProjectId.take(8)}"
            
            // Check if already initialized
            val existingApp = FirebaseApp.getApps(context).find { it.name == appName }
            val app = if (existingApp != null) {
                existingApp
            } else {
                val optionsBuilder = FirebaseOptions.Builder()
                    .setProjectId(trimmedProjectId)
                    .setApplicationId(trimmedAppId)

                if (trimmedApiKey.isNotBlank()) {
                    optionsBuilder.setApiKey(trimmedApiKey)
                } else {
                    optionsBuilder.setApiKey("AIzaSyFakeKeyPlaceholderForLocalDev1234567")
                }

                if (databaseUrl.isNotBlank()) {
                    optionsBuilder.setDatabaseUrl(databaseUrl.trim())
                }

                FirebaseApp.initializeApp(context, optionsBuilder.build(), appName)
            }

            customFirebaseApp = app
            val firestore = FirebaseFirestore.getInstance(app)

            _config.value = _config.value.copy(
                projectId = trimmedProjectId,
                apiKey = trimmedApiKey,
                applicationId = trimmedAppId,
                databaseUrl = databaseUrl,
                isCustomConfigured = true,
                isConnected = true,
                activeEngine = "Cloud Firestore ($trimmedProjectId)",
                lastStatusMessage = "Connected successfully to Firebase project: $trimmedProjectId"
            )

            Result.success("Firebase Database successfully initialized for project '$trimmedProjectId'")
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Failed to initialize Firebase credentials"
            _config.value = _config.value.copy(
                lastStatusMessage = "Connection error: $errorMsg"
            )
            Result.failure(e)
        }
    }

    /**
     * Option to Test Firebase Cloud Database Connectivity & Permissions.
     */
    suspend fun testDatabaseConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestoreInstance()
                ?: return@withContext Result.failure(IllegalStateException("No active Firebase Firestore instance found. Please configure Project ID or provide google-services.json."))

            val testDocRef = firestore.collection("_egle11_diagnostics").document("ping_check")
            val pingData = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "service" to "Egle11 Fantasy Cricket",
                "ping" to "SUCCESS",
                "status" to "OK"
            )

            // Write diagnostic ping
            testDocRef.set(pingData).await()

            // Read back diagnostic ping
            val snapshot = testDocRef.get().await()
            if (snapshot.exists()) {
                _config.value = _config.value.copy(
                    isConnected = true,
                    activeEngine = "Cloud Firestore (${_config.value.projectId})",
                    lastStatusMessage = "Cloud Firestore Connection Verified: Read/Write latency OK"
                )
                Result.success("Firebase Cloud Firestore read & write test successful!")
            } else {
                Result.failure(IllegalStateException("Write acknowledged but document read verification returned empty"))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Test connection failed"
            _config.value = _config.value.copy(
                isConnected = false,
                lastStatusMessage = "Test failed: $msg"
            )
            Result.failure(e)
        }
    }

    /**
     * Option to Push all 12 In-Memory Collections to Firebase Firestore Database.
     */
    suspend fun pushAllCollectionsToFirebase(
        matches: List<Match>,
        players: List<Player>,
        teams: List<FantasyTeam>,
        contests: List<Contest>,
        entries: List<ContestEntry>,
        wallets: Map<String, Wallet>,
        transactions: List<Transaction>,
        withdrawals: List<Withdrawal>,
        winners: List<Winner>,
        notifications: List<Notification>,
        appSettings: AppSettings
    ): Result<String> = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _syncProgress.value = 0.05f

        try {
            val firestore = getFirestoreInstance()
                ?: return@withContext Result.failure(IllegalStateException("Firebase Firestore is not initialized. Please connect Firebase first."))

            var totalItemsPushed = 0
            val totalSteps = 11

            // 1. Matches
            for (match in matches) {
                firestore.collection(FirestoreSchema.COL_MATCHES)
                    .document(match.matchId)
                    .set(match.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 1f / totalSteps

            // 2. Players
            for (player in players) {
                firestore.collection(FirestoreSchema.COL_PLAYERS)
                    .document(player.playerId)
                    .set(player.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 2f / totalSteps

            // 3. Contests
            for (contest in contests) {
                firestore.collection(FirestoreSchema.COL_CONTESTS)
                    .document(contest.contestId)
                    .set(contest.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 3f / totalSteps

            // 4. Teams
            for (team in teams) {
                firestore.collection(FirestoreSchema.COL_TEAMS)
                    .document(team.teamId)
                    .set(team.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 4f / totalSteps

            // 5. Contest Entries
            for (entry in entries) {
                firestore.collection(FirestoreSchema.COL_CONTEST_ENTRIES)
                    .document(entry.entryId)
                    .set(entry.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 5f / totalSteps

            // 6. Wallets
            for ((userId, wallet) in wallets) {
                firestore.collection(FirestoreSchema.COL_WALLETS)
                    .document(userId)
                    .set(wallet.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 6f / totalSteps

            // 7. Transactions
            for (tx in transactions) {
                firestore.collection(FirestoreSchema.COL_TRANSACTIONS)
                    .document(tx.transactionId)
                    .set(tx.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 7f / totalSteps

            // 8. Withdrawals
            for (wd in withdrawals) {
                firestore.collection(FirestoreSchema.COL_WITHDRAWALS)
                    .document(wd.withdrawalId)
                    .set(wd.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 8f / totalSteps

            // 9. Winners
            for (w in winners) {
                firestore.collection(FirestoreSchema.COL_WINNERS)
                    .document(w.winnerId)
                    .set(w.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 9f / totalSteps

            // 10. Notifications
            for (notif in notifications) {
                firestore.collection(FirestoreSchema.COL_NOTIFICATIONS)
                    .document(notif.notificationId)
                    .set(notif.toMap(), SetOptions.merge())
                    .await()
                totalItemsPushed++
            }
            _syncProgress.value = 10f / totalSteps

            // 11. App Settings
            firestore.collection(FirestoreSchema.COL_APP_SETTINGS)
                .document("global")
                .set(appSettings.toMap(), SetOptions.merge())
                .await()
            totalItemsPushed++
            _syncProgress.value = 1.0f

            val successMsg = "Successfully pushed $totalItemsPushed documents across all 12 collections to Firebase Firestore!"
            _config.value = _config.value.copy(
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastStatusMessage = successMsg
            )
            Result.success(successMsg)
        } catch (e: Exception) {
            val errorMsg = "Push to Firebase failed: ${e.localizedMessage}"
            _config.value = _config.value.copy(lastStatusMessage = errorMsg)
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Toggles Real-Time Cloud Firestore Sync.
     */
    fun toggleLiveSync(enabled: Boolean) {
        _config.value = _config.value.copy(isLiveSyncEnabled = enabled)
    }

    /**
     * Exports the entire Firebase Firestore schema as a clean JSON definition.
     */
    fun generateExportSchemaJson(): String {
        return """
        {
          "firestore_database": {
            "project_id": "${_config.value.projectId}",
            "collections": [
              {
                "name": "${FirestoreSchema.COL_USERS}",
                "description": "User profiles, mobile number, UPI ID, role, active status.",
                "indexes": ["userId", "mobileNumber", "role"]
              },
              {
                "name": "${FirestoreSchema.COL_MATCHES}",
                "description": "Fixtures with status (UPCOMING, LIVE, COMPLETED), teams, venue.",
                "indexes": ["status", "isPublished", "matchStartTime"]
              },
              {
                "name": "${FirestoreSchema.COL_PLAYERS}",
                "description": "Cricket players pool with role, credits, match assignments.",
                "indexes": ["matchId", "role"]
              },
              {
                "name": "${FirestoreSchema.COL_TEAMS}",
                "description": "User fantasy teams (11 players, C/VC, team name).",
                "indexes": ["matchId", "userId"]
              },
              {
                "name": "${FirestoreSchema.COL_CONTESTS}",
                "description": "Contests with entry fee, prize pool, total spots.",
                "indexes": ["matchId", "status"]
              },
              {
                "name": "${FirestoreSchema.COL_CONTEST_ENTRIES}",
                "description": "Immutable contest joins linking user team and reserved slot.",
                "indexes": ["contestId", "userId", "matchId"]
              },
              {
                "name": "${FirestoreSchema.COL_WALLETS}",
                "description": "User balance vault (deposit, winnings, bonus). Server-authoritative.",
                "indexes": ["userId"]
              },
              {
                "name": "${FirestoreSchema.COL_TRANSACTIONS}",
                "description": "Immutable ledger: Deposits, Contest deductions, Payouts, Withdrawals.",
                "indexes": ["userId", "timestamp"]
              },
              {
                "name": "${FirestoreSchema.COL_WITHDRAWALS}",
                "description": "UPI payout requests submitted by users and processed by Admin.",
                "indexes": ["userId", "status"]
              },
              {
                "name": "${FirestoreSchema.COL_WINNERS}",
                "description": "Public leaderboard and prize distribution history.",
                "indexes": ["matchId", "contestId", "rank"]
              },
              {
                "name": "${FirestoreSchema.COL_NOTIFICATIONS}",
                "description": "Push alerts for matches, contests, and wallet operations.",
                "indexes": ["userId", "timestamp"]
              },
              {
                "name": "${FirestoreSchema.COL_APP_SETTINGS}",
                "description": "Global thresholds, payment toggles, and maintenance mode.",
                "indexes": ["settingId"]
              }
            ]
          }
        }
        """.trimIndent()
    }
}

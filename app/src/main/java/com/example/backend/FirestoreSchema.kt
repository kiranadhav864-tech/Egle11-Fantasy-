package com.example.backend

/**
 * Centralized Firestore Schema definition for Egle11 cricket fantasy platform.
 * 
 * Collections:
 * 1. users
 * 2. matches
 * 3. players
 * 4. teams
 * 5. contests
 * 6. contest_entries
 * 7. wallets
 * 8. transactions
 * 9. withdrawals
 * 10. winners
 * 11. notifications
 * 12. app_settings
 */
object FirestoreSchema {
    const val COL_USERS = "users"
    const val COL_MATCHES = "matches"
    const val COL_PLAYERS = "players"
    const val COL_TEAMS = "teams"
    const val COL_CONTESTS = "contests"
    const val COL_CONTEST_ENTRIES = "contest_entries"
    const val COL_WALLETS = "wallets"
    const val COL_TRANSACTIONS = "transactions"
    const val COL_WITHDRAWALS = "withdrawals"
    const val COL_WINNERS = "winners"
    const val COL_NOTIFICATIONS = "notifications"
    const val COL_APP_SETTINGS = "app_settings"

    val ALL_COLLECTIONS = listOf(
        COL_USERS to "User profiles, mobile number, UPI ID, role (user/admin), active status. No Aadhaar/PAN.",
        COL_MATCHES to "Fixtures managed by Admin (Upcoming, Live, Completed, Publish/Unpublish toggle).",
        COL_PLAYERS to "Cricket players pool, role (WK, BAT, AR, BOWL), credits, match assignments, availability.",
        COL_TEAMS to "Fantasy teams created by users (11 players, C/VC, team name uniqueness, 1-edit constraint).",
        COL_CONTESTS to "Contests created by Admin (entry fee, total spots, prize pool, prize breakdown).",
        COL_CONTEST_ENTRIES to "Immutable contest joins linking user team with atomic slot reservation and fee ledger.",
        COL_WALLETS to "User balance vault (deposit, winnings, bonus). Strictly protected from client writes.",
        COL_TRANSACTIONS to "Financial audit trail: Deposits (PhonePe/Paytm), Entry fee deductions, Winnings, Withdrawals.",
        COL_WITHDRAWALS to "UPI withdrawal requests submitted by users, reviewed & approved/completed by Admin.",
        COL_WINNERS to "Public leaderboard & prize payout records for completed contests.",
        COL_NOTIFICATIONS to "System alerts: new matches, contest updates, wallet events, withdrawal status.",
        COL_APP_SETTINGS to "Global configurations: min/max withdrawal thresholds, payment toggles, maintenance mode."
    )

    // Indian States list for registration with gaming restriction compliance
    // Restricted States: Assam, Telangana, Andhra Pradesh, Nagaland, Odisha, Sikkim, Tamil Nadu
    val RESTRICTED_STATES = listOf(
        "Assam", "Telangana", "Andhra Pradesh", "Nagaland", "Odisha", "Sikkim", "Tamil Nadu"
    )

    // Allowed States for Registration (Complying with Indian State Gaming Regulations)
    val ALLOWED_STATES = listOf(
        "Maharashtra", "Delhi", "Karnataka", "Uttar Pradesh",
        "Gujarat", "Rajasthan", "West Bengal", "Madhya Pradesh", "Haryana",
        "Punjab", "Bihar", "Jharkhand", "Kerala", "Chhattisgarh",
        "Uttarakhand", "Himachal Pradesh", "Goa", "Jammu and Kashmir",
        "Chandigarh", "Puducherry", "Tripura", "Meghalaya", "Manipur", "Mizoram"
    )

    // Full list for state dropdown showing restricted warnings if needed
    val INDIAN_STATES = ALLOWED_STATES

    fun isStateRestricted(state: String): Boolean {
        val clean = state.replace(" (Restricted)", "").replace(" (Not Allowed)", "").trim()
        return RESTRICTED_STATES.any { it.equals(clean, ignoreCase = true) } ||
                state.contains("Restricted", ignoreCase = true)
    }
}

package com.example.model

/**
 * Cricket Data API Decoupled Architecture
 * Keeps external sports data provider strictly separate from financial/wallet transactions.
 */
data class CricketMatchFixture(
    val externalMatchId: String,
    val seriesName: String,
    val matchDesc: String,
    val team1: CricketTeamData,
    val team2: CricketTeamData,
    val venue: String,
    val startTimestamp: Long,
    val matchFormat: String, // T20, ODI, Test
    val liveScore: CricketLiveScore? = null
)

data class CricketTeamData(
    val name: String,
    val shortName: String,
    val squad: List<CricketSquadPlayer> = emptyList()
)

data class CricketSquadPlayer(
    val externalPlayerId: String,
    val fullName: String,
    val roleString: String, // BAT, BOWL, ALL, WK
    val creditValue: Double = 8.5
)

data class CricketLiveScore(
    val team1Score: String = "",
    val team2Score: String = "",
    val statusText: String = "",
    val currentInnings: Int = 1,
    val oversBowled: Double = 0.0
)

/**
 * Standard Cricket Fantasy Point Calculation Matrix
 */
object FantasyPointRules {
    const val RUN_POINT = 1.0
    const val BOUNDARY_FOUR_BONUS = 1.0
    const val BOUNDARY_SIX_BONUS = 2.0
    const val HALF_CENTURY_BONUS = 8.0
    const val CENTURY_BONUS = 16.0
    const val DUCK_PENALTY = -2.0 // (Batsman/WK/AR)

    const val WICKET_POINT = 25.0
    const val LBW_BOWLED_BONUS = 8.0
    const val THREE_WICKET_BONUS = 4.0
    const val FOUR_WICKET_BONUS = 8.0
    const val FIVE_WICKET_BONUS = 16.0
    const val MAIDEN_OVER_POINT = 12.0

    const val CATCH_POINT = 8.0
    const val STUMPING_POINT = 12.0
    const val RUNOUT_DIRECT_POINT = 12.0

    const val CAPTAIN_MULTIPLIER = 2.0
    const val VICE_CAPTAIN_MULTIPLIER = 1.5
}

package com.example.backend

import com.example.model.CricketLiveScore
import com.example.model.CricketMatchFixture
import com.example.model.CricketSquadPlayer
import com.example.model.CricketTeamData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Decoupled Cricket Data Provider
 * Provides live match data, squads, and ball-by-ball updates without mixing financial logic.
 */
interface CricketApiService {
    suspend fun getUpcomingFixtures(): List<CricketMatchFixture>
    suspend fun getMatchSquad(externalMatchId: String): Pair<List<CricketSquadPlayer>, List<CricketSquadPlayer>>
    fun observeLiveScore(externalMatchId: String): Flow<CricketLiveScore>
}

/**
 * Standard Implementation of CricketApiService
 * Connectable to SportsMonks / CricketData.org / CricAPI or Mock Feed for staging.
 */
class StandardCricketApiService : CricketApiService {

    override suspend fun getUpcomingFixtures(): List<CricketMatchFixture> {
        return listOf(
            CricketMatchFixture(
                externalMatchId = "CRIC_IPL_2026_01",
                seriesName = "Indian T20 League 2026",
                matchDesc = "Match 1st: Mumbai Champions vs Chennai Kings",
                team1 = CricketTeamData(name = "Mumbai Champions", shortName = "MUM"),
                team2 = CricketTeamData(name = "Chennai Kings", shortName = "CHE"),
                venue = "Wankhede Stadium, Mumbai",
                startTimestamp = System.currentTimeMillis() + 3600000L * 4,
                matchFormat = "T20"
            ),
            CricketMatchFixture(
                externalMatchId = "CRIC_IPL_2026_02",
                seriesName = "Indian T20 League 2026",
                matchDesc = "Match 2nd: Bangalore Royals vs Kolkata Riders",
                team1 = CricketTeamData(name = "Bangalore Royals", shortName = "BLR"),
                team2 = CricketTeamData(name = "Kolkata Riders", shortName = "KOL"),
                venue = "M. Chinnaswamy Stadium, Bengaluru",
                startTimestamp = System.currentTimeMillis() + 3600000L * 28,
                matchFormat = "T20"
            )
        )
    }

    override suspend fun getMatchSquad(externalMatchId: String): Pair<List<CricketSquadPlayer>, List<CricketSquadPlayer>> {
        val team1Squad = listOf(
            CricketSquadPlayer("P_MUM_01", "Rohit Sharma", "BAT", 9.5),
            CricketSquadPlayer("P_MUM_02", "Ishan Kishan", "WK", 8.5),
            CricketSquadPlayer("P_MUM_03", "Suryakumar Yadav", "BAT", 9.0),
            CricketSquadPlayer("P_MUM_04", "Tilak Varma", "BAT", 8.5),
            CricketSquadPlayer("P_MUM_05", "Hardik Pandya", "ALL", 9.0),
            CricketSquadPlayer("P_MUM_06", "Tim David", "BAT", 8.0),
            CricketSquadPlayer("P_MUM_07", "Jasprit Bumrah", "BOWL", 9.5),
            CricketSquadPlayer("P_MUM_08", "Piyush Chawla", "BOWL", 8.0),
            CricketSquadPlayer("P_MUM_09", "Gerald Coetzee", "BOWL", 8.5),
            CricketSquadPlayer("P_MUM_10", "Nuwan Thushara", "BOWL", 8.0),
            CricketSquadPlayer("P_MUM_11", "Naman Dhir", "ALL", 7.5)
        )

        val team2Squad = listOf(
            CricketSquadPlayer("P_CHE_01", "Ruturaj Gaikwad", "BAT", 9.0),
            CricketSquadPlayer("P_CHE_02", "Rachin Ravindra", "ALL", 8.5),
            CricketSquadPlayer("P_CHE_03", "Shivam Dube", "ALL", 9.0),
            CricketSquadPlayer("P_CHE_04", "MS Dhoni", "WK", 8.5),
            CricketSquadPlayer("P_CHE_05", "Ravindra Jadeja", "ALL", 9.0),
            CricketSquadPlayer("P_CHE_06", "Daryl Mitchell", "BAT", 8.5),
            CricketSquadPlayer("P_CHE_07", "Matheesha Pathirana", "BOWL", 9.0),
            CricketSquadPlayer("P_CHE_08", "Tushar Deshpande", "BOWL", 8.0),
            CricketSquadPlayer("P_CHE_09", "Maheesh Theekshana", "BOWL", 8.5),
            CricketSquadPlayer("P_CHE_10", "Mustafizur Rahman", "BOWL", 8.5),
            CricketSquadPlayer("P_CHE_11", "Sameer Rizvi", "BAT", 7.5)
        )

        return Pair(team1Squad, team2Squad)
    }

    override fun observeLiveScore(externalMatchId: String): Flow<CricketLiveScore> = flow {
        emit(CricketLiveScore(team1Score = "178/4 (20.0)", team2Score = "132/3 (15.2)", statusText = "CHE need 47 runs in 28 balls", currentInnings = 2, oversBowled = 15.2))
    }
}

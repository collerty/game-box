package com.example.gamehub.features.whereandwhen.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.whereandwhe.model.WWPlayerRoundResult
import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState
import com.example.gamehub.features.whereandwhen.ui.arcadeFontFamily_WhereAndWhen
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import com.example.gamehub.R

/**
 * Dialog composable for displaying round results in Where & When.
 *
 * @param show Whether to show the dialog
 * @param currentRoundIndex The current round index
 * @param currentChallengeName The name of the current challenge/event
 * @param currentChallengeYear The correct year for the challenge
 * @param currentChallengeLocation The correct location name for the challenge
 * @param allPlayerResultsInfo Map of playerId to WWPlayerRoundResult
 * @param myPlayerId The current user's player ID
 * @param roomPlayers List of player info maps
 * @param onContinue Callback when the user clicks Continue
 */
@Composable
fun RoundResultsDialog(
    show: Boolean,
    currentRoundIndex: Int,
    currentChallengeName: String,
    currentChallengeYear: Int,
    currentChallengeLocation: String,
    allPlayerResultsInfo: Map<String, WWPlayerRoundResult>,
    myPlayerId: String,
    roomPlayers: List<Map<String, Any>>,
    onContinue: () -> Unit
) {
    if (!show) return
    val actualChallengeInfoName = currentChallengeName
    val actualChallengeInfoYear = currentChallengeYear
    val actualChallengeInfoLocation = currentChallengeLocation
    val sortedPlayerResults = allPlayerResultsInfo.entries.sortedByDescending { it.value.roundScore }
    val myResultData = allPlayerResultsInfo[myPlayerId]
    val iWonThisRound = myResultData != null && allPlayerResultsInfo.isNotEmpty() && myResultData.roundScore >= (allPlayerResultsInfo.values.maxOfOrNull { it.roundScore } ?: 0) && myResultData.roundScore > 0
    val dialogBorderColor = if (iWonThisRound) Color(0xFF27AE60) else Color(0xFFE74C3C)

    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(0.dp),
        containerColor = Color(0xFFDCDCDC),
        titleContentColor = Color.Black,
        textContentColor = Color.Black,
        modifier = Modifier.border(BorderStroke(4.dp, dialogBorderColor)),
        title = {
            Text(
                "ROUND ${currentRoundIndex + 1} RESULTS",
                fontFamily = arcadeFontFamily_WhereAndWhen,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                Text(
                    "EVENT: ${actualChallengeInfoName.uppercase()}",
                    fontFamily = arcadeFontFamily_WhereAndWhen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                Text(
                    "ACTUAL: $actualChallengeInfoYear - $actualChallengeInfoLocation",
                    fontFamily = arcadeFontFamily_WhereAndWhen,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                sortedPlayerResults.forEachIndexed { index, entry ->
                    val pId = entry.key
                    val result = entry.value
                    val playerName = roomPlayers.find { it["uid"] == pId }?.get("name") as? String ?: "Player"
                    val isCurrentUser = pId == myPlayerId
                    val playerPrefix = if (isCurrentUser) "YOUR" else playerName.uppercase()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .then(
                                if (isCurrentUser) Modifier.background(
                                    Color.Yellow.copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                ).padding(4.dp) else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$playerPrefix SCORE:",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                fontSize = 20.sp,
                                fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold
                            )
                            Text(
                                "${result.roundScore} PTS",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (result.roundScore > 0) Color(0xFF27AE60) else Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "  YEAR: ${result.guessedYear} (ACTUAL: $actualChallengeInfoYear)",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "-> ${result.yearScore} PTS",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val distKmStr = result.distanceKm?.let { "%.0f KM".format(it) } ?: "N/A"
                            Text(
                                "  LOCATION: $distKmStr",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "-> ${result.locationScore} PTS",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (result.timeRanOut) {
                            Text(
                                "  (TIME RAN OUT!)",
                                fontFamily = arcadeFontFamily_WhereAndWhen,
                                color = Color.Red.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (index < sortedPlayerResults.size - 1) {
                        Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White),
                border = BorderStroke(2.dp, Color.Black)
            ) {
                Text("Continue", fontFamily = arcadeFontFamily_WhereAndWhen)
            }
        }
    )
} 
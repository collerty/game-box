package com.example.gamehub

import MainMenu
import TriviatoeIntroAnimScreen
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.GamesListScreen
import com.example.gamehub.ui.SettingsScreen
import com.example.gamehub.ui.TestSensorsScreen
import com.example.gamehub.features.battleships.ui.BattleshipsPlayScreen
import com.example.gamehub.features.battleships.ui.MapVoteScreen
import com.example.gamehub.features.battleships.ui.ShipPlacementRoute
import com.example.gamehub.features.ohpardon.ui.OhPardonScreen
import com.example.gamehub.features.spy.ui.SpyScreen
import com.example.gamehub.features.jorisjump.ui.JorisJumpScreen
import com.example.gamehub.features.screamosaur.ui.ScreamosaurScreen
import com.example.gamehub.features.spaceinvaders.ui.SpaceInvadersPreGameScreen
import com.example.gamehub.features.spaceinvaders.ui.SpaceInvadersScreen
import com.example.gamehub.features.triviatoe.FirestoreTriviatoeSession
import com.example.gamehub.features.triviatoe.ui.TriviatoePlayScreen
import com.example.gamehub.features.codenames.ui.CodenamesScreen
import com.example.gamehub.ui.GuestGameScreen
import com.example.gamehub.ui.HostLobbyScreen
import com.example.gamehub.ui.LobbyMenuScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.example.gamehub.features.whereandwhe.ui.WhereAndWhenScreen
import com.example.gamehub.features.MemoryMatching.ui.MemoryMatchingScreen
import com.google.firebase.auth.auth
import com.example.gamehub.features.triviatoe.ui.TriviatoeXOAssignScreen
import com.example.gamehub.ui.AccelerometerTestScreen
import com.example.gamehub.ui.CameraTestScreen
import com.example.gamehub.ui.GameInfoScreen
import com.example.gamehub.ui.GyroscopeTestScreen
import com.example.gamehub.ui.MicrophoneTestScreen
import com.example.gamehub.ui.ProximityTestScreen
import com.example.gamehub.ui.SplashScreen
import com.example.gamehub.ui.VibrationTestScreen
import com.example.gamehub.ui.theme.GameHubTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener { Log.d("Auth", "Signed in anonymously") }
            .addOnFailureListener { Log.e("Auth", "Anonymous sign-in failed", it) }

        setContent {
            GameHubTheme {  // <--- YOUR THEME!
                GameHubApp()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    Log.d("MainActivity", "GameHubApp composable created")

    Scaffold { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "splash",
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable("splash") { SplashScreen(navController) }
            composable(NavRoutes.MAIN_MENU)    { MainMenu(navController) }
            composable(NavRoutes.GAMES_LIST)   { GamesListScreen(navController) }
            composable(NavRoutes.SETTINGS)     { SettingsScreen(navController) }
            composable(NavRoutes.TEST_SENSORS) { TestSensorsScreen(navController) }

            composable(NavRoutes.ACCEL_TEST) { AccelerometerTestScreen(navController) }
            composable(NavRoutes.GYRO_TEST) { GyroscopeTestScreen(navController) }
            composable(NavRoutes.PROXIMITY_TEST) { ProximityTestScreen(navController) }
            composable(NavRoutes.VIBRATION_TEST) { VibrationTestScreen(navController) }
            composable(NavRoutes.MIC_TEST) { MicrophoneTestScreen(navController) }
            composable(NavRoutes.CAMERA_TEST) { CameraTestScreen(navController) }

            composable(
                NavRoutes.LOBBY_MENU,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStack ->
                val gameId = backStack.arguments?.getString("gameId") ?: return@composable
                Log.d("MainActivity", "Navigating to LobbyMenuScreen with gameId: $gameId")
                LobbyMenuScreen(navController, gameId)
            }

            composable(
                NavRoutes.HOST_LOBBY,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("code")   { type = NavType.StringType }
                )
            ) { backStack ->
                val gameId = backStack.arguments?.getString("gameId") ?: return@composable
                val code   = backStack.arguments?.getString("code")   ?: return@composable
                Log.d("MainActivity", "Navigating to HostLobbyScreen with gameId: $gameId, code: $code")
                HostLobbyScreen(
                    navController = navController,
                    gameId = gameId,
                    roomId = code,
                    context = context
                )
            }

            composable(
                NavRoutes.GUEST_GAME,
                arguments = listOf(
                    navArgument("gameId")   { type = NavType.StringType },
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val gameId   = backStack.arguments?.getString("gameId")   ?: return@composable
                val code     = backStack.arguments?.getString("code")     ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                GuestGameScreen(
                    navController = navController,
                    gameId = gameId,
                    code = code,
                    userName = userName,
                    context = context
                )
            }

            // Voting screen
            composable(
                NavRoutes.BATTLE_VOTE,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code     = backStack.arguments?.getString("code")     ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                MapVoteScreen(navController, code,  userName)
            }

            // Play screen
            composable(
                NavRoutes.BATTLESHIPS_GAME,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code     = backStack.arguments?.getString("code")     ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                BattleshipsPlayScreen(navController, code, userName)
            }

            // Placement screen via route wrapper
            composable(
                NavRoutes.BATTLE_PLACE,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType },
                    navArgument("mapId")    { type = NavType.IntType }
                )
            ) { backStack ->
                val code     = backStack.arguments!!.getString("code")!!
                val userName = backStack.arguments!!.getString("userName")!!
                val mapId    = backStack.arguments!!.getInt("mapId")
                ShipPlacementRoute(navController, code, userName, mapId)
            }

            composable(
                NavRoutes.OHPARDON_GAME,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code     = backStack.arguments?.getString("code")     ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                OhPardonScreen(navController, code, userName)
            }

            composable(
                NavRoutes.WHERE_AND_WHEN_GAME,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                WhereAndWhenScreen(navController = navController, roomCode = code, currentUserName = userName)
            }

            composable(
                NavRoutes.CODENAMES_GAME,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                    val code = backStack.arguments?.getString("code") ?: return@composable
                    val userName = backStack.arguments?.getString("userName") ?: return@composable
                    val isMaster = userName.contains("master", ignoreCase = true)
                    val masterTeam = if (isMaster) {
                        if (userName.contains("red", ignoreCase = true)) "RED" else "BLUE"
                    } else null
                    CodenamesScreen(
                        navController = navController,
                        roomId = code,
                        isMaster = isMaster,
                        masterTeam = masterTeam
                    )
                }
            composable(
                "${NavRoutes.SPACE_INVADERS_GAME}/{name}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStack ->
                val name = backStack.arguments?.getString("name") ?: return@composable
                SpaceInvadersScreen(navController = navController, name = name)
            }


            composable(NavRoutes.SPY_GAME)       { SpyScreen() }
            composable(NavRoutes.JORISJUMP_GAME) { JorisJumpScreen() }
            composable(NavRoutes.SCREAMOSAUR_GAME) { ScreamosaurScreen() }
            composable(NavRoutes.SPACE_INVADERS_PREGAME) { SpaceInvadersPreGameScreen(navController = navController) }


            composable(
                NavRoutes.TRIVIATOE_XO_ASSIGN,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                val session = remember(code) { FirestoreTriviatoeSession(code) }
                val playerId = Firebase.auth.currentUser?.uid ?: ""
                TriviatoeXOAssignScreen(
                    session = session,
                    playerId = playerId,
                    userName = userName,
                    navController = navController
                )
            }

            composable(
                NavRoutes.TRIVIATOE_GAME,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                val session = remember(code) { FirestoreTriviatoeSession(code) }
                val playerId = Firebase.auth.currentUser?.uid ?: ""
                TriviatoePlayScreen(
                    session = session,
                    playerId = playerId,
                    navController = navController,
                    originalRoomCode = code
                )
            }

            composable(
                NavRoutes.TRIVIATOE_INTRO_ANIM,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                TriviatoeIntroAnimScreen(
                    navController = navController,
                    code = code,
                    userName = userName
                )
            }


            composable(
                NavRoutes.TRIVIATOE_XO_ASSIGN,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                val session = remember(code) { FirestoreTriviatoeSession(code) }
                val playerId = Firebase.auth.currentUser?.uid ?: ""
                TriviatoeXOAssignScreen(
                    session = session,
                    playerId = playerId,
                    userName = userName,
                    navController = navController
                )
            }

            composable(
                NavRoutes.TRIVIATOE_GAME,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                val session = remember(code) { FirestoreTriviatoeSession(code) }
                val playerId = Firebase.auth.currentUser?.uid ?: ""
                TriviatoePlayScreen(
                    session = session,
                    playerId = playerId,
                    navController = navController,
                    originalRoomCode = code
                )
            }

            composable(
                NavRoutes.TRIVIATOE_INTRO_ANIM,
                arguments = listOf(
                    navArgument("code") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code = backStack.arguments?.getString("code") ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                TriviatoeIntroAnimScreen(
                    navController = navController,
                    code = code,
                    userName = userName
                )
            }

            composable(NavRoutes.MEMORY_MATCHING_GAME) { MemoryMatchingScreen() }

            composable(
                NavRoutes.GAME_INFO,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStack ->
                val gameId = backStack.arguments?.getString("gameId") ?: return@composable
                GameInfoScreen(navController, gameId)
            }
        }
    }
}

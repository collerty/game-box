package com.example.gamehub

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.GamesListScreen
import com.example.gamehub.ui.MainMenu
import com.example.gamehub.ui.SettingsScreen
import com.example.gamehub.ui.TestSensorsScreen
import com.example.gamehub.features.battleships.ui.BattleshipsPlayScreen
import com.example.gamehub.features.battleships.ui.MapVoteScreen
import com.example.gamehub.features.battleships.ui.ShipPlacementRoute
import com.example.gamehub.features.ohpardon.ui.OhPardonScreen
import com.example.gamehub.features.spy.ui.SpyScreen
import com.example.gamehub.features.jorisjump.ui.JorisJumpScreen
import com.example.gamehub.features.screamosaur.ui.ScreamosaurScreen
import com.example.gamehub.features.codenames.ui.CodenamesScreen
import com.example.gamehub.ui.GuestGameScreen
import com.example.gamehub.ui.HostLobbyScreen
import com.example.gamehub.ui.LobbyMenuScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener { Log.d("Auth", "Signed in anonymously") }
            .addOnFailureListener { Log.e("Auth", "Anonymous sign-in failed", it) }

        setContent { GameHubApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubApp() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = NavRoutes.MAIN_MENU,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.MAIN_MENU)    { MainMenu(navController) }
            composable(NavRoutes.GAMES_LIST)   { GamesListScreen(navController) }
            composable(NavRoutes.SETTINGS)     { SettingsScreen() }
            composable(NavRoutes.TEST_SENSORS) { TestSensorsScreen(navController) }

            composable(
                NavRoutes.LOBBY_MENU,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { backStack ->
                val gameId = backStack.arguments?.getString("gameId") ?: return@composable
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
                HostLobbyScreen(navController, gameId, code)
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
                GuestGameScreen(navController, gameId, code, userName)
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
                NavRoutes.CODENAMES_GAME,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStack ->
                val code     = backStack.arguments?.getString("code")     ?: return@composable
                val userName = backStack.arguments?.getString("userName") ?: return@composable
                CodenamesScreen(navController, code, userName)
            }

            composable(NavRoutes.SPY_GAME)       { SpyScreen() }
            composable(NavRoutes.JORISJUMP_GAME) { JorisJumpScreen() }
            composable(NavRoutes.SCREAMOSAUR_GAME) { ScreamosaurScreen() }
        }
    }
}

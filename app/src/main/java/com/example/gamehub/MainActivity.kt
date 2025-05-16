package com.example.gamehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.*                                 // LobbyMenuScreen, HostLobbyScreen, GuestGameScreen, MainMenu, GamesListScreen, SettingsScreen, TestSensorsScreen
import com.example.gamehub.features.battleships.ui.BattleshipsScreen
import com.example.gamehub.features.ohpardon.ui.OhPardonScreen
import com.example.gamehub.features.spy.ui.SpyScreen
import com.example.gamehub.features.jorisjump.ui.JorisJumpScreen
import com.example.gamehub.features.screamosaur.ui.ScreamosaurScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameHubApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubApp() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.MAIN_MENU,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1) Main menu / sensor tests
            composable(NavRoutes.MAIN_MENU)    { MainMenu(navController) }
            composable(NavRoutes.GAMES_LIST)   { GamesListScreen(navController) }
            composable(NavRoutes.SETTINGS)     { SettingsScreen() }
            composable(NavRoutes.TEST_SENSORS) { TestSensorsScreen(navController) }

            // 2) Lobby flow
            composable(
                NavRoutes.LOBBY_MENU,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType })
            ) { back ->
                val gameId = back.arguments!!.getString("gameId")!!
                LobbyMenuScreen(navController, gameId)
            }

            composable(
                NavRoutes.HOST_LOBBY,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("code")   { type = NavType.StringType }
                )
            ) { back ->
                val gameId = back.arguments!!.getString("gameId")!!
                val code   = back.arguments!!.getString("code")!!
                HostLobbyScreen(navController, gameId, code)
            }

            composable(
                NavRoutes.GUEST_GAME,
                arguments = listOf(
                    navArgument("gameId")   { type = NavType.StringType },
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { back ->
                val gameId   = back.arguments!!.getString("gameId")!!
                val code     = back.arguments!!.getString("code")!!
                val userName = back.arguments!!.getString("userName")!!
                GuestGameScreen(navController, gameId, code, userName)
            }

            // 3) After “Start Game” you land in the real game screen
            composable(
                NavRoutes.BATTLESHIPS_GAME,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { back ->
                val code     = back.arguments!!.getString("code")!!
                val userName = back.arguments!!.getString("userName")!!
                BattleshipsScreen(navController, code, userName)
            }

            composable(
                NavRoutes.OHPARDON_GAME,
                arguments = listOf(
                    navArgument("code")     { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { back ->
                val code     = back.arguments!!.getString("code")!!
                val userName = back.arguments!!.getString("userName")!!
                OhPardonScreen(navController, code, userName)
            }

            // 4) Local single-player games
            composable(NavRoutes.SPY_GAME)        { SpyScreen() }
            composable(NavRoutes.JORISJUMP_GAME)  { JorisJumpScreen() }
            composable(NavRoutes.SCREAMOSAUR_GAME){ ScreamosaurScreen() }
        }
    }
}

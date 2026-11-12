package com.irv205.pokeapi.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.irv205.pokeapi.BuildConfig
import com.irv205.pokeapi.presentation.navigation.FavoritesRoute
import com.irv205.pokeapi.presentation.navigation.MockConfigRoute
import com.irv205.pokeapi.presentation.navigation.PokeNavHost
import com.irv205.pokeapi.presentation.navigation.PokedexRoute
import com.irv205.pokeapi.presentation.navigation.QuizRoute

@Composable
fun PokeComposeApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var mockConfigVersion by remember { mutableIntStateOf(0) }

    val openMockConfig: (() -> Unit)? = if (BuildConfig.DEBUG) {
        {
            navController.navigate(MockConfigRoute) {
                launchSingleTop = true
            }
        }
    } else {
        null
    }

    Scaffold(
        bottomBar = {
            if (currentDestination.isTopLevelDestination()) {
                PokeBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                )
            }
        },
    ) { innerPadding ->
        PokeNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            refreshToken = mockConfigVersion,
            onOpenMockConfig = openMockConfig,
            onMockConfigApplied = {
                mockConfigVersion++
                navController.popBackStack()
            },
        )
    }
}

private data class BottomDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

private val bottomDestinations = listOf(
    BottomDestination(PokedexRoute, "Pokédex", Icons.Filled.List, "tab_pokedex"),
    BottomDestination(FavoritesRoute, "Mi equipo", Icons.Filled.Favorite, "tab_favorites"),
    BottomDestination(QuizRoute, "Quiz", Icons.Filled.Star, "tab_quiz"),
)

@Composable
private fun PokeBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    NavigationBar {
        bottomDestinations.forEach { destination ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.hasRoute(destination.route::class) } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
                modifier = Modifier.testTag(destination.testTag),
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestination(): Boolean {
    val destination = this ?: return false
    return destination.hasRoute(PokedexRoute::class) ||
        destination.hasRoute(FavoritesRoute::class) ||
        destination.hasRoute(QuizRoute::class)
}

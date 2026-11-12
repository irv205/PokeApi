package com.irv205.pokeapi.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.irv205.pokeapi.BuildConfig
import com.irv205.pokeapi.presentation.detail.DetailScreen
import com.irv205.pokeapi.presentation.evolution.EvolutionScreen
import com.irv205.pokeapi.presentation.favorites.FavoritesScreen
import com.irv205.pokeapi.presentation.mock.MockConfigScreen
import com.irv205.pokeapi.presentation.mock.MockServiceDetailScreen
import com.irv205.pokeapi.presentation.pokedex.PokedexScreen
import com.irv205.pokeapi.presentation.quiz.QuizScreen

@Composable
fun PokeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    refreshToken: Int = 0,
    onOpenMockConfig: (() -> Unit)? = null,
    onMockConfigApplied: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = PokedexRoute,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it / 10 })
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it / 10 })
        },
    ) {
        composable<PokedexRoute> {
            PokedexScreen(
                onPokemonClick = { pokemonId ->
                    navController.navigate(DetailRoute(pokemonId))
                },
                onOpenMockConfig = onOpenMockConfig,
                refreshToken = refreshToken,
            )
        }
        composable<FavoritesRoute> {
            FavoritesScreen(
                onPokemonClick = { pokemonId ->
                    navController.navigate(DetailRoute(pokemonId))
                },
            )
        }
        composable<QuizRoute> {
            QuizScreen()
        }
        composable<DetailRoute> {
            DetailScreen(
                onBack = { navController.popBackStack() },
                onPokemonClick = { pokemonId ->
                    navController.navigate(DetailRoute(pokemonId)) {
                        launchSingleTop = true
                    }
                },
                onOpenEvolution = { pokemonId, chainId ->
                    navController.navigate(EvolutionRoute(pokemonId = pokemonId, chainId = chainId))
                },
            )
        }
        composable<EvolutionRoute> {
            EvolutionScreen(
                onBack = { navController.popBackStack() },
                onPokemonClick = { pokemonId ->
                    navController.navigate(DetailRoute(pokemonId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        if (BuildConfig.DEBUG) {
            composable<MockConfigRoute> {
                MockConfigScreen(
                    onBack = { navController.popBackStack() },
                    onApplied = onMockConfigApplied,
                    onOpenService = { serviceId ->
                        navController.navigate(MockServiceDetailRoute(serviceId))
                    },
                )
            }
            composable<MockServiceDetailRoute> {
                MockServiceDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

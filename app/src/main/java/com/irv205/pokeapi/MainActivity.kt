package com.irv205.pokeapi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.irv205.pokeapi.data.mock.DebugMockOverrides
import com.irv205.pokeapi.data.mock.InMemoryMockConfigProvider
import com.irv205.pokeapi.data.mock.MutableEnvironmentProvider
import com.irv205.pokeapi.presentation.PokeComposeApp
import com.irv205.pokeapi.ui.theme.PokeApiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mockEnvironment: MutableEnvironmentProvider

    @Inject
    lateinit var mockConfig: InMemoryMockConfigProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyMockOverrides(intent)
        enableEdgeToEdge()
        setContent {
            PokeApiTheme {
                PokeComposeApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyMockOverrides(intent)
    }

    private fun applyMockOverrides(intent: Intent?) {
        DebugMockOverrides.applyFromIntent(intent, mockEnvironment, mockConfig)
    }
}

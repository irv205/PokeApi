package com.irv205.pokeapi.presentation.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.irv205.pokeapi.core.common.toDisplayName
import com.irv205.pokeapi.presentation.components.ErrorState
import com.irv205.pokeapi.presentation.components.LoadingState
import com.irv205.pokeapi.presentation.components.PokemonImage

@Composable
fun QuizScreen(
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    QuizContent(
        state = state,
        onSelectOption = viewModel::selectOption,
        onNext = viewModel::advance,
        onPlayAgain = viewModel::playAgain,
        onRetry = viewModel::retry,
    )
}

@Composable
fun QuizContent(
    state: QuizUiState,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
    onPlayAgain: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        QuizHeader()

        AnimatedContent(
            targetState = state.phase,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(250)) togetherWith fadeOut(tween(150))
            },
            label = "quiz_phase",
        ) { phase ->
            when (phase) {
                QuizPhase.Loading -> {
                    if (state.error != null) {
                        ErrorState(message = state.error, onRetry = onRetry)
                    } else {
                        LoadingState(message = "Preparando el quiz...")
                    }
                }

                QuizPhase.Playing -> {
                    val question = state.question
                    if (question == null) {
                        LoadingState(message = "Eligiendo un Pokémon...")
                    } else {
                        QuizPlayingContent(
                            state = state,
                            question = question,
                            onSelectOption = onSelectOption,
                            onNext = onNext,
                        )
                    }
                }

                QuizPhase.Results -> {
                    QuizResultsContent(
                        score = state.score,
                        onPlayAgain = onPlayAgain,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = "Guess that Pokémon",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Adivina la silueta y suma puntos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuizPlayingContent(
    state: QuizUiState,
    question: QuizQuestion,
    onSelectOption: (Int) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip(
                text = "Ronda ${state.round}/$QUIZ_TOTAL_ROUNDS",
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                text = if (state.score == 1) "1 acierto" else "${state.score} aciertos",
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                text = "${state.secondsLeft}s",
                modifier = Modifier.weight(1f),
                highlighted = state.secondsLeft <= 5,
            )
        }

        LinearProgressIndicator(
            progress = { state.secondsLeft.toFloat() / QUIZ_SECONDS_PER_ROUND.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(8.dp)
                .clip(CircleShape),
            color = if (state.secondsLeft <= 5) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(250.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                PokemonImage(
                    imageUrl = question.pokemon.imageUrl,
                    contentDescription = "Silueta de un Pokémon misterioso",
                    modifier = Modifier.size(200.dp),
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    ),
                )
            }
        }

        Text(
            text = "¿Quién es este Pokémon?",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(14.dp))

        question.options.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowOptions.forEach { option ->
                    QuizOptionButton(
                        option = option,
                        correctId = question.correctId,
                        selectedOptionId = state.selectedOptionId,
                        isAnswerRevealed = state.isAnswerRevealed,
                        onClick = { onSelectOption(option.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        AnimatedVisibility(
            visible = state.isAnswerRevealed,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        state.isCorrect == true -> "¡Correcto! 🎉"
                        state.selectedOptionId == null -> "¡Se acabó el tiempo! ⏰"
                        else -> "Casi... era ${question.pokemon.name.toDisplayName()}"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (state.isCorrect == true) {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text(
                        text = if (state.isLastRound) "Ver resultados" else "Siguiente ronda",
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (highlighted) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuizOptionButton(
    option: QuizOption,
    correctId: Int,
    selectedOptionId: Int?,
    isAnswerRevealed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCorrect = option.id == correctId
    val isSelected = option.id == selectedOptionId

    val containerColor = when {
        isAnswerRevealed && isCorrect -> Color(0xFF2E7D32)
        isAnswerRevealed && isSelected -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val contentColor = when {
        isAnswerRevealed && isCorrect -> Color.White
        isAnswerRevealed && isSelected -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        isAnswerRevealed || isSelected -> null
        else -> BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )
    }

    Surface(
        onClick = onClick,
        enabled = !isAnswerRevealed,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuizResultsContent(
    score: Int,
    onPlayAgain: () -> Unit,
) {
    val message = when {
        score == QUIZ_TOTAL_ROUNDS -> "¡Perfecto! Eres todo un Maestro Pokémon."
        score >= 3 -> "¡Muy bien! Tienes buen ojo para las siluetas."
        else -> "Sigue practicando, ¡la Pokédex te espera!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🏆",
            fontSize = 64.sp,
        )
        Text(
            text = "¡Reto completado!",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier
                .padding(top = 20.dp)
                .size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$score / $QUIZ_TOTAL_ROUNDS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
        ) {
            Text("Jugar de nuevo")
        }
    }
}

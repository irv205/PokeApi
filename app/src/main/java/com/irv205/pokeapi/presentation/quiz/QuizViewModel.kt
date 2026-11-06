package com.irv205.pokeapi.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.irv205.pokeapi.core.common.Resource
import com.irv205.pokeapi.core.common.toDisplayName
import com.irv205.pokeapi.domain.model.PokemonSummary
import com.irv205.pokeapi.domain.usecase.GetQuizPoolUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val QUIZ_TOTAL_ROUNDS = 5
const val QUIZ_SECONDS_PER_ROUND = 15

enum class QuizPhase { Loading, Playing, Results }

data class QuizOption(
    val id: Int,
    val name: String,
)

data class QuizQuestion(
    val pokemon: PokemonSummary,
    val options: List<QuizOption>,
    val correctId: Int,
)

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.Loading,
    val question: QuizQuestion? = null,
    val round: Int = 0,
    val score: Int = 0,
    val secondsLeft: Int = QUIZ_SECONDS_PER_ROUND,
    val selectedOptionId: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val isCorrect: Boolean? = null,
    val error: String? = null,
) {
    val isLastRound: Boolean get() = round >= QUIZ_TOTAL_ROUNDS
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val getQuizPool: GetQuizPoolUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val random = Random.Default
    private var pool: List<PokemonSummary> = emptyList()
    private var timerJob: Job? = null

    init {
        loadPool()
    }

    fun loadPool() {
        timerJob?.cancel()
        _uiState.value = QuizUiState(phase = QuizPhase.Loading)
        viewModelScope.launch {
            when (val result = getQuizPool()) {
                is Resource.Success -> {
                    pool = result.data.items.distinctBy { it.id }
                    if (pool.size < OPTIONS_PER_ROUND) {
                        _uiState.update {
                            it.copy(error = "No hay suficientes Pokémon para jugar. Inténtalo más tarde.")
                        }
                    } else {
                        startGame()
                    }
                }

                is Resource.Error -> _uiState.update {
                    it.copy(error = result.message)
                }

                Resource.Loading -> Unit
            }
        }
    }

    fun startGame() {
        timerJob?.cancel()
        _uiState.value = QuizUiState(
            phase = QuizPhase.Playing,
            round = 1,
            score = 0,
            secondsLeft = QUIZ_SECONDS_PER_ROUND,
        )
        nextQuestion()
    }

    private fun nextQuestion() {
        if (pool.size < OPTIONS_PER_ROUND) return

        val correct = pool.random(random)
        val distractors = pool
            .filter { it.id != correct.id }
            .shuffled(random)
            .take(OPTIONS_PER_ROUND - 1)

        val options = (distractors + correct)
            .shuffled(random)
            .map { QuizOption(id = it.id, name = it.name.toDisplayName()) }

        _uiState.update {
            it.copy(
                question = QuizQuestion(
                    pokemon = correct,
                    options = options,
                    correctId = correct.id,
                ),
                selectedOptionId = null,
                isAnswerRevealed = false,
                isCorrect = null,
                secondsLeft = QUIZ_SECONDS_PER_ROUND,
                error = null,
            )
        }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsLeft > 0 && !_uiState.value.isAnswerRevealed) {
                delay(1_000)
                if (_uiState.value.isAnswerRevealed) break

                val remaining = (_uiState.value.secondsLeft - 1).coerceAtLeast(0)
                _uiState.update { it.copy(secondsLeft = remaining) }

                if (remaining == 0) {
                    reveal(correct = false, selectedOptionId = null)
                }
            }
        }
    }

    fun selectOption(optionId: Int) {
        val state = _uiState.value
        if (state.phase != QuizPhase.Playing || state.isAnswerRevealed) return

        reveal(
            correct = optionId == state.question?.correctId,
            selectedOptionId = optionId,
        )
    }

    private fun reveal(correct: Boolean, selectedOptionId: Int?) {
        timerJob?.cancel()
        _uiState.update { state ->
            state.copy(
                selectedOptionId = selectedOptionId,
                isAnswerRevealed = true,
                isCorrect = correct,
                score = if (correct) state.score + 1 else state.score,
            )
        }
    }

    fun advance() {
        val state = _uiState.value
        if (!state.isAnswerRevealed) return

        if (state.round >= QUIZ_TOTAL_ROUNDS) {
            timerJob?.cancel()
            _uiState.update { it.copy(phase = QuizPhase.Results) }
        } else {
            _uiState.update { it.copy(round = it.round + 1) }
            nextQuestion()
        }
    }

    fun playAgain() {
        if (pool.size < OPTIONS_PER_ROUND) {
            loadPool()
        } else {
            startGame()
        }
    }

    fun retry() {
        loadPool()
    }

    override fun onCleared() {
        timerJob?.cancel()
    }

    companion object {
        private const val OPTIONS_PER_ROUND = 4
    }
}

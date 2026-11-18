package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.GameRepository
import com.example.model.ChatMessage
import com.example.model.DiagramAnalysisResult
import com.example.model.DiagramAttempt
import com.example.model.DiagramChallenge
import com.example.model.DiagramChallengesCatalog
import com.example.model.LecturerQuiz
import com.example.model.QuizQuestion
import com.example.model.QuizResultRecord
import com.example.model.StarterQuizCatalog
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QuestionAnswerSummary(
    val question: QuizQuestion,
    val selectedIndex: Int?,
    val isCorrect: Boolean,
    val pointsAwarded: Long
)

data class ActiveQuizUiState(
    val quiz: LecturerQuiz,
    val currentQuestionIndex: Int = 0,
    val remainingMillis: Long = 15_000L,
    val totalMillisPerQuestion: Long = 15_000L,
    val score: Long = 0L,
    val currentCombo: Int = 0,
    val maxCombo: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerRevealed: Boolean = false,
    val lastPointsDelta: Long = 0L,
    val summaries: List<QuestionAnswerSummary> = emptyList()
)

data class QuizOutcomeReport(
    val quiz: LecturerQuiz,
    val won: Boolean,
    val score: Long,
    val correctCount: Int,
    val totalQuestions: Int,
    val accuracyPercent: Int,
    val pointsEarned: Long,
    val maxStreak: Int,
    val summaries: List<QuestionAnswerSummary>,
    val saveError: String? = null
)

class MainViewModel(
    private val gameRepository: GameRepository?,
    private val aiRepository: AiRepository,
    private val currentUserId: String,
    private val currentDisplayName: String
) : ViewModel() {

    val isVisitorMode: Boolean = gameRepository == null

    // Local visitor-session flows (used only when gameRepository == null)
    private val visitorProfile = MutableStateFlow(
        UserProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = currentDisplayName
        )
    )
    private val visitorQuizzes = MutableStateFlow(StarterQuizCatalog.starterQuizzes)
    private val visitorQuizResults = MutableStateFlow<List<QuizResultRecord>>(emptyList())
    private val visitorDiagramAttempts = MutableStateFlow<List<DiagramAttempt>>(emptyList())

    val profileState: StateFlow<UiState<UserProfile?>> = if (gameRepository != null) {
        gameRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load profile"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        visitorProfile
            .map<UserProfile, UiState<UserProfile?>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(visitorProfile.value))
    }

    val quizzesState: StateFlow<UiState<List<LecturerQuiz>>> = if (gameRepository != null) {
        gameRepository.observePublicQuizzes()
            .map<List<LecturerQuiz>, UiState<List<LecturerQuiz>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing quizzes", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load lecturer quizzes"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        visitorQuizzes
            .map<List<LecturerQuiz>, UiState<List<LecturerQuiz>>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(StarterQuizCatalog.starterQuizzes))
    }

    val quizResultsState: StateFlow<UiState<List<QuizResultRecord>>> = if (gameRepository != null) {
        gameRepository.observeQuizResults(currentUserId)
            .map<List<QuizResultRecord>, UiState<List<QuizResultRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing quiz results", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load quiz results"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        visitorQuizResults
            .map<List<QuizResultRecord>, UiState<List<QuizResultRecord>>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    }

    val diagramAttemptsState: StateFlow<UiState<List<DiagramAttempt>>> = if (gameRepository != null) {
        gameRepository.observeDiagramAttempts(currentUserId)
            .map<List<DiagramAttempt>, UiState<List<DiagramAttempt>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing diagram attempts", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load diagram attempts"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        visitorDiagramAttempts
            .map<List<DiagramAttempt>, UiState<List<DiagramAttempt>>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    }

    // --- Diagram Lab State ---
    private val _selectedChallenge = MutableStateFlow(DiagramChallengesCatalog.challenges.first())
    val selectedChallenge: StateFlow<DiagramChallenge> = _selectedChallenge.asStateFlow()

    private val _diagramAnalysisState = MutableStateFlow<UiState<DiagramAnalysisResult>?>(null)
    val diagramAnalysisState: StateFlow<UiState<DiagramAnalysisResult>?> = _diagramAnalysisState.asStateFlow()

    // --- Fast-Paced Quiz Engine State ---
    private val _activeQuiz = MutableStateFlow<ActiveQuizUiState?>(null)
    val activeQuiz: StateFlow<ActiveQuizUiState?> = _activeQuiz.asStateFlow()

    private val _quizOutcome = MutableStateFlow<QuizOutcomeReport?>(null)
    val quizOutcome: StateFlow<QuizOutcomeReport?> = _quizOutcome.asStateFlow()

    private val _publishQuizState = MutableStateFlow<UiState<String>?>(null)
    val publishQuizState: StateFlow<UiState<String>?> = _publishQuizState.asStateFlow()

    private var timerJob: Job? = null

    // --- Gemini AI Tutor Chat State ---
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = "welcome_oracle",
                isFromUser = false,
                text = "Hey there, DBA! I'm Oracle, your Gemini Database Tutor.\n\nAsk me anything about ER diagrams, 1NF–3NF normalization, SQL Joins, ACID transactions, or B+ Tree indexing!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _isTutorResponding = MutableStateFlow(false)
    val isTutorResponding: StateFlow<Boolean> = _isTutorResponding.asStateFlow()

    init {
        if (gameRepository != null) {
            viewModelScope.launch {
                gameRepository.ensureUserProfile(currentUserId, currentDisplayName)
                gameRepository.seedStarterLecturerQuizzesIfNeeded()
            }
        }
    }

    // --- Diagram Lab Actions ---
    fun selectDiagramChallenge(challenge: DiagramChallenge) {
        _selectedChallenge.value = challenge
        _diagramAnalysisState.value = null
    }

    fun dismissDiagramAnalysis() {
        _diagramAnalysisState.value = null
    }

    fun analyzeDrawIoDiagram(diagramBitmap: Bitmap?, mxGraphXml: String?) {
        val challenge = _selectedChallenge.value
        if (diagramBitmap == null && mxGraphXml.isNullOrBlank()) {
            _diagramAnalysisState.value = UiState.Error("Draw on the Draw.io canvas first so AI can inspect your diagram.")
            return
        }
        viewModelScope.launch {
            _diagramAnalysisState.value = UiState.Loading
            try {
                val analysis = aiRepository.analyzeDiagram(challenge, diagramBitmap, mxGraphXml)
                _diagramAnalysisState.value = UiState.Success(analysis)

                if (gameRepository != null) {
                    val persistResult = gameRepository.recordDiagramAttempt(
                        userId = currentUserId,
                        challengeId = challenge.id,
                        challengeTitle = challenge.title,
                        verdictTitle = analysis.verdictTitle,
                        feedbackSummary = analysis.feedbackSummary,
                        score = analysis.score,
                        passed = analysis.passed,
                        xpAwarded = analysis.xpAwarded
                    )
                    if (persistResult.isFailure) {
                        Log.w(TAG, "Failed to persist diagram attempt: ${persistResult.exceptionOrNull()?.message}")
                    }
                } else {
                    val localAttempt = DiagramAttempt(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        challengeId = challenge.id,
                        challengeTitle = challenge.title,
                        verdictTitle = analysis.verdictTitle,
                        feedbackSummary = analysis.feedbackSummary,
                        score = analysis.score.toLong(),
                        passed = analysis.passed,
                        xpAwarded = analysis.xpAwarded,
                        createdAt = Timestamp.now()
                    )
                    visitorDiagramAttempts.update { listOf(localAttempt) + it }
                    visitorProfile.update { p ->
                        p.copy(
                            totalPoints = p.totalPoints + analysis.xpAwarded,
                            diagramsSolved = p.diagramsSolved + if (analysis.passed) 1L else 0L
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Diagram evaluation error", e)
                _diagramAnalysisState.value = UiState.Error(e.localizedMessage ?: "Failed to evaluate diagram.")
            }
        }
    }

    // --- Fast-Paced Quiz Engine Actions ---
    fun startQuiz(quiz: LecturerQuiz) {
        if (quiz.questions.isEmpty()) return
        timerJob?.cancel()
        _quizOutcome.value = null
        val perQMillis = (quiz.timePerQuestionSec.coerceIn(5L, 60L)) * 1000L
        _activeQuiz.value = ActiveQuizUiState(
            quiz = quiz,
            currentQuestionIndex = 0,
            remainingMillis = perQMillis,
            totalMillisPerQuestion = perQMillis
        )
        startQuestionTimer()
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val tickMs = 50L
            while (true) {
                delay(tickMs)
                val current = _activeQuiz.value ?: break
                if (current.isAnswerRevealed) break
                val nextRemaining = (current.remainingMillis - tickMs).coerceAtLeast(0L)
                _activeQuiz.update { it?.copy(remainingMillis = nextRemaining) }
                if (nextRemaining <= 0L) {
                    handleTimeExpired()
                    break
                }
            }
        }
    }

    fun submitQuizAnswer(optionIndex: Int) {
        val current = _activeQuiz.value ?: return
        if (current.isAnswerRevealed) return
        timerJob?.cancel()

        val question = current.quiz.questions[current.currentQuestionIndex]
        val isCorrect = optionIndex.toLong() == question.correctIndex
        val newCombo = if (isCorrect) current.currentCombo + 1 else 0
        val newMaxCombo = maxOf(current.maxCombo, newCombo)

        val speedRatio = (current.remainingMillis.toFloat() / current.totalMillisPerQuestion.toFloat()).coerceIn(0f, 1f)
        val multiplier = when {
            newCombo >= 4 -> 2.5
            newCombo == 3 -> 2.0
            newCombo == 2 -> 1.5
            else -> 1.0
        }
        val pointsDelta = if (isCorrect) {
            ((100.0 + 100.0 * speedRatio) * multiplier).toLong()
        } else 0L

        val summary = QuestionAnswerSummary(
            question = question,
            selectedIndex = optionIndex,
            isCorrect = isCorrect,
            pointsAwarded = pointsDelta
        )

        _activeQuiz.value = current.copy(
            score = current.score + pointsDelta,
            currentCombo = newCombo,
            maxCombo = newMaxCombo,
            selectedOptionIndex = optionIndex,
            isAnswerRevealed = true,
            lastPointsDelta = pointsDelta,
            summaries = current.summaries + summary
        )
    }

    private fun handleTimeExpired() {
        val current = _activeQuiz.value ?: return
        if (current.isAnswerRevealed) return
        val question = current.quiz.questions[current.currentQuestionIndex]
        val summary = QuestionAnswerSummary(
            question = question,
            selectedIndex = null,
            isCorrect = false,
            pointsAwarded = 0L
        )
        _activeQuiz.value = current.copy(
            currentCombo = 0,
            selectedOptionIndex = null,
            isAnswerRevealed = true,
            lastPointsDelta = 0L,
            summaries = current.summaries + summary
        )
    }

    fun advanceQuizQuestion() {
        val current = _activeQuiz.value ?: return
        val nextIndex = current.currentQuestionIndex + 1
        if (nextIndex >= current.quiz.questions.size) {
            finishQuiz(current)
        } else {
            _activeQuiz.value = current.copy(
                currentQuestionIndex = nextIndex,
                remainingMillis = current.totalMillisPerQuestion,
                selectedOptionIndex = null,
                isAnswerRevealed = false,
                lastPointsDelta = 0L
            )
            startQuestionTimer()
        }
    }

    private fun finishQuiz(state: ActiveQuizUiState) {
        timerJob?.cancel()
        val total = state.quiz.questions.size.coerceAtLeast(1)
        val correctCount = state.summaries.count { it.isCorrect }
        val accuracy = ((correctCount * 100f) / total).toInt().coerceIn(0, 100)
        val won = accuracy >= state.quiz.passThresholdPercent
        val xpEarned = if (won) {
            state.quiz.xpReward + (state.maxCombo * 20L)
        } else {
            (correctCount * 15L).coerceAtLeast(15L)
        }

        _activeQuiz.value = null
        val report = QuizOutcomeReport(
            quiz = state.quiz,
            won = won,
            score = state.score,
            correctCount = correctCount,
            totalQuestions = total,
            accuracyPercent = accuracy,
            pointsEarned = xpEarned,
            maxStreak = state.maxCombo,
            summaries = state.summaries
        )
        _quizOutcome.value = report

        if (gameRepository != null) {
            viewModelScope.launch {
                val profile = (profileState.value as? UiState.Success)?.data
                val saveRes = gameRepository.recordQuizResult(
                    userId = currentUserId,
                    quiz = state.quiz,
                    score = state.score,
                    correctCount = correctCount.toLong(),
                    totalQuestions = total.toLong(),
                    accuracyPercent = accuracy.toLong(),
                    won = won,
                    pointsEarned = xpEarned,
                    maxStreak = state.maxCombo.toLong(),
                    currentProfile = profile
                )
                if (saveRes.isFailure) {
                    _quizOutcome.update {
                        it?.copy(saveError = saveRes.exceptionOrNull()?.localizedMessage ?: "Failed to save score")
                    }
                }
            }
        } else {
            val localRecord = QuizResultRecord(
                id = UUID.randomUUID().toString(),
                userId = currentUserId,
                quizId = state.quiz.id,
                quizTitle = state.quiz.title,
                lecturerName = state.quiz.lecturerName,
                score = state.score,
                correctCount = correctCount.toLong(),
                totalQuestions = total.toLong(),
                accuracyPercent = accuracy.toLong(),
                outcome = if (won) "WIN" else "LOSS",
                pointsEarned = xpEarned,
                maxStreak = state.maxCombo.toLong(),
                createdAt = Timestamp.now()
            )
            visitorQuizResults.update { listOf(localRecord) + it }
            visitorProfile.update { p ->
                val newStreak = if (won) p.currentStreak + 1L else 0L
                p.copy(
                    totalPoints = p.totalPoints + xpEarned,
                    quizzesCompleted = p.quizzesCompleted + 1L,
                    quizzesWon = p.quizzesWon + if (won) 1L else 0L,
                    currentStreak = newStreak,
                    bestStreak = maxOf(p.bestStreak, newStreak)
                )
            }
        }
    }

    fun exitActiveQuiz() {
        timerJob?.cancel()
        _activeQuiz.value = null
    }

    fun dismissQuizOutcome() {
        _quizOutcome.value = null
    }

    // --- Lecturer Studio Actions ---
    fun publishLecturerQuiz(
        lecturerName: String,
        lecturerTitle: String,
        title: String,
        topic: String,
        difficulty: String,
        timePerQuestionSec: Long,
        passThresholdPercent: Long,
        xpReward: Long,
        questions: List<QuizQuestion>
    ) {
        if (gameRepository == null) {
            _publishQuizState.value = UiState.Error("Sign in with Google to publish shared lecturer quizzes.")
            return
        }
        viewModelScope.launch {
            _publishQuizState.value = UiState.Loading
            val res = gameRepository.publishLecturerQuiz(
                lecturerName = lecturerName,
                lecturerTitle = lecturerTitle,
                title = title,
                topic = topic,
                difficulty = difficulty,
                timePerQuestionSec = timePerQuestionSec,
                passThresholdPercent = passThresholdPercent,
                xpReward = xpReward,
                questions = questions
            )
            _publishQuizState.value = if (res.isSuccess) {
                UiState.Success(res.getOrThrow())
            } else {
                UiState.Error(res.exceptionOrNull()?.localizedMessage ?: "Failed to publish quiz.")
            }
        }
    }

    fun resetPublishQuizState() {
        _publishQuizState.value = null
    }

    // --- Gemini AI Tutor Actions ---
    fun sendTutorMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isTutorResponding.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            text = trimmed
        )
        val botMsgId = UUID.randomUUID().toString()
        val placeholderBotMsg = ChatMessage(
            id = botMsgId,
            isFromUser = false,
            text = "",
            isStreaming = true
        )

        _chatError.value = null
        _isTutorResponding.value = true
        _chatMessages.update { it + userMsg + placeholderBotMsg }

        viewModelScope.launch {
            try {
                aiRepository.sendTutorMessageStream(trimmed).collect { partial ->
                    _chatMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == botMsgId) msg.copy(text = partial, isStreaming = true) else msg
                        }
                    }
                }
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == botMsgId) msg.copy(isStreaming = false) else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatMessages.update { list -> list.filterNot { it.id == botMsgId && it.text.isBlank() } }
                _chatError.value = e.localizedMessage ?: "Failed to reach Gemini AI Tutor."
            } finally {
                _isTutorResponding.value = false
            }
        }
    }

    fun clearTutorChat() {
        aiRepository.resetTutorChat()
        _chatError.value = null
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome_reset_${System.currentTimeMillis()}",
                isFromUser = false,
                text = "Fresh slate! Ask me anything about database design, SQL, Draw.io ERDs, or normalization."
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}

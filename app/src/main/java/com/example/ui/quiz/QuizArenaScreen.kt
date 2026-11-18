package com.example.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.LecturerQuiz
import com.example.model.QuizQuestion
import com.example.model.UiState
import com.example.ui.ActiveQuizUiState
import com.example.ui.QuizOutcomeReport
import com.example.ui.components.DefeatPulseCanvas
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.StatPill
import com.example.ui.components.VictoryFireworksCanvas
import com.example.ui.components.playOutcomeSpecialFeedback
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.HighSurfaceNavy
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletSecondary
import kotlin.math.roundToInt

@Composable
fun QuizArenaScreen(
    quizzesState: UiState<List<LecturerQuiz>>,
    activeQuiz: ActiveQuizUiState?,
    quizOutcome: QuizOutcomeReport?,
    publishQuizState: UiState<String>?,
    defaultLecturerName: String,
    isVisitorMode: Boolean,
    onSignInRequest: () -> Unit,
    onStartQuiz: (LecturerQuiz) -> Unit,
    onSubmitAnswer: (Int) -> Unit,
    onAdvanceQuestion: () -> Unit,
    onExitActiveQuiz: () -> Unit,
    onDismissOutcome: () -> Unit,
    onPublishQuiz: (
        lecturerName: String,
        lecturerTitle: String,
        title: String,
        topic: String,
        difficulty: String,
        timePerQuestionSec: Long,
        passThresholdPercent: Long,
        xpReward: Long,
        questions: List<QuizQuestion>
    ) -> Unit,
    onResetPublishState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showStudio by remember { mutableStateOf(false) }

    when {
        quizOutcome != null -> {
            BackHandler { onDismissOutcome() }
            QuizOutcomeView(
                report = quizOutcome,
                onReplay = { onStartQuiz(quizOutcome.quiz) },
                onBackToLobby = onDismissOutcome,
                modifier = modifier
            )
        }

        activeQuiz != null -> {
            BackHandler { onExitActiveQuiz() }
            ActiveQuizPlayView(
                state = activeQuiz,
                onSelectOption = onSubmitAnswer,
                onNextQuestion = onAdvanceQuestion,
                onExit = onExitActiveQuiz,
                modifier = modifier
            )
        }

        showStudio -> {
            BackHandler {
                onResetPublishState()
                showStudio = false
            }
            if (isVisitorMode) {
                VisitorStudioSignInGate(
                    onSignInRequest = onSignInRequest,
                    onBack = { showStudio = false },
                    modifier = modifier
                )
            } else {
                LecturerStudioView(
                    defaultLecturerName = defaultLecturerName,
                    publishState = publishQuizState,
                    onPublish = onPublishQuiz,
                    onBack = {
                        onResetPublishState()
                        showStudio = false
                    },
                    modifier = modifier
                )
            }
        }

        else -> {
            QuizLobbyView(
                quizzesState = quizzesState,
                onStartQuiz = onStartQuiz,
                onOpenStudio = {
                    onResetPublishState()
                    showStudio = true
                },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun VisitorStudioSignInGate(
    onSignInRequest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ElevatedNavyCard)
                .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = VioletSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Lecturer Studio Requires Sign-In",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Visitors can play every lecturer quiz freely! Sign in with Google to publish new quizzes to the shared arena.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onSignInRequest,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF041C24)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("studio_sign_in_button")
            ) {
                Text(
                    text = stringResource(R.string.sign_in_google),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Back to Quizzes")
            }
        }
    }
}

@Composable
private fun QuizLobbyView(
    quizzesState: UiState<List<LecturerQuiz>>,
    onStartQuiz: (LecturerQuiz) -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTopic by remember { mutableStateOf("All") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FAST-PACED QUIZ ARENA",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberGold
                    )
                    Text(
                        text = "Lecturer Challenges",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Button(
                    onClick = onOpenStudio,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletSecondary,
                        contentColor = Color(0xFF160E2E)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("create_quiz_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.create_lecturer_quiz),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (quizzesState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyanPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Loading lecturer quizzes…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                is UiState.Error -> {
                    Surface(
                        color = RoseLoss.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = quizzesState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = RoseLoss,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    val quizzes = quizzesState.data
                    val topics = remember(quizzes) {
                        listOf("All") + quizzes.map { it.topic }.distinct()
                    }
                    val filtered = remember(quizzes, selectedTopic) {
                        if (selectedTopic == "All") quizzes else quizzes.filter { it.topic == selectedTopic }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(topics) { topic ->
                            FilterChip(
                                selected = selectedTopic == topic,
                                onClick = { selectedTopic = topic },
                                label = { Text(topic) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No quizzes yet for this topic. Create one in Lecturer Studio!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filtered, key = { it.id }) { quiz ->
                                LecturerQuizCard(
                                    quiz = quiz,
                                    onPlay = { onStartQuiz(quiz) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerQuizCard(
    quiz: LecturerQuiz,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ElevatedNavyCard)
            .border(1.dp, BorderSlate, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.School,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = quiz.lecturerName,
                        style = MaterialTheme.typography.labelLarge,
                        color = CyanPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = quiz.lecturerTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            DifficultyBadge(difficulty = quiz.difficulty)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = quiz.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(
                icon = Icons.Rounded.Bolt,
                label = "${quiz.questions.size} Qs",
                tint = VioletSecondary
            )
            StatPill(
                icon = Icons.Rounded.Timer,
                label = "${quiz.timePerQuestionSec}s / Q",
                tint = AmberGold
            )
            StatPill(
                icon = Icons.Rounded.EmojiEvents,
                label = "+${quiz.xpReward} XP",
                tint = EmeraldWin
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = Color(0xFF041C24)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("play_quiz_${quiz.id}")
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Start Blitz (${quiz.passThresholdPercent}% to Win)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActiveQuizPlayView(
    state: ActiveQuizUiState,
    onSelectOption: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = state.quiz.questions[state.currentQuestionIndex]
    val progress = (state.remainingMillis.toFloat() / state.totalMillisPerQuestion.toFloat()).coerceIn(0f, 1f)
    val secondsLeft = ((state.remainingMillis + 999L) / 1000L).toInt()
    val timerColor = when {
        secondsLeft <= 3 -> RoseLoss
        secondsLeft <= 6 -> AmberGold
        else -> CyanPrimary
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElevatedNavyCard)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Exit quiz")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Q ${state.currentQuestionIndex + 1} / ${state.quiz.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "by ${state.quiz.lecturerName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.currentCombo >= 2) {
                        StatPill(
                            icon = Icons.Rounded.LocalFireDepartment,
                            label = "${state.currentCombo}x",
                            tint = AmberGold
                        )
                    }
                    StatPill(
                        icon = Icons.Rounded.EmojiEvents,
                        label = "${state.score}",
                        tint = CyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fast-Paced Timer Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIME REMAINING",
                    style = MaterialTheme.typography.labelSmall,
                    color = timerColor
                )
                Text(
                    text = "${secondsLeft}s",
                    style = MaterialTheme.typography.titleLarge,
                    color = timerColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = timerColor,
                trackColor = ElevatedNavyCard,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Question Card
            Surface(
                color = ElevatedNavyCard,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(22.dp))
            ) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Answer Options
            question.options.forEachIndexed { idx, optionText ->
                val isSelected = state.selectedOptionIndex == idx
                val isCorrectOption = idx.toLong() == question.correctIndex

                val (bg, borderColor, textColor) = when {
                    !state.isAnswerRevealed -> Triple(ElevatedNavyCard, BorderSlate, Color.White)
                    isCorrectOption -> Triple(EmeraldWin.copy(alpha = 0.22f), EmeraldWin, EmeraldWin)
                    isSelected -> Triple(RoseLoss.copy(alpha = 0.22f), RoseLoss, RoseLoss)
                    else -> Triple(ElevatedNavyCard.copy(alpha = 0.6f), BorderSlate.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable(enabled = !state.isAnswerRevealed) { onSelectOption(idx) }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag("quiz_option_$idx"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + idx).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.isAnswerRevealed && isCorrectOption) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Correct", tint = EmeraldWin)
                    } else if (state.isAnswerRevealed && isSelected) {
                        Icon(Icons.Rounded.Close, contentDescription = "Incorrect", tint = RoseLoss)
                    }
                }
            }

            // Explanation + Next CTA when answered / expired
            AnimatedVisibility(visible = state.isAnswerRevealed) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    val wasCorrect = state.selectedOptionIndex?.toLong() == question.correctIndex
                    val statusTint = if (wasCorrect) EmeraldWin else RoseLoss
                    Surface(
                        color = statusTint.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, statusTint.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = when {
                                    wasCorrect -> "Spot On! +${state.lastPointsDelta} pts"
                                    state.selectedOptionIndex == null -> "Time's Up! 0 pts"
                                    else -> "Not Quite! 0 pts"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = statusTint
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val isLast = state.currentQuestionIndex >= state.quiz.questions.lastIndex
                    Button(
                        onClick = onNextQuestion,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF041C24)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quiz_next_button")
                    ) {
                        Text(
                            text = if (isLast) "See Final Result" else "Next Question",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuizOutcomeView(
    report: QuizOutcomeReport,
    onReplay: () -> Unit,
    onBackToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val badgeScale = remember { Animatable(0.5f) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(report.quiz.id, report.won, report.score) {
        playOutcomeSpecialFeedback(context, report.won)
        if (report.won) {
            badgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            badgeScale.animateTo(1f, tween(300))
            // Dramatic screen shake effect on loss
            val shakes = listOf(-24f, 22f, -18f, 14f, -8f, 4f, 0f)
            shakes.forEach { target ->
                shakeOffset.animateTo(target, tween(55))
            }
        }
    }

    val outcomeColor = if (report.won) EmeraldWin else RoseLoss

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg)
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        contentAlignment = Alignment.TopCenter
    ) {
        // Full-Screen Winning or Loss Special Effect Canvas!
        if (report.won) {
            VictoryFireworksCanvas(modifier = Modifier.fillMaxSize())
        } else {
            DefeatPulseCanvas(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .scale(badgeScale.value)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(outcomeColor.copy(alpha = 0.2f))
                    .border(2.5.dp, outcomeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (report.won) Icons.Rounded.EmojiEvents else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = outcomeColor,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (report.won) "VICTORY! QUERY COMMITTED" else "ROLLBACK! TRY AGAIN",
                style = MaterialTheme.typography.labelSmall,
                color = outcomeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (report.won) "You Crushed It!" else "Below ${report.quiz.passThresholdPercent}% Threshold",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Text(
                text = "${report.quiz.title} • ${report.quiz.lecturerName}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultMetricCard("Accuracy", "${report.accuracyPercent}%", outcomeColor, Modifier.weight(1f))
                ResultMetricCard("Blitz Score", "${report.score}", CyanPrimary, Modifier.weight(1f))
                ResultMetricCard("XP Earned", "+${report.pointsEarned}", AmberGold, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultMetricCard(
                    "Correct",
                    "${report.correctCount}/${report.totalQuestions}",
                    EmeraldWin,
                    Modifier.weight(1f)
                )
                ResultMetricCard(
                    "Max Streak",
                    "${report.maxStreak}x",
                    VioletSecondary,
                    Modifier.weight(1f)
                )
            }

            if (report.saveError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Note: Cloud sync issue (${report.saveError})",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberGold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReplay,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("replay_quiz_button")
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Again")
                }

                Button(
                    onClick = onBackToLobby,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF041C24)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("back_to_lobby_button")
                ) {
                    Text(
                        text = "Quiz Arena",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Question Breakdown
            Text(
                text = "Question Breakdown",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            report.summaries.forEachIndexed { idx, item ->
                val tint = if (item.isCorrect) EmeraldWin else RoseLoss
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedNavyCard)
                        .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Q${idx + 1}: ${item.question.prompt}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val correctOpt = item.question.options.getOrNull(item.question.correctIndex.toInt()).orEmpty()
                    Text(
                        text = "Answer: $correctOpt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldWin
                    )
                    Text(
                        text = item.question.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ResultMetricCard(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedNavyCard)
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = tint)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class DraftQuestion(
    var prompt: String = "",
    var optA: String = "",
    var optB: String = "",
    var optC: String = "",
    var optD: String = "",
    var correctIndex: Int = 0,
    var explanation: String = ""
)

@Composable
private fun LecturerStudioView(
    defaultLecturerName: String,
    publishState: UiState<String>?,
    onPublish: (
        lecturerName: String,
        lecturerTitle: String,
        title: String,
        topic: String,
        difficulty: String,
        timePerQuestionSec: Long,
        passThresholdPercent: Long,
        xpReward: Long,
        questions: List<QuizQuestion>
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lecturerName by remember { mutableStateOf(defaultLecturerName.ifBlank { "Madam Haida" }) }
    var lecturerTitle by remember { mutableStateOf("Database Systems Lecturer") }
    var quizTitle by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("SQL Queries") }
    var difficulty by remember { mutableStateOf("Intermediate") }
    var secondsPerQ by remember { mutableStateOf("15") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val draftQuestions = remember {
        mutableStateListOf(
            DraftQuestion(
                prompt = "Which SQL constraint uniquely identifies each row in a table?",
                optA = "FOREIGN KEY",
                optB = "PRIMARY KEY",
                optC = "DEFAULT",
                optD = "INDEX",
                correctIndex = 1,
                explanation = "A PRIMARY KEY enforces uniqueness and NOT NULL on the row identifier."
            )
        )
    }

    LaunchedEffect(publishState) {
        if (publishState is UiState.Success) {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "LECTURER QUIZ STUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = VioletSecondary
                    )
                    Text(
                        text = "Author a Fast-Paced Quiz",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = lecturerName,
                onValueChange = { lecturerName = it },
                label = { Text("Lecturer Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = lecturerTitle,
                onValueChange = { lecturerTitle = it },
                label = { Text("Department / Academic Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = quizTitle,
                onValueChange = { quizTitle = it },
                label = { Text("Quiz Title (e.g., PostgreSQL Window Functions)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("studio_quiz_title_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = secondsPerQ,
                    onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 2) secondsPerQ = it },
                    label = { Text("Sec / Q (5-60)") },
                    singleLine = true,
                    modifier = Modifier.width(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Beginner", "Intermediate", "Advanced").forEach { tier ->
                    FilterChip(
                        selected = difficulty == tier,
                        onClick = { difficulty = tier },
                        label = { Text(tier) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Questions (${draftQuestions.size}/10)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                OutlinedButton(
                    onClick = {
                        if (draftQuestions.size < 10) {
                            draftQuestions.add(DraftQuestion())
                        }
                    },
                    enabled = draftQuestions.size < 10
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Q")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            draftQuestions.forEachIndexed { idx, q ->
                var prompt by remember(q) { mutableStateOf(q.prompt) }
                var optA by remember(q) { mutableStateOf(q.optA) }
                var optB by remember(q) { mutableStateOf(q.optB) }
                var optC by remember(q) { mutableStateOf(q.optC) }
                var optD by remember(q) { mutableStateOf(q.optD) }
                var correctIdx by remember(q) { mutableIntStateOf(q.correctIndex) }
                var explanation by remember(q) { mutableStateOf(q.explanation) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElevatedNavyCard)
                        .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Question ${idx + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyanPrimary
                        )
                        if (draftQuestions.size > 1) {
                            IconButton(onClick = { draftQuestions.removeAt(idx) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove question", tint = RoseLoss)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it; q.prompt = it },
                        label = { Text("Question Prompt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val opts = listOf(
                        "A" to optA,
                        "B" to optB,
                        "C" to optC,
                        "D" to optD
                    )
                    opts.forEachIndexed { optIdx, (letter, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            RadioButton(
                                selected = correctIdx == optIdx,
                                onClick = { correctIdx = optIdx; q.correctIndex = optIdx }
                            )
                            OutlinedTextField(
                                value = value,
                                onValueChange = { newVal ->
                                    when (optIdx) {
                                        0 -> { optA = newVal; q.optA = newVal }
                                        1 -> { optB = newVal; q.optB = newVal }
                                        2 -> { optC = newVal; q.optC = newVal }
                                        else -> { optD = newVal; q.optD = newVal }
                                    }
                                },
                                label = { Text("Option $letter" + if (correctIdx == optIdx) " (Correct)" else "") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it; q.explanation = it },
                        label = { Text("Explanation (shown after answer)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (validationError != null || publishState is UiState.Error) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError ?: (publishState as? UiState.Error)?.message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoseLoss
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val sec = secondsPerQ.toLongOrNull() ?: 15L
                    val titleTrim = quizTitle.trim()
                    if (titleTrim.length < 3) {
                        validationError = "Please enter a quiz title (at least 3 characters)."
                        return@Button
                    }
                    if (sec !in 5L..60L) {
                        validationError = "Time per question must be between 5 and 60 seconds."
                        return@Button
                    }
                    val builtQuestions = draftQuestions.map {
                        QuizQuestion(
                            prompt = it.prompt.trim(),
                            options = listOf(it.optA.trim(), it.optB.trim(), it.optC.trim(), it.optD.trim()),
                            correctIndex = it.correctIndex.toLong(),
                            explanation = it.explanation.trim()
                        )
                    }
                    if (builtQuestions.any { it.prompt.isBlank() || it.options.any { opt -> opt.isBlank() } }) {
                        validationError = "Every question needs a prompt and 4 non-empty options."
                        return@Button
                    }
                    validationError = null
                    val xp = when (difficulty) {
                        "Beginner" -> 180L
                        "Advanced" -> 320L
                        else -> 240L
                    }
                    onPublish(
                        lecturerName,
                        lecturerTitle,
                        titleTrim,
                        topic,
                        difficulty,
                        sec,
                        60L,
                        xp,
                        builtQuestions
                    )
                },
                enabled = publishState !is UiState.Loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF041C24)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_quiz_button")
            ) {
                if (publishState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF041C24))
                } else {
                    Text(
                        text = stringResource(R.string.publish_quiz),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

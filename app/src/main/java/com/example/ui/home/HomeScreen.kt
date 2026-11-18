package com.example.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppTab
import com.example.model.DiagramAttempt
import com.example.model.PlayerRankHelper
import com.example.model.QuizResultRecord
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.StatPill
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.HighSurfaceNavy
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletSecondary

@Composable
fun HomeScreen(
    profileState: UiState<UserProfile?>,
    quizResultsState: UiState<List<QuizResultRecord>>,
    diagramAttemptsState: UiState<List<DiagramAttempt>>,
    isVisitorMode: Boolean,
    onNavigateTab: (AppTab) -> Unit,
    onSignInRequest: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val totalPoints = profile?.totalPoints ?: 0L
    val rank = PlayerRankHelper.getRank(totalPoints)
    val quizResults = (quizResultsState as? UiState.Success)?.data.orEmpty()
    val diagramAttempts = (diagramAttemptsState as? UiState.Success)?.data.orEmpty()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Greeting & Sign In / Sign Out
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVisitorMode) "DATA QUESTZ • VISITOR SESSION" else "DATA QUESTZ ARENA",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isVisitorMode) AmberGold else CyanPrimary
                        )
                        Text(
                            text = "Welcome, ${profile?.displayName ?: "Explorer"}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isVisitorMode) {
                        Button(
                            onClick = onSignInRequest,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color(0xFF041C24)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("visitor_sign_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Login,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.sign_in_to_save),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ElevatedNavyCard)
                                .border(1.dp, BorderSlate, CircleShape)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Sign out",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isVisitorMode) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AmberGold.copy(alpha = 0.12f))
                            .border(1.dp, AmberGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Playing as Visitor — session XP is temporary until you sign in.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFDE68A),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onSignInRequest) {
                            Text("Sign In", color = CyanPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hero Banner + XP Rank Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(ElevatedNavyCard)
                        .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_quest_1794908358138),
                                contentDescription = "Database quest arena banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, ElevatedNavyCard)
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatPill(
                                    icon = Icons.Rounded.LocalFireDepartment,
                                    label = "${profile?.currentStreak ?: 0} Streak",
                                    tint = AmberGold
                                )
                                StatPill(
                                    icon = Icons.Rounded.EmojiEvents,
                                    label = "$totalPoints XP",
                                    tint = CyanPrimary
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lvl ${rank.level} • ${rank.title}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "$totalPoints / ${rank.nextLevelXp} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { rank.progress.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = CyanPrimary,
                                trackColor = MidnightBg,
                                strokeCap = StrokeCap.Round
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MiniMetricBox(
                                    label = "Quizzes Won",
                                    value = "${profile?.quizzesWon ?: 0}/${profile?.quizzesCompleted ?: 0}",
                                    tint = EmeraldWin,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniMetricBox(
                                    label = "ERDs Passed",
                                    value = "${profile?.diagramsSolved ?: 0}",
                                    tint = CyanPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniMetricBox(
                                    label = "Best Streak",
                                    value = "${profile?.bestStreak ?: 0}",
                                    tint = AmberGold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (profileState is UiState.Loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing cloud stats…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (profileState is UiState.Error) {
                item {
                    Surface(
                        color = RoseLoss.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = profileState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RoseLoss,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Game Modes Section
            item {
                Text(
                    text = "Choose Your Quest",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                QuestModeCard(
                    title = "Draw.io Diagram Lab",
                    subtitle = "Design ER diagrams directly in Draw.io. Gemini AI inspects your schema & awards XP.",
                    badge = "AI GRADING",
                    icon = Icons.Rounded.AccountTree,
                    accentColor = CyanPrimary,
                    testTag = "launch_diagram_lab_card",
                    onClick = { onNavigateTab(AppTab.DIAGRAM_LAB) }
                )
            }

            item {
                QuestModeCard(
                    title = "Fast-Paced Lecturer Quizzes",
                    subtitle = "Beat the timer on challenges authored by lecturers, or publish your own quiz!",
                    badge = "WIN / LOSS FX",
                    icon = Icons.Rounded.Bolt,
                    accentColor = AmberGold,
                    testTag = "launch_speed_quiz_card",
                    onClick = { onNavigateTab(AppTab.SPEED_QUIZ) }
                )
            }

            item {
                QuestModeCard(
                    title = "Oracle Gemini AI Tutor",
                    subtitle = "Chat live with Gemini about SQL joins, 3NF normalization, keys & transactions.",
                    badge = "24/7 TUTOR",
                    icon = Icons.Rounded.AutoAwesome,
                    accentColor = VioletSecondary,
                    testTag = "launch_ai_tutor_card",
                    onClick = { onNavigateTab(AppTab.AI_TUTOR) }
                )
            }

            // Recent Activity
            if (quizResults.isNotEmpty() || diagramAttempts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent Quest Logs",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(quizResults.take(3), key = { "qr_${it.id}" }) { result ->
                    val won = result.outcome == "WIN"
                    ActivityLogRow(
                        title = result.quizTitle,
                        subtitle = "by ${result.lecturerName} • ${result.accuracyPercent}% accuracy",
                        pointsLabel = "+${result.pointsEarned} XP",
                        isPositive = won,
                        icon = if (won) Icons.Rounded.CheckCircle else Icons.Rounded.Close
                    )
                }

                items(diagramAttempts.take(3), key = { "da_${it.id}" }) { attempt ->
                    ActivityLogRow(
                        title = attempt.challengeTitle,
                        subtitle = "${attempt.verdictTitle} • Score ${attempt.score}/100",
                        pointsLabel = "+${attempt.xpAwarded} XP",
                        isPositive = attempt.passed,
                        icon = Icons.Rounded.School
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MiniMetricBox(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(HighSurfaceNavy)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun QuestModeCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ElevatedNavyCard)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "Open $title",
            tint = accentColor
        )
    }
}

@Composable
private fun ActivityLogRow(
    title: String,
    subtitle: String,
    pointsLabel: String,
    isPositive: Boolean,
    icon: ImageVector
) {
    val tint = if (isPositive) EmeraldWin else RoseLoss
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ElevatedNavyCard)
            .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = pointsLabel,
            style = MaterialTheme.typography.labelLarge,
            color = if (isPositive) EmeraldWin else AmberGold
        )
    }
}

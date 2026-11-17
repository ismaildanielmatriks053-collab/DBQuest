package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutUser
import com.example.data.AiRepository
import com.example.data.GameRepository
import com.example.firebase.configureAppCheck
import com.example.model.AppTab
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.diagram.DrawIoLabScreen
import com.example.ui.home.HomeScreen
import com.example.ui.quiz.QuizArenaScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DBQuestTheme
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.TextSecondary
import com.example.ui.tutor.AiTutorScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val VISITOR_USER_ID = "visitor_guest"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure App Check with launch intent before rendering UI or creating ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            DBQuestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MidnightBg
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isVisitorMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { isVisitorMode = false },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    when {
        user != null -> {
            // Key composition and ViewModels by uid so account switches dispose old Firestore listeners cleanly.
            key(user.uid) {
                DbQuestApp(
                    currentUserId = user.uid,
                    currentDisplayName = user.displayName ?: "DB Explorer",
                    isVisitorMode = false,
                    onSignInRequest = {},
                    onSignOut = {
                        isVisitorMode = false
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }

        isVisitorMode -> {
            key(VISITOR_USER_ID) {
                DbQuestApp(
                    currentUserId = VISITOR_USER_ID,
                    currentDisplayName = "Visitor Player",
                    isVisitorMode = true,
                    onSignInRequest = {
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = { isVisitorMode = false },
                            onAuthError = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            scope = scope
                        )
                    },
                    onSignOut = { isVisitorMode = false }
                )
            }
        }

        else -> {
            AuthScreen(
                onAuthSuccess = { isVisitorMode = false },
                onContinueAsVisitor = { isVisitorMode = true }
            )
        }
    }
}

private data class NavItem(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navItems = listOf(
    NavItem(AppTab.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    NavItem(AppTab.DIAGRAM_LAB, R.string.nav_diagram, Icons.Rounded.AccountTree, Icons.Outlined.AccountTree, "nav_diagram"),
    NavItem(AppTab.SPEED_QUIZ, R.string.nav_quiz, Icons.Rounded.Bolt, Icons.Outlined.Bolt, "nav_quiz"),
    NavItem(AppTab.AI_TUTOR, R.string.nav_tutor, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_tutor")
)

@Composable
private fun DbQuestApp(
    currentUserId: String,
    currentDisplayName: String,
    isVisitorMode: Boolean,
    onSignInRequest: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: MainViewModel = viewModel(
        key = "${currentUserId}_$isVisitorMode",
        factory = viewModelFactory {
            initializer {
                val repo = if (isVisitorMode) {
                    null
                } else {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    GameRepository(db)
                }
                MainViewModel(
                    gameRepository = repo,
                    aiRepository = AiRepository(),
                    currentUserId = currentUserId,
                    currentDisplayName = currentDisplayName
                )
            }
        }
    )

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val quizzesState by viewModel.quizzesState.collectAsStateWithLifecycle()
    val quizResultsState by viewModel.quizResultsState.collectAsStateWithLifecycle()
    val diagramAttemptsState by viewModel.diagramAttemptsState.collectAsStateWithLifecycle()

    val selectedChallenge by viewModel.selectedChallenge.collectAsStateWithLifecycle()
    val diagramAnalysisState by viewModel.diagramAnalysisState.collectAsStateWithLifecycle()

    val activeQuiz by viewModel.activeQuiz.collectAsStateWithLifecycle()
    val quizOutcome by viewModel.quizOutcome.collectAsStateWithLifecycle()
    val publishQuizState by viewModel.publishQuizState.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isTutorResponding by viewModel.isTutorResponding.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = MidnightBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail && activeQuiz == null && quizOutcome == null) {
                    NavigationBar(
                        containerColor = DeepNavySurface,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = selectedTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = CyanPrimary.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && activeQuiz == null && quizOutcome == null) {
                    NavigationRail(containerColor = DeepNavySurface) {
                        navItems.forEach { item ->
                            val selected = selectedTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = item.tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanPrimary,
                                    selectedTextColor = CyanPrimary,
                                    indicatorColor = ElevatedNavyCard,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    AppTab.HOME -> {
                        HomeScreen(
                            profileState = profileState,
                            quizResultsState = quizResultsState,
                            diagramAttemptsState = diagramAttemptsState,
                            isVisitorMode = isVisitorMode,
                            onNavigateTab = { selectedTab = it },
                            onSignInRequest = onSignInRequest,
                            onSignOut = onSignOut
                        )
                    }

                    AppTab.DIAGRAM_LAB -> {
                        DrawIoLabScreen(
                            selectedChallenge = selectedChallenge,
                            analysisState = diagramAnalysisState,
                            onSelectChallenge = viewModel::selectDiagramChallenge,
                            onAnalyzeDiagram = viewModel::analyzeDrawIoDiagram,
                            onDismissAnalysis = viewModel::dismissDiagramAnalysis
                        )
                    }

                    AppTab.SPEED_QUIZ -> {
                        QuizArenaScreen(
                            quizzesState = quizzesState,
                            activeQuiz = activeQuiz,
                            quizOutcome = quizOutcome,
                            publishQuizState = publishQuizState,
                            defaultLecturerName = currentDisplayName,
                            isVisitorMode = isVisitorMode,
                            onSignInRequest = onSignInRequest,
                            onStartQuiz = viewModel::startQuiz,
                            onSubmitAnswer = viewModel::submitQuizAnswer,
                            onAdvanceQuestion = viewModel::advanceQuizQuestion,
                            onExitActiveQuiz = viewModel::exitActiveQuiz,
                            onDismissOutcome = viewModel::dismissQuizOutcome,
                            onPublishQuiz = viewModel::publishLecturerQuiz,
                            onResetPublishState = viewModel::resetPublishQuizState
                        )
                    }

                    AppTab.AI_TUTOR -> {
                        AiTutorScreen(
                            messages = chatMessages,
                            isResponding = isTutorResponding,
                            chatError = chatError,
                            onSendMessage = viewModel::sendTutorMessage,
                            onResetChat = viewModel::clearTutorChat
                        )
                    }
                }
            }
        }
    }
}

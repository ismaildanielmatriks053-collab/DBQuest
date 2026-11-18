package com.example.ui.diagram

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.model.DiagramAnalysisResult
import com.example.model.DiagramChallenge
import com.example.model.DiagramChallengesCatalog
import com.example.model.UiState
import com.example.ui.components.DefeatPulseCanvas
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.VictoryFireworksCanvas
import com.example.ui.components.playOutcomeSpecialFeedback
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.RoseLoss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val DRAWIO_BASE_URL = "https://appassets.androidplatform.net/"
private const val DRAWIO_IFRAME_URL =
    "https://embed.diagrams.net/?embed=1&ui=dark&spin=1&proto=json&configure=1&libraries=1&noSaveBtn=1&noExitBtn=1&stealth=1&gapi=0&db=0&od=0&gh=0&gl=0&tr=0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawIoLabScreen(
    selectedChallenge: DiagramChallenge,
    analysisState: UiState<DiagramAnalysisResult>?,
    onSelectChallenge: (DiagramChallenge) -> Unit,
    onAnalyzeDiagram: (Bitmap?, String?) -> Unit,
    onDismissAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBrief by remember { mutableStateOf(false) }
    var useOfflineFallback by remember { mutableStateOf(false) }
    var webViewInstanceKey by remember { mutableIntStateOf(0) }
    var crashRetryCount by remember { mutableIntStateOf(0) }
    var isEditorReady by remember { mutableStateOf(false) }
    var webError by remember { mutableStateOf<String?>(null) }
    var latestXml by remember(selectedChallenge.id) { mutableStateOf(selectedChallenge.starterXml) }
    var isExporting by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Load current challenge XML into Draw.io whenever the challenge changes and Draw.io is ready
    LaunchedEffect(selectedChallenge.id, isEditorReady, useOfflineFallback) {
        if (!useOfflineFallback && isEditorReady) {
            sendDrawIoLoad(webViewRef, latestXml)
        }
    }

    // Play haptic/audio special effect when AI analysis completes
    LaunchedEffect(analysisState) {
        if (analysisState is UiState.Success) {
            playOutcomeSpecialFeedback(context, analysisState.data.passed)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Header + Difficulty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DRAW.IO DIAGRAM LAB",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanPrimary
                )
                Text(
                    text = selectedChallenge.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DifficultyBadge(difficulty = selectedChallenge.difficulty)
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(DiagramChallengesCatalog.challenges, key = { it.id }) { challenge ->
                val isSelected = challenge.id == selectedChallenge.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyanPrimary.copy(alpha = 0.2f) else ElevatedNavyCard)
                        .border(
                            1.dp,
                            if (isSelected) CyanPrimary else BorderSlate,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectChallenge(challenge) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("challenge_chip_${challenge.id}")
                ) {
                    Text(
                        text = challenge.title.substringBefore(" "),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Collapsible Requirements Brief
        Surface(
            color = ElevatedNavyCard,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSlate, RoundedCornerShape(14.dp))
                .clickable { showBrief = !showBrief }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mission Brief (+${selectedChallenge.xpReward} XP)",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberGold
                    )
                    Icon(
                        imageVector = if (showBrief) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (showBrief) "Collapse brief" else "Expand brief",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = showBrief) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(
                            text = selectedChallenge.scenario,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        selectedChallenge.requirements.forEach { req ->
                            Text(
                                text = "• $req",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Embedded Original Draw.io System (with crash recovery & offline fallback only on error)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DeepNavySurface)
                .border(1.dp, BorderSlate, RoundedCornerShape(16.dp))
        ) {
            if (useOfflineFallback) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ElevatedNavyCard)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offline ER Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberGold
                        )
                        OutlinedButton(
                            onClick = {
                                useOfflineFallback = false
                                webError = null
                                isEditorReady = false
                                crashRetryCount = 0
                                webViewInstanceKey++
                            },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text("Reconnect Draw.io", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    LocalDrawIoCanvas(
                        challengeId = selectedChallenge.id,
                        xmlSource = latestXml,
                        onXmlUpdated = { updated -> latestXml = updated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            } else {
                key(webViewInstanceKey) {
                    DrawIoWebView(
                        starterXml = latestXml,
                        onWebViewReady = { wv -> webViewRef = wv },
                        onEditorInitialized = {
                            isEditorReady = true
                            webError = null
                            crashRetryCount = 0
                        },
                        onAutosaveXml = { xml ->
                            if (xml.isNotBlank()) latestXml = xml
                        },
                        onExportCompleted = { bitmap, exportedXml ->
                            isExporting = false
                            val finalXml = exportedXml?.takeIf { it.isNotBlank() } ?: latestXml
                            onAnalyzeDiagram(bitmap, finalXml)
                        },
                        onLoadError = { err ->
                            webError = err
                        },
                        onRendererCrashed = {
                            Log.w("DrawIoLab", "WebView renderer terminated; recovering Draw.io (attempt=${crashRetryCount + 1})")
                            webViewRef = null
                            isEditorReady = false
                            isExporting = false
                            if (crashRetryCount < 2) {
                                crashRetryCount++
                                webError = null
                                webViewInstanceKey++
                            } else {
                                webError = "Draw.io renderer ran out of memory on this device. Tap Retry to reload Draw.io."
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isEditorReady && webError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepNavySurface.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyanPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading Draw.io Studio…",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (webError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepNavySurface.copy(alpha = 0.94f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Draw.io failed to load",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = webError ?: "Check network connectivity and retry.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        webError = null
                                        isEditorReady = false
                                        crashRetryCount = 0
                                        webViewInstanceKey++
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary,
                                        contentColor = Color(0xFF041C24)
                                    )
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Draw.io")
                                }
                                OutlinedButton(
                                    onClick = {
                                        webError = null
                                        useOfflineFallback = true
                                    }
                                ) {
                                    Text("Offline Mode")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Bar: Reset Starter & AI Analysis CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    latestXml = selectedChallenge.starterXml
                    if (!useOfflineFallback && isEditorReady) {
                        sendDrawIoLoad(webViewRef, selectedChallenge.starterXml)
                    }
                },
                enabled = (useOfflineFallback || isEditorReady) && !isExporting && analysisState !is UiState.Loading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("reset_drawio_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset diagram",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.reset_canvas))
            }

            Button(
                onClick = {
                    if (isExporting || analysisState is UiState.Loading) return@Button
                    if (useOfflineFallback) {
                        val (parsedNodes, parsedEdges) = parseDrawIoXml(latestXml)
                        val bmp = renderErDiagramToBitmap(parsedNodes, parsedEdges)
                        onAnalyzeDiagram(bmp, latestXml)
                    } else {
                        isExporting = true
                        if (isEditorReady && webViewRef != null) {
                            requestDrawIoExport(webViewRef)
                            scope.launch {
                                delay(4000L)
                                if (isExporting) {
                                    isExporting = false
                                    val fallbackBitmap = webViewRef?.let { captureWebViewBitmap(it) }
                                    onAnalyzeDiagram(fallbackBitmap, latestXml)
                                }
                            }
                        } else {
                            isExporting = false
                            val fallbackBitmap = webViewRef?.let { captureWebViewBitmap(it) }
                            onAnalyzeDiagram(fallbackBitmap, latestXml)
                        }
                    }
                },
                enabled = !isExporting && analysisState !is UiState.Loading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF041C24)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("analyze_diagram_button")
            ) {
                if (isExporting || analysisState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF041C24),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini Analyzing…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.analyze_diagram),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // AI Evaluation Result Modal Bottom Sheet
    if (analysisState is UiState.Success || analysisState is UiState.Error) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissAnalysis,
            sheetState = sheetState,
            containerColor = ElevatedNavyCard
        ) {
            when (analysisState) {
                is UiState.Success -> {
                    DiagramResultSheetContent(
                        result = analysisState.data,
                        onDismiss = onDismissAnalysis
                    )
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Analysis Unavailable",
                            style = MaterialTheme.typography.headlineMedium,
                            color = RoseLoss
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysisState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onDismissAnalysis,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Draw.io")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun DiagramResultSheetContent(
    result: DiagramAnalysisResult,
    onDismiss: () -> Unit
) {
    val accent = if (result.passed) EmeraldWin else RoseLoss
    Box(modifier = Modifier.fillMaxWidth()) {
        if (result.passed) {
            VictoryFireworksCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        } else {
            DefeatPulseCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (result.passed) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (result.passed) "SCHEMA VERIFIED" else "NEEDS REVISION",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = result.verdictTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.2f))
                        .border(1.dp, accent, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${result.score}/100",
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AmberGold.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "+${result.xpAwarded} XP Awarded",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = result.feedbackSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (result.strengths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "What You Modeled Well",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldWin
                )
                Spacer(modifier = Modifier.height(6.dp))
                result.strengths.forEach { item ->
                    Text(
                        text = "✓ $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            if (result.missingOrIncorrect.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Missing or Needs Fixing",
                    style = MaterialTheme.typography.titleMedium,
                    color = RoseLoss
                )
                Spacer(modifier = Modifier.height(6.dp))
                result.missingOrIncorrect.forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF041C24)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Continue Diagramming",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DrawIoWebView(
    starterXml: String,
    onWebViewReady: (WebView) -> Unit,
    onEditorInitialized: () -> Unit,
    onAutosaveXml: (String) -> Unit,
    onExportCompleted: (Bitmap?, String?) -> Unit,
    onLoadError: (String) -> Unit,
    onRendererCrashed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialXml = remember { starterXml }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(android.graphics.Color.parseColor("#111830"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowContentAccess = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false
                    }
                }

                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onDrawIoMessage(rawJson: String) {
                            try {
                                val json = JSONObject(rawJson)
                                when (json.optString("event")) {
                                    "init" -> {
                                        post { onEditorInitialized() }
                                    }
                                    "autosave", "save" -> {
                                        val xml = json.optString("xml")
                                        post { onAutosaveXml(xml) }
                                    }
                                    "export" -> {
                                        val dataUrl = json.optString("data")
                                        val xml = json.optString("xml")
                                        val bmp = decodeBase64DataUrl(dataUrl)
                                        post { onExportCompleted(bmp, xml) }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("DrawIoBridge", "Failed to parse Draw.io message: ${e.message}")
                            }
                        }
                    },
                    "AndroidDrawIo"
                )

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val host = request?.url?.host.orEmpty()
                        return !(host.endsWith("diagrams.net") ||
                            host.endsWith("draw.io") ||
                            host == "appassets.androidplatform.net")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        val urlStr = request?.url?.toString().orEmpty()
                        if (request?.isForMainFrame == true || urlStr.contains("embed.diagrams.net")) {
                            onLoadError(error?.description?.toString() ?: "Unable to connect to Draw.io")
                        }
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        val crashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detail?.didCrash() ?: true
                        } else {
                            true
                        }
                        Log.e("DrawIoWebView", "Chromium render process gone (didCrash=$crashed); recovering Draw.io.")
                        try {
                            (view?.parent as? ViewGroup)?.removeView(view)
                            view?.destroy()
                        } catch (_: Exception) {
                        }
                        onRendererCrashed()
                        return true
                    }
                }

                onWebViewReady(this)
                loadDataWithBaseURL(
                    DRAWIO_BASE_URL,
                    buildDrawIoHostHtml(initialXml),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        onRelease = { webView ->
            try {
                webView.stopLoading()
                webView.removeJavascriptInterface("AndroidDrawIo")
                webView.destroy()
            } catch (_: Exception) {
            }
        }
    )
}

private fun buildDrawIoHostHtml(initialXml: String): String {
    val quotedXml = JSONObject.quote(initialXml).replace("</", "<\\/")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              overflow: hidden;
              background-color: #111830;
            }
            iframe {
              border: 0;
              width: 100%;
              height: 100%;
              display: block;
            }
          </style>
        </head>
        <body>
          <iframe id="drawioFrame" allow="clipboard-read; clipboard-write"></iframe>
          <script>
            (function() {
              var frame = document.getElementById('drawioFrame');
              var editorReady = false;
              var pendingXml = $quotedXml;

              function postToEditor(obj) {
                if (frame && frame.contentWindow) {
                  frame.contentWindow.postMessage(JSON.stringify(obj), '*');
                }
              }

              window.loadDrawIoXml = function(xml) {
                pendingXml = xml;
                if (editorReady && xml) {
                  postToEditor({ action: 'load', xml: xml, autosave: 1 });
                }
              };

              window.exportDrawIoPng = function() {
                if (editorReady) {
                  postToEditor({ action: 'export', format: 'png', xml: true, spin: 'Exporting diagram...' });
                }
              };

              window.addEventListener('message', function(evt) {
                if (!evt.data) return;
                var msg;
                try {
                  msg = typeof evt.data === 'string' ? JSON.parse(evt.data) : evt.data;
                } catch (e) {
                  return;
                }
                if (!msg || !msg.event) return;

                if (msg.event === 'configure') {
                  postToEditor({
                    action: 'configure',
                    config: {
                      defaultLibraries: 'er;general',
                      enabledLibraries: ['er', 'general', 'uml']
                    }
                  });
                } else if (msg.event === 'init') {
                  editorReady = true;
                  if (pendingXml) {
                    postToEditor({ action: 'load', xml: pendingXml, autosave: 1 });
                  }
                }

                if (window.AndroidDrawIo) {
                  window.AndroidDrawIo.onDrawIoMessage(JSON.stringify(msg));
                }
              });

              frame.src = '$DRAWIO_IFRAME_URL';
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun sendDrawIoLoad(webView: WebView?, xml: String) {
    val quotedXml = JSONObject.quote(xml)
    webView?.evaluateJavascript("if (window.loadDrawIoXml) window.loadDrawIoXml($quotedXml);", null)
}

private fun requestDrawIoExport(webView: WebView?) {
    webView?.evaluateJavascript("if (window.exportDrawIoPng) window.exportDrawIoPng();", null)
}

private fun decodeBase64DataUrl(dataUrl: String): Bitmap? {
    if (dataUrl.isBlank()) return null
    return try {
        val base64Part = dataUrl.substringAfter("base64,", "")
        if (base64Part.isBlank()) return null
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

private fun captureWebViewBitmap(webView: WebView): Bitmap? {
    if (webView.width <= 0 || webView.height <= 0) return null
    return try {
        val bmp = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        webView.draw(canvas)
        bmp
    } catch (_: Exception) {
        null
    }
}

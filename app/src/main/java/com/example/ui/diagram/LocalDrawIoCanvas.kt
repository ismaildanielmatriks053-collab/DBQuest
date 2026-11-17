package com.example.ui.diagram

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Xml
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepNavySurface
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletSecondary
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.roundToInt

data class ErNode(
    val id: String,
    val label: String,
    val x: Float,
    val y: Float,
    val width: Float = 155f,
    val height: Float = 110f,
    val isNote: Boolean = false
)

data class ErEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val label: String = "1:N"
)

fun parseDrawIoXml(xml: String): Pair<List<ErNode>, List<ErEdge>> {
    val nodes = mutableListOf<ErNode>()
    val edges = mutableListOf<ErEdge>()
    if (xml.isBlank()) return nodes to edges

    try {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var currentId = ""
        var currentValue = ""
        var currentStyle = ""
        var isVertex = false
        var isEdge = false
        var source = ""
        var target = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "mxCell" -> {
                            currentId = parser.getAttributeValue(null, "id").orEmpty()
                            currentValue = parser.getAttributeValue(null, "value").orEmpty()
                                .replace("<br>", "\n")
                                .replace("<br/>", "\n")
                                .replace("&#xa;", "\n")
                            currentStyle = parser.getAttributeValue(null, "style").orEmpty()
                            isVertex = parser.getAttributeValue(null, "vertex") == "1"
                            isEdge = parser.getAttributeValue(null, "edge") == "1"
                            source = parser.getAttributeValue(null, "source").orEmpty()
                            target = parser.getAttributeValue(null, "target").orEmpty()

                            if (isEdge && source.isNotBlank() && target.isNotBlank()) {
                                edges.add(
                                    ErEdge(
                                        id = currentId.ifBlank { "edge_${edges.size + 1}" },
                                        sourceId = source,
                                        targetId = target,
                                        label = currentValue.ifBlank { "1:N" }
                                    )
                                )
                            }
                        }
                        "mxGeometry" -> {
                            if (isVertex && currentId != "0" && currentId != "1") {
                                val x = parser.getAttributeValue(null, "x")?.toFloatOrNull() ?: 60f
                                val y = parser.getAttributeValue(null, "y")?.toFloatOrNull() ?: 80f
                                val w = parser.getAttributeValue(null, "width")?.toFloatOrNull() ?: 155f
                                val h = parser.getAttributeValue(null, "height")?.toFloatOrNull() ?: 110f
                                nodes.add(
                                    ErNode(
                                        id = currentId,
                                        label = currentValue.ifBlank { "Entity" },
                                        x = x,
                                        y = y,
                                        width = w,
                                        height = h,
                                        isNote = currentStyle.contains("shape=note")
                                    )
                                )
                                isVertex = false
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "mxCell") {
                        isVertex = false
                        isEdge = false
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (_: Exception) {
        // Return any parsed cells cleanly
    }
    return nodes to edges
}

fun buildDrawIoXml(nodes: List<ErNode>, edges: List<ErEdge>): String {
    fun escapeAttr(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\n", "&#xa;")

    val sb = StringBuilder()
    sb.append("""<mxfile host="embed.diagrams.net"><diagram id="dbquest_erd" name="ERD"><mxGraphModel dx="900" dy="600" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169"><root><mxCell id="0"/><mxCell id="1" parent="0"/>""")
    nodes.forEach { node ->
        val style = if (node.isNote) {
            "shape=note;whiteSpace=wrap;html=1;fillColor=#452c09;strokeColor=#f59e0b;fontColor=#fde68a;"
        } else {
            "rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#22d3ee;fontColor=#f8fafc;fontStyle=1;"
        }
        sb.append(
            """<mxCell id="${escapeAttr(node.id)}" value="${escapeAttr(node.label)}" style="$style" vertex="1" parent="1"><mxGeometry x="${node.x.roundToInt()}" y="${node.y.roundToInt()}" width="${node.width.roundToInt()}" height="${node.height.roundToInt()}" as="geometry"/></mxCell>"""
        )
    }
    edges.forEach { edge ->
        sb.append(
            """<mxCell id="${escapeAttr(edge.id)}" value="${escapeAttr(edge.label)}" style="edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;strokeColor=#22d3ee;fontColor=#f8fafc;" edge="1" parent="1" source="${escapeAttr(edge.sourceId)}" target="${escapeAttr(edge.targetId)}"><mxGeometry relative="1" as="geometry"/></mxCell>"""
        )
    }
    sb.append("</root></mxGraphModel></diagram></mxfile>")
    return sb.toString()
}

fun renderErDiagramToBitmap(nodes: List<ErNode>, edges: List<ErEdge>): Bitmap {
    val width = 1024
    val height = 720
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.parseColor("#0B1020"))

    val gridPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A2342")
        strokeWidth = 1.5f
    }
    for (gx in 0..width step 40) {
        canvas.drawLine(gx.toFloat(), 0f, gx.toFloat(), height.toFloat(), gridPaint)
    }
    for (gy in 0..height step 40) {
        canvas.drawLine(0f, gy.toFloat(), width.toFloat(), gy.toFloat(), gridPaint)
    }

    val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#22D3EE")
        strokeWidth = 4f
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F59E0B")
        textSize = 24f
        isFakeBoldText = true
    }

    val nodeMap = nodes.associateBy { it.id }
    val scale = 1.35f
    edges.forEach { edge ->
        val src = nodeMap[edge.sourceId]
        val dst = nodeMap[edge.targetId]
        if (src != null && dst != null) {
            val sx = (src.x + src.width / 2f) * scale
            val sy = (src.y + src.height / 2f) * scale
            val tx = (dst.x + dst.width / 2f) * scale
            val ty = (dst.y + dst.height / 2f) * scale
            canvas.drawLine(sx, sy, tx, ty, edgePaint)
            canvas.drawText(edge.label, (sx + tx) / 2f, (sy + ty) / 2f - 8f, labelPaint)
        }
    }

    val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#192242")
    }
    val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#22D3EE")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F1F5F9")
        textSize = 22f
    }

    nodes.forEach { node ->
        val left = node.x * scale
        val top = node.y * scale
        val rect = RectF(left, top, left + node.width * scale, top + node.height * scale)
        canvas.drawRoundRect(rect, 16f, 16f, boxFill)
        canvas.drawRoundRect(rect, 16f, 16f, boxStroke)

        var lineY = top + 30f
        node.label.lines().take(5).forEach { line ->
            canvas.drawText(line.take(24), left + 14f, lineY, textPaint)
            lineY += 26f
        }
    }
    return bitmap
}

@Composable
fun LocalDrawIoCanvas(
    challengeId: String,
    xmlSource: String,
    onXmlUpdated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodes = remember { mutableStateListOf<ErNode>() }
    val edges = remember { mutableStateListOf<ErEdge>() }

    LaunchedEffect(challengeId, xmlSource) {
        val (parsedNodes, parsedEdges) = parseDrawIoXml(xmlSource)
        if (nodes.isEmpty() || challengeId.isNotBlank()) {
            // Re-sync when challenge changes or xml externally resets
            if (buildDrawIoXml(nodes, edges) != xmlSource) {
                nodes.clear()
                nodes.addAll(parsedNodes)
                edges.clear()
                edges.addAll(parsedEdges)
            }
        }
    }

    var showAddEntityDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<ErNode?>(null) }

    val density = LocalDensity.current

    fun syncXml() {
        onXmlUpdated(buildDrawIoXml(nodes.toList(), edges.toList()))
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Compact ER Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElevatedNavyCard)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showAddEntityDialog = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("local_add_entity_button")
            ) {
                Icon(Icons.Rounded.AddBox, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanPrimary)
                Spacer(modifier = Modifier.width(5.dp))
                Text("Add Entity", style = MaterialTheme.typography.labelMedium, color = CyanPrimary)
            }

            OutlinedButton(
                onClick = { showAddLinkDialog = true },
                enabled = nodes.size >= 2,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("local_add_link_button")
            ) {
                Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberGold)
                Spacer(modifier = Modifier.width(5.dp))
                Text("Add Relation", style = MaterialTheme.typography.labelMedium, color = AmberGold)
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Drag cards • Tap to edit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Interactive Blueprint Grid Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DeepNavySurface)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 28.dp.toPx()
                val gridColor = Color(0xFF1A2342)
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                // Draw relationship edges
                val map = nodes.associateBy { it.id }
                val scalePx = 0.65.dp.toPx()
                edges.forEach { edge ->
                    val s = map[edge.sourceId]
                    val t = map[edge.targetId]
                    if (s != null && t != null) {
                        val p1 = Offset((s.x + s.width / 2f) * scalePx, (s.y + s.height / 2f) * scalePx)
                        val p2 = Offset((t.x + t.width / 2f) * scalePx, (t.y + t.height / 2f) * scalePx)
                        drawLine(
                            color = CyanPrimary,
                            start = p1,
                            end = p2,
                            strokeWidth = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                        )
                        drawCircle(AmberGold, radius = 5.dp.toPx(), center = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f))
                    }
                }
            }

            // Draggable Entity Nodes
            nodes.forEach { node ->
                val xPx = with(density) { (node.x * 0.65f).dp.toPx() }
                val yPx = with(density) { (node.y * 0.65f).dp.toPx() }
                val borderTint = when {
                    node.isNote -> AmberGold
                    node.label.contains("Enrollment") || node.label.contains("OrderItem") || node.label.contains("Appointment") -> EmeraldWin
                    else -> CyanPrimary
                }

                Column(
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .width(136.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedNavyCard)
                        .border(1.5.dp, borderTint, RoundedCornerShape(12.dp))
                        .pointerInput(node.id) {
                            detectDragGestures(
                                onDragEnd = { syncXml() }
                            ) { change, dragAmount ->
                                change.consume()
                                val idx = nodes.indexOfFirst { it.id == node.id }
                                if (idx >= 0) {
                                    val dx = with(density) { dragAmount.x.toDp().value / 0.65f }
                                    val dy = with(density) { dragAmount.y.toDp().value / 0.65f }
                                    val cur = nodes[idx]
                                    nodes[idx] = cur.copy(
                                        x = (cur.x + dx).coerceIn(0f, 650f),
                                        y = (cur.y + dy).coerceIn(0f, 520f)
                                    )
                                }
                            }
                        }
                        .clickable { editingNode = node }
                        .padding(8.dp)
                ) {
                    val lines = node.label.lines().filter { !it.startsWith("──") }
                    Text(
                        text = lines.firstOrNull() ?: "Entity",
                        style = MaterialTheme.typography.labelLarge,
                        color = borderTint,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    lines.drop(1).take(4).forEach { attr ->
                        Text(
                            text = attr,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showAddEntityDialog) {
        var name by remember { mutableStateOf("") }
        var attrs by remember { mutableStateOf("PK id\nname") }
        AlertDialog(
            onDismissRequest = { showAddEntityDialog = false },
            containerColor = ElevatedNavyCard,
            title = { Text("Add Entity Table", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Entity Name (e.g., Enrollment)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = attrs,
                        onValueChange = { attrs = it },
                        label = { Text("Attributes (one per line, e.g. PK id, FK student_id)") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = name.trim().ifBlank { "Entity_${nodes.size + 1}" }
                        val body = "$cleanName\n──────────\n${attrs.trim()}"
                        val offset = (nodes.size % 4) * 55f
                        nodes.add(
                            ErNode(
                                id = "node_${System.currentTimeMillis()}",
                                label = body,
                                x = 140f + offset,
                                y = 120f + offset
                            )
                        )
                        syncXml()
                        showAddEntityDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color(0xFF041C24))
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEntityDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddLinkDialog && nodes.size >= 2) {
        var sourceIdx by remember { mutableStateOf(0) }
        var targetIdx by remember { mutableStateOf(1) }
        var cardinality by remember { mutableStateOf("1:N") }
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            containerColor = ElevatedNavyCard,
            title = { Text("Connect Entities", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("From:", style = MaterialTheme.typography.labelMedium, color = CyanPrimary)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(nodes.size) { idx ->
                            val n = nodes[idx]
                            FilterChip(
                                selected = sourceIdx == idx,
                                onClick = { sourceIdx = idx },
                                label = { Text(n.label.lines().firstOrNull()?.take(14) ?: "E$idx") }
                            )
                        }
                    }
                    Text("To:", style = MaterialTheme.typography.labelMedium, color = VioletSecondary)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(nodes.size) { idx ->
                            val n = nodes[idx]
                            FilterChip(
                                selected = targetIdx == idx,
                                onClick = { targetIdx = idx },
                                label = { Text(n.label.lines().firstOrNull()?.take(14) ?: "E$idx") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = cardinality,
                        onValueChange = { cardinality = it },
                        label = { Text("Cardinality (1:1, 1:N, M:N)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = nodes.getOrNull(sourceIdx)
                        val t = nodes.getOrNull(targetIdx)
                        if (s != null && t != null && s.id != t.id) {
                            edges.add(
                                ErEdge(
                                    id = "edge_${System.currentTimeMillis()}",
                                    sourceId = s.id,
                                    targetId = t.id,
                                    label = cardinality.ifBlank { "1:N" }
                                )
                            )
                            syncXml()
                        }
                        showAddLinkDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color(0xFF041C24))
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    val activeEdit = editingNode
    if (activeEdit != null) {
        var editedText by remember(activeEdit.id) { mutableStateOf(activeEdit.label) }
        AlertDialog(
            onDismissRequest = { editingNode = null },
            containerColor = ElevatedNavyCard,
            title = { Text("Edit Entity", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Table & Attributes") },
                    minLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idx = nodes.indexOfFirst { it.id == activeEdit.id }
                        if (idx >= 0) {
                            nodes[idx] = activeEdit.copy(label = editedText.trim())
                            syncXml()
                        }
                        editingNode = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color(0xFF041C24))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            nodes.removeAll { it.id == activeEdit.id }
                            edges.removeAll { it.sourceId == activeEdit.id || it.targetId == activeEdit.id }
                            syncXml()
                            editingNode = null
                        }
                    ) {
                        Text("Delete", color = RoseLoss)
                    }
                    TextButton(onClick = { editingNode = null }) { Text("Cancel") }
                }
            }
        )
    }
}

package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.DiagramAnalysisResult
import com.example.model.DiagramChallenge
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiRepository {

    private val diagramAnalysisSchema = Schema.obj(
        properties = mapOf(
            "passed" to Schema.boolean(description = "True if the ER diagram satisfies the core database design requirements with score >= 65."),
            "score" to Schema.integer(description = "Accuracy and completeness score from 0 to 100."),
            "verdictTitle" to Schema.string(description = "Short 3-6 word verdict headline, e.g., 'Solid 3NF Schema!' or 'Missing Junction Table'."),
            "feedbackSummary" to Schema.string(description = "Clear 2-3 sentence evaluation explaining why the diagram succeeded or what database design concepts need fixing."),
            "strengths" to Schema.array(
                items = Schema.string(),
                description = "List of up to 4 things the player modeled correctly."
            ),
            "missingOrIncorrect" to Schema.array(
                items = Schema.string(),
                description = "List of up to 4 missing entities, keys, relationships, or normalization issues."
            )
        )
    )

    private val diagramModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagramAnalysisSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are a strict yet encouraging Database Systems Professor grading a student's Draw.io ER/Relational diagram. " +
                        "Inspect both the rendered diagram image (if provided) and the Draw.io mxGraph XML structure. " +
                        "Check for required entities, Primary Keys (PK), Foreign Keys (FK), junction/associative tables for M:N relationships, and valid connectors. " +
                        "Award score >= 65 and passed = true only if the student actually modeled the core entities and relationships beyond the starter stub."
                )
            }
        )
    }

    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.6f
            },
            systemInstruction = content {
                text(
                    "You are Oracle, the DATA QUESTZ AI Database Tutor. Help students master relational databases, SQL queries, " +
                        "Entity-Relationship (ER) modeling, normalization (1NF, 2NF, 3NF, BCNF), ACID transactions, and indexing. " +
                        "Keep explanations crisp, visual, and practical with short SQL or schema examples. Encourage active learning."
                )
            }
        )
    }

    private var tutorChat: Chat? = null

    private fun getOrCreateChat(): Chat {
        return tutorChat ?: tutorModel.startChat(
            history = listOf(
                content("user") { text("Hi Oracle! I'm playing DATA QUESTZ to master databases.") },
                content("model") {
                    text(
                        "Welcome to DATA QUESTZ! I'm Oracle, your Database Systems Tutor. " +
                            "Ask me about SQL joins, ER diagrams in Draw.io, normalization (1NF–3NF), ACID transactions, or indexing — let's level up your schema skills!"
                    )
                }
            )
        ).also { tutorChat = it }
    }

    fun resetTutorChat() {
        tutorChat = null
    }

    suspend fun analyzeDiagram(
        challenge: DiagramChallenge,
        diagramBitmap: Bitmap?,
        mxGraphXml: String?
    ): DiagramAnalysisResult {
        val cleanedXml = mxGraphXml?.take(14_000).orEmpty()
        val promptText = buildString {
            appendLine("Challenge Title: ${challenge.title}")
            appendLine("Difficulty: ${challenge.difficulty}")
            appendLine("Scenario: ${challenge.scenario}")
            appendLine("Required Criteria:")
            challenge.requirements.forEachIndexed { idx, req ->
                appendLine("${idx + 1}. $req")
            }
            if (cleanedXml.isNotBlank()) {
                appendLine("\nDraw.io mxGraph XML:")
                appendLine(cleanedXml)
            } else {
                appendLine("\nNote: Analyze the attached rendered diagram image carefully.")
            }
        }

        try {
            val response = withTimeout(60_000L) {
                diagramModel.generateContent(
                    content {
                        if (diagramBitmap != null) {
                            image(diagramBitmap)
                        }
                        text(promptText)
                    }
                )
            }

            val jsonString = response.text
                ?: throw IllegalStateException("AI returned an empty response while analyzing diagram.")
            val json = JSONObject(jsonString)

            val passed = json.optBoolean("passed", false)
            val score = json.optInt("score", 0).coerceIn(0, 100)
            val verdictTitle = json.optString("verdictTitle", if (passed) "Schema Approved!" else "Needs Refinement")
            val feedbackSummary = json.optString("feedbackSummary", "Evaluation complete.")

            val strengthsArr = json.optJSONArray("strengths")
            val strengths = if (strengthsArr != null) {
                (0 until strengthsArr.length()).mapNotNull { strengthsArr.optString(it).takeIf { s -> s.isNotBlank() } }
            } else emptyList()

            val issuesArr = json.optJSONArray("missingOrIncorrect")
            val missing = if (issuesArr != null) {
                (0 until issuesArr.length()).mapNotNull { issuesArr.optString(it).takeIf { s -> s.isNotBlank() } }
            } else emptyList()

            val xpAwarded = if (passed) {
                (challenge.xpReward * (score / 100.0)).toLong().coerceAtLeast(challenge.xpReward / 2)
            } else {
                20L // Participation XP for trying
            }

            return DiagramAnalysisResult(
                passed = passed,
                score = score,
                verdictTitle = verdictTitle,
                feedbackSummary = feedbackSummary,
                strengths = strengths,
                missingOrIncorrect = missing,
                xpAwarded = xpAwarded
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Diagram analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Diagram analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to analyze diagram with Gemini AI.")
        }
    }

    fun sendTutorMessageStream(userMessage: String): Flow<String> = flow {
        val chat = getOrCreateChat()
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty reply.")
        }
    }.catch { e ->
        if (e is CancellationException && e !is TimeoutCancellationException) {
            throw e
        }
        Log.e(TAG, "Tutor chat stream failed", e)
        throw when (e) {
            is TimeoutCancellationException -> IllegalStateException("Tutor response timed out. Please try again.")
            else -> IllegalStateException(e.localizedMessage ?: "Failed to get response from Gemini Tutor.")
        }
    }

    private companion object {
        const val TAG = "AiRepository"
    }
}

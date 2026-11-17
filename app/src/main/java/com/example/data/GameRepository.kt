package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.DiagramAttempt
import com.example.model.LecturerQuiz
import com.example.model.QuizQuestion
import com.example.model.QuizResultRecord
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = com.google.firebase.Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            val docRef = db.collection("users").document(userId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = displayName.trim().ifBlank { "DB Explorer" }.take(50)
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to safeName,
                    "totalPoints" to 0L,
                    "quizzesCompleted" to 0L,
                    "quizzesWon" to 0L,
                    "diagramsSolved" to 0L,
                    "currentStreak" to 0L,
                    "bestStreak" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicQuizzes(): Flow<List<LecturerQuiz>> = flow {
        val uid = requireUserId()
        val path = "quizzes"
        emitAll(
            db.collection("quizzes")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LecturerQuiz::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedStarterLecturerQuizzesIfNeeded(): Result<Unit> {
        val path = "quizzes"
        return try {
            val uid = requireUserId()
            val existing = db.collection("quizzes")
                .whereEqualTo("visibility", "public")
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.success(Unit)
            }

            val starters = buildStarterLecturerQuizzes(uid)
            val batch = db.batch()
            starters.forEach { (docId, payload) ->
                val ref = db.collection("quizzes").document(docId)
                batch.set(ref, payload)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishLecturerQuiz(
        lecturerName: String,
        lecturerTitle: String,
        title: String,
        topic: String,
        difficulty: String,
        timePerQuestionSec: Long,
        passThresholdPercent: Long,
        xpReward: Long,
        questions: List<QuizQuestion>
    ): Result<String> {
        val path = "quizzes"
        return try {
            val uid = requireUserId()
            val cleanLecturer = lecturerName.trim()
            val cleanRole = lecturerTitle.trim().ifBlank { "Database Lecturer" }
            val cleanTitle = title.trim()
            val cleanTopic = topic.trim()
            require(cleanLecturer.length in 2..60) { "Lecturer name must be 2-60 characters." }
            require(cleanRole.length in 2..60) { "Lecturer title must be 2-60 characters." }
            require(cleanTitle.length in 3..100) { "Quiz title must be 3-100 characters." }
            require(cleanTopic.length in 2..40) { "Topic must be 2-40 characters." }
            require(difficulty in listOf("Beginner", "Intermediate", "Advanced")) { "Invalid difficulty." }
            require(timePerQuestionSec in 5L..60L) { "Time per question must be 5-60 seconds." }
            require(passThresholdPercent in 40L..100L) { "Pass threshold must be 40-100%." }
            require(xpReward in 50L..1000L) { "XP reward must be 50-1000." }
            require(questions.size in 1..15) { "Quiz must contain 1-15 questions." }

            val docRef = db.collection("quizzes").document()
            val questionMaps = questions.map { q ->
                mapOf(
                    "prompt" to q.prompt.trim(),
                    "options" to q.options.map { it.trim() },
                    "correctIndex" to q.correctIndex.coerceIn(0L, 3L),
                    "explanation" to q.explanation.trim().ifBlank { "Review database fundamentals for this concept." }
                )
            }
            val payload = mapOf(
                "creatorId" to uid,
                "lecturerName" to cleanLecturer,
                "lecturerTitle" to cleanRole,
                "title" to cleanTitle,
                "topic" to cleanTopic,
                "difficulty" to difficulty,
                "visibility" to "public",
                "timePerQuestionSec" to timePerQuestionSec,
                "passThresholdPercent" to passThresholdPercent,
                "xpReward" to xpReward,
                "questions" to questionMaps,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizResult(
        userId: String,
        quiz: LecturerQuiz,
        score: Long,
        correctCount: Long,
        totalQuestions: Long,
        accuracyPercent: Long,
        won: Boolean,
        pointsEarned: Long,
        maxStreak: Long,
        currentProfile: UserProfile?
    ): Result<Unit> {
        val resultPath = "users/$userId/quizResults"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val resultRef = db.collection("users").document(userId).collection("quizResults").document()
            val userRef = db.collection("users").document(userId)

            val prevStreak = currentProfile?.currentStreak ?: 0L
            val prevBest = currentProfile?.bestStreak ?: 0L
            val newStreak = if (won) prevStreak + 1L else 0L
            val newBest = maxOf(prevBest, newStreak)

            val resultPayload = mapOf(
                "userId" to userId,
                "quizId" to quiz.id.ifBlank { "custom_quiz" }.take(128),
                "quizTitle" to quiz.title.take(100).ifBlank { "Database Quiz" },
                "lecturerName" to quiz.lecturerName.take(60).ifBlank { "Lecturer" },
                "score" to score.coerceIn(0L, 100000L),
                "correctCount" to correctCount.coerceIn(0L, 15L),
                "totalQuestions" to totalQuestions.coerceIn(1L, 15L),
                "accuracyPercent" to accuracyPercent.coerceIn(0L, 100L),
                "outcome" to if (won) "WIN" else "LOSS",
                "pointsEarned" to pointsEarned.coerceIn(0L, 5000L),
                "maxStreak" to maxStreak.coerceIn(0L, 15L),
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(resultRef, resultPayload)
            batch.update(
                userRef,
                mapOf(
                    "totalPoints" to FieldValue.increment(pointsEarned.coerceIn(0L, 5000L)),
                    "quizzesCompleted" to FieldValue.increment(1L),
                    "quizzesWon" to FieldValue.increment(if (won) 1L else 0L),
                    "currentStreak" to newStreak,
                    "bestStreak" to newBest,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, resultPath)
            Result.failure(e)
        }
    }

    fun observeQuizResults(userId: String): Flow<List<QuizResultRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$userId/quizResults"
        emitAll(
            db.collection("users").document(userId).collection("quizResults")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(QuizResultRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun recordDiagramAttempt(
        userId: String,
        challengeId: String,
        challengeTitle: String,
        verdictTitle: String,
        feedbackSummary: String,
        score: Int,
        passed: Boolean,
        xpAwarded: Long
    ): Result<Unit> {
        val path = "users/$userId/diagramAttempts"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }

            val attemptRef = db.collection("users").document(userId).collection("diagramAttempts").document()
            val userRef = db.collection("users").document(userId)

            val payload = mapOf(
                "userId" to userId,
                "challengeId" to challengeId.take(64).ifBlank { "challenge" },
                "challengeTitle" to challengeTitle.take(100).ifBlank { "ER Diagram" },
                "verdictTitle" to verdictTitle.take(100).ifBlank { "Evaluation" },
                "feedbackSummary" to feedbackSummary.take(1200).ifBlank { "Evaluated by AI." },
                "score" to score.toLong().coerceIn(0L, 100L),
                "passed" to passed,
                "xpAwarded" to xpAwarded.coerceIn(0L, 1000L),
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(attemptRef, payload)
            batch.update(
                userRef,
                mapOf(
                    "totalPoints" to FieldValue.increment(xpAwarded.coerceIn(0L, 1000L)),
                    "diagramsSolved" to FieldValue.increment(if (passed) 1L else 0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeDiagramAttempts(userId: String): Flow<List<DiagramAttempt>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$userId/diagramAttempts"
        emitAll(
            db.collection("users").document(userId).collection("diagramAttempts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DiagramAttempt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun buildStarterLecturerQuizzes(creatorUid: String): Map<String, Map<String, Any>> {
        val now = FieldValue.serverTimestamp()
        return com.example.model.StarterQuizCatalog.starterQuizzes.associate { quiz ->
            quiz.id to mapOf(
                "creatorId" to creatorUid,
                "lecturerName" to quiz.lecturerName,
                "lecturerTitle" to quiz.lecturerTitle,
                "title" to quiz.title,
                "topic" to quiz.topic,
                "difficulty" to quiz.difficulty,
                "visibility" to "public",
                "timePerQuestionSec" to quiz.timePerQuestionSec,
                "passThresholdPercent" to quiz.passThresholdPercent,
                "xpReward" to quiz.xpReward,
                "questions" to quiz.questions.map { q ->
                    mapOf(
                        "prompt" to q.prompt,
                        "options" to q.options,
                        "correctIndex" to q.correctIndex,
                        "explanation" to q.explanation
                    )
                },
                "createdAt" to now,
                "updatedAt" to now
            )
        }
    }
}

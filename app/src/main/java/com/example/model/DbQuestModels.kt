package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "DB Explorer",
    val totalPoints: Long = 0L,
    val quizzesCompleted: Long = 0L,
    val quizzesWon: Long = 0L,
    val diagramsSolved: Long = 0L,
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanation: String = ""
)

@Keep
@IgnoreExtraProperties
data class LecturerQuiz(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val lecturerName: String = "",
    val lecturerTitle: String = "",
    val title: String = "",
    val topic: String = "",
    val difficulty: String = "Intermediate",
    val visibility: String = "public",
    val timePerQuestionSec: Long = 15L,
    val passThresholdPercent: Long = 60L,
    val xpReward: Long = 200L,
    val questions: List<QuizQuestion> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizResultRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val lecturerName: String = "",
    val score: Long = 0L,
    val correctCount: Long = 0L,
    val totalQuestions: Long = 0L,
    val accuracyPercent: Long = 0L,
    val outcome: String = "LOSS",
    val pointsEarned: Long = 0L,
    val maxStreak: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DiagramAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val challengeId: String = "",
    val challengeTitle: String = "",
    val verdictTitle: String = "",
    val feedbackSummary: String = "",
    val score: Long = 0L,
    val passed: Boolean = false,
    val xpAwarded: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DiagramChallenge(
    val id: String,
    val title: String,
    val difficulty: String,
    val xpReward: Long,
    val scenario: String,
    val requirements: List<String>,
    val starterXml: String
)

data class DiagramAnalysisResult(
    val passed: Boolean,
    val score: Int,
    val verdictTitle: String,
    val feedbackSummary: String,
    val strengths: List<String>,
    val missingOrIncorrect: List<String>,
    val xpAwarded: Long
)

data class ChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

enum class AppTab {
    HOME,
    DIAGRAM_LAB,
    SPEED_QUIZ,
    AI_TUTOR
}

object PlayerRankHelper {
    data class RankInfo(val title: String, val level: Int, val nextLevelXp: Long, val progress: Float)

    fun getRank(totalPoints: Long): RankInfo {
        return when {
            totalPoints < 300 -> RankInfo("Query Novice", 1, 300, totalPoints / 300f)
            totalPoints < 800 -> RankInfo("Schema Apprentice", 2, 800, (totalPoints - 300) / 500f)
            totalPoints < 1600 -> RankInfo("Relational Tactician", 3, 1600, (totalPoints - 800) / 800f)
            totalPoints < 3000 -> RankInfo("Index Architect", 4, 3000, (totalPoints - 1600) / 1400f)
            else -> RankInfo("Grand DBA Master", 5, 5000, ((totalPoints - 3000) / 2000f).coerceIn(0f, 1f))
        }
    }
}

object DiagramChallengesCatalog {
    val challenges: List<DiagramChallenge> = listOf(
        DiagramChallenge(
            id = "challenge_university",
            title = "University Course Enrollment ERD",
            difficulty = "Beginner",
            xpReward = 180L,
            scenario = "Design an Entity-Relationship diagram for a university where Students enroll in multiple Courses, and each Course can have many Students. Avoid a direct M:N table in relational schema by introducing an Enrollment junction entity.",
            requirements = listOf(
                "Entity 'Student' with PK student_id, name, email",
                "Entity 'Course' with PK course_id, title, credits",
                "Junction entity 'Enrollment' with PK enrollment_id, FK student_id, FK course_id, grade",
                "1:N relationships from Student -> Enrollment and Course -> Enrollment"
            ),
            starterXml = """
                <mxfile host="embed.diagrams.net"><diagram id="univ1" name="University ERD"><mxGraphModel dx="900" dy="600" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169"><root><mxCell id="0"/><mxCell id="1" parent="0"/><mxCell id="student" value="Student&#xa;──────────&#xa;PK student_id&#xa;name&#xa;email" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#22d3ee;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="60" y="140" width="150" height="110" as="geometry"/></mxCell><mxCell id="course" value="Course&#xa;──────────&#xa;PK course_id&#xa;title&#xa;credits" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#a78bfa;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="440" y="140" width="150" height="110" as="geometry"/></mxCell><mxCell id="hint" value="Add Enrollment junction entity&#xa;with FKs &amp; 1:N connectors!" style="shape=note;whiteSpace=wrap;html=1;fillColor=#452c09;strokeColor=#f59e0b;fontColor=#fde68a;" vertex="1" parent="1"><mxGeometry x="235" y="40" width="180" height="65" as="geometry"/></mxCell></root></mxGraphModel></diagram></mxfile>
            """.trimIndent()
        ),
        DiagramChallenge(
            id = "challenge_ecommerce",
            title = "E-Commerce Order & Payment Schema",
            difficulty = "Intermediate",
            xpReward = 240L,
            scenario = "Model an e-commerce checkout system. A Customer places Orders. Each Order contains one or more Products via OrderItem (with quantity & unit_price), and has a Payment record.",
            requirements = listOf(
                "Customer (PK customer_id, name, email)",
                "Order (PK order_id, FK customer_id, order_date, total)",
                "OrderItem junction (PK item_id, FK order_id, FK product_id, quantity, unit_price)",
                "Product (PK product_id, sku, price) & Payment (PK payment_id, FK order_id, method, status)"
            ),
            starterXml = """
                <mxfile host="embed.diagrams.net"><diagram id="ecom1" name="E-Commerce"><mxGraphModel dx="900" dy="600" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169"><root><mxCell id="0"/><mxCell id="1" parent="0"/><mxCell id="cust" value="Customer&#xa;──────────&#xa;PK customer_id&#xa;name&#xa;email" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#22d3ee;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="50" y="120" width="150" height="100" as="geometry"/></mxCell><mxCell id="ord" value="Order&#xa;──────────&#xa;PK order_id&#xa;FK customer_id&#xa;order_date" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#10b981;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="260" y="120" width="150" height="100" as="geometry"/></mxCell><mxCell id="edge1" style="edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;strokeColor=#22d3ee;fontColor=#f8fafc;" edge="1" parent="1" source="cust" target="ord"><mxGeometry relative="1" as="geometry"/></mxCell></root></mxGraphModel></diagram></mxfile>
            """.trimIndent()
        ),
        DiagramChallenge(
            id = "challenge_hospital",
            title = "Hospital Doctor-Patient Appointments",
            difficulty = "Intermediate",
            xpReward = 260L,
            scenario = "Create a normalized ER diagram for a clinic. Doctors belong to a Department. Patients book Appointments with a Doctor on a specific date/time, and each Appointment can generate a Prescription.",
            requirements = listOf(
                "Department (PK dept_id, dept_name) 1:N Doctor (PK doctor_id, FK dept_id, specialty)",
                "Patient (PK patient_id, full_name, dob)",
                "Appointment (PK appt_id, FK doctor_id, FK patient_id, appt_time, status)",
                "Prescription (PK rx_id, FK appt_id, medication, dosage)"
            ),
            starterXml = """
                <mxfile host="embed.diagrams.net"><diagram id="hosp1" name="Hospital ERD"><mxGraphModel dx="900" dy="600" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169"><root><mxCell id="0"/><mxCell id="1" parent="0"/><mxCell id="doc" value="Doctor&#xa;──────────&#xa;PK doctor_id&#xa;FK dept_id&#xa;specialty" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#22d3ee;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="70" y="140" width="150" height="100" as="geometry"/></mxCell><mxCell id="pat" value="Patient&#xa;──────────&#xa;PK patient_id&#xa;full_name&#xa;dob" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#a78bfa;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="430" y="140" width="150" height="100" as="geometry"/></mxCell></root></mxGraphModel></diagram></mxfile>
            """.trimIndent()
        ),
        DiagramChallenge(
            id = "challenge_library",
            title = "Library Lending & 3NF Normalization",
            difficulty = "Advanced",
            xpReward = 320L,
            scenario = "Normalize a library system into 3NF. A Book has Authors (M:N via BookAuthor) and belongs to a Publisher. A Member borrows physical BookCopies via Loan records.",
            requirements = listOf(
                "Publisher (PK pub_id, name, city) -> Book (PK book_id, FK pub_id, isbn, title)",
                "Author (PK author_id, name) & BookAuthor junction (FK book_id, FK author_id)",
                "BookCopy (PK copy_id, FK book_id, shelf_code, status)",
                "Member (PK member_id, name) -> Loan (PK loan_id, FK copy_id, FK member_id, due_date)"
            ),
            starterXml = """
                <mxfile host="embed.diagrams.net"><diagram id="lib1" name="Library 3NF"><mxGraphModel dx="900" dy="600" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169"><root><mxCell id="0"/><mxCell id="1" parent="0"/><mxCell id="book" value="Book&#xa;──────────&#xa;PK book_id&#xa;FK pub_id&#xa;isbn, title" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#22d3ee;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="220" y="130" width="160" height="105" as="geometry"/></mxCell></root></mxGraphModel></diagram></mxfile>
            """.trimIndent()
        ),
        DiagramChallenge(
            id = "challenge_rideshare",
            title = "Ride-Sharing Dispatch & Ratings",
            difficulty = "Advanced",
            xpReward = 350L,
            scenario = "Model a ride-hailing platform: Riders request Trips, Drivers drive Vehicles and accept Trips, and each completed Trip has a Payment and a Rating.",
            requirements = listOf(
                "Rider (PK rider_id, name, phone) & Driver (PK driver_id, license_no, rating_avg)",
                "Vehicle (PK vehicle_id, FK driver_id, plate_no, model)",
                "Trip (PK trip_id, FK rider_id, FK driver_id, origin, destination, fare, status)",
                "Payment (PK pay_id, FK trip_id, amount, method) & Rating (PK rate_id, FK trip_id, stars)"
            ),
            starterXml = """
                <mxfile host="embed.diagrams.net"><diagram id="ride1" name="RideShare"><mxGraphModel dx="900" dy="600" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="827" pageHeight="1169"><root><mxCell id="0"/><mxCell id="1" parent="0"/><mxCell id="rider" value="Rider&#xa;──────────&#xa;PK rider_id&#xa;name&#xa;phone" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#22d3ee;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="60" y="140" width="140" height="95" as="geometry"/></mxCell><mxCell id="driver" value="Driver&#xa;──────────&#xa;PK driver_id&#xa;license_no&#xa;rating_avg" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1e293b;strokeColor=#10b981;fontColor=#f8fafc;fontStyle=1;" vertex="1" parent="1"><mxGeometry x="430" y="140" width="140" height="95" as="geometry"/></mxCell></root></mxGraphModel></diagram></mxfile>
            """.trimIndent()
        )
    )
}

object StarterQuizCatalog {
    val starterQuizzes: List<LecturerQuiz> = listOf(
        LecturerQuiz(
            id = "starter_joins_vance",
            creatorId = "lecturer_vance",
            lecturerName = "Prof. Elena Vance",
            lecturerTitle = "Relational Systems Chair",
            title = "SQL Joins Blitz",
            topic = "SQL Joins",
            difficulty = "Beginner",
            visibility = "public",
            timePerQuestionSec = 15L,
            passThresholdPercent = 60L,
            xpReward = 200L,
            questions = listOf(
                QuizQuestion(
                    prompt = "Which JOIN returns every row from the left table and NULLs for unmatched right rows?",
                    options = listOf("INNER JOIN", "LEFT OUTER JOIN", "CROSS JOIN", "NATURAL JOIN"),
                    correctIndex = 1L,
                    explanation = "LEFT OUTER JOIN preserves all rows from the left table even if no matching row exists on the right."
                ),
                QuizQuestion(
                    prompt = "What does a CROSS JOIN between a 4-row table and a 5-row table produce?",
                    options = listOf("9 rows", "4 rows", "20 rows", "5 rows"),
                    correctIndex = 2L,
                    explanation = "CROSS JOIN computes the Cartesian product: 4 × 5 = 20 rows."
                ),
                QuizQuestion(
                    prompt = "Which clause filters grouped rows AFTER aggregation (like COUNT or SUM)?",
                    options = listOf("WHERE", "HAVING", "ORDER BY", "DISTINCT"),
                    correctIndex = 1L,
                    explanation = "WHERE filters rows before grouping; HAVING filters groups after GROUP BY."
                ),
                QuizQuestion(
                    prompt = "When joining a table to itself (e.g., Employee -> Manager), what must you use?",
                    options = listOf("Table aliases", "A VIEW", "A TRIGGER", "UNION ALL"),
                    correctIndex = 0L,
                    explanation = "Self-joins require distinct table aliases (e.g., e and m) to disambiguate columns."
                ),
                QuizQuestion(
                    prompt = "Which JOIN excludes rows that do not have a match in both tables?",
                    options = listOf("FULL OUTER JOIN", "LEFT JOIN", "INNER JOIN", "RIGHT JOIN"),
                    correctIndex = 2L,
                    explanation = "INNER JOIN returns only the intersection of matching rows from both tables."
                )
            )
        ),
        LecturerQuiz(
            id = "starter_normalization_chen",
            creatorId = "lecturer_chen",
            lecturerName = "Dr. Marcus Chen",
            lecturerTitle = "Data Modeling Lab",
            title = "Normalization & Keys Gauntlet",
            topic = "Normalization",
            difficulty = "Intermediate",
            visibility = "public",
            timePerQuestionSec = 15L,
            passThresholdPercent = 60L,
            xpReward = 260L,
            questions = listOf(
                QuizQuestion(
                    prompt = "First Normal Form (1NF) strictly requires that every column contains:",
                    options = listOf("Only integers", "Atomic (indivisible) values", "Foreign keys", "No NULLs"),
                    correctIndex = 1L,
                    explanation = "1NF eliminates repeating groups and multi-valued attributes so each cell holds an atomic value."
                ),
                QuizQuestion(
                    prompt = "Second Normal Form (2NF) eliminates which kind of dependency?",
                    options = listOf("Transitive dependency", "Partial dependency on a composite PK", "Reflexive dependency", "Multi-valued dependency"),
                    correctIndex = 1L,
                    explanation = "In 2NF, every non-key attribute must depend on the ENTIRE composite primary key, not just part of it."
                ),
                QuizQuestion(
                    prompt = "Third Normal Form (3NF) removes which anomaly source?",
                    options = listOf("Transitive dependencies (non-key -> non-key)", "Repeating columns", "Duplicate primary keys", "Missing indexes"),
                    correctIndex = 0L,
                    explanation = "3NF requires no non-prime attribute to transitively depend on a candidate key."
                ),
                QuizQuestion(
                    prompt = "How do you represent a Many-to-Many (M:N) relationship in a relational database?",
                    options = listOf("Store comma-separated IDs", "Add a junction/bridge table with two FKs", "Merge both tables into one", "Use a CHECK constraint"),
                    correctIndex = 1L,
                    explanation = "An associative (junction) table breaks M:N into two clean 1:N relationships."
                ),
                QuizQuestion(
                    prompt = "A foreign key enforces which database integrity rule?",
                    options = listOf("Entity integrity", "Domain integrity", "Referential integrity", "User-defined integrity"),
                    correctIndex = 2L,
                    explanation = "Referential integrity ensures a foreign key value always points to an existing primary key or is NULL."
                )
            )
        ),
        LecturerQuiz(
            id = "starter_acid_patel",
            creatorId = "lecturer_patel",
            lecturerName = "Prof. Aisha Patel",
            lecturerTitle = "Distributed Databases",
            title = "ACID & Transactions Sprint",
            topic = "Transactions",
            difficulty = "Intermediate",
            visibility = "public",
            timePerQuestionSec = 14L,
            passThresholdPercent = 60L,
            xpReward = 280L,
            questions = listOf(
                QuizQuestion(
                    prompt = "Which ACID property guarantees a transaction either completes fully or rolls back completely?",
                    options = listOf("Consistency", "Isolation", "Atomicity", "Durability"),
                    correctIndex = 2L,
                    explanation = "Atomicity means 'all or nothing' execution for every operation inside a transaction."
                ),
                QuizQuestion(
                    prompt = "What does Durability ensure once a COMMIT succeeds?",
                    options = listOf("Zero lock contention", "Changes survive power loss or crashes", "Queries run in O(1)", "Schema never changes"),
                    correctIndex = 1L,
                    explanation = "Durability persists committed state to non-volatile storage (often via Write-Ahead Logging)."
                ),
                QuizQuestion(
                    prompt = "Reading uncommitted changes from another concurrent transaction that later rolls back is called:",
                    options = listOf("Phantom read", "Dirty read", "Deadlock", "Lost update"),
                    correctIndex = 1L,
                    explanation = "A dirty read occurs when transaction T2 reads data modified by T1 before T1 commits."
                ),
                QuizQuestion(
                    prompt = "Which SQL isolation level is the strictest and prevents dirty, non-repeatable, and phantom reads?",
                    options = listOf("READ UNCOMMITTED", "READ COMMITTED", "REPEATABLE READ", "SERIALIZABLE"),
                    correctIndex = 3L,
                    explanation = "SERIALIZABLE ensures concurrent transactions yield the same result as some serial execution."
                )
            )
        ),
        LecturerQuiz(
            id = "starter_indexing_oconnor",
            creatorId = "lecturer_oconnor",
            lecturerName = "Dr. Liam O'Connor",
            lecturerTitle = "Query Optimization",
            title = "Indexing & Performance Rush",
            topic = "Indexing",
            difficulty = "Advanced",
            visibility = "public",
            timePerQuestionSec = 12L,
            passThresholdPercent = 75L,
            xpReward = 350L,
            questions = listOf(
                QuizQuestion(
                    prompt = "What data structure do most relational databases use for default B-Tree indexes?",
                    options = listOf("Balanced multi-way search tree (B+ Tree)", "Singly linked list", "Binary heap", "Adjacency matrix"),
                    correctIndex = 0L,
                    explanation = "B+ Trees keep sorted keys in leaf nodes linked sequentially for fast lookups and range scans."
                ),
                QuizQuestion(
                    prompt = "What is the main trade-off of adding many indexes to a write-heavy table?",
                    options = listOf("SELECT becomes slower", "INSERT/UPDATE/DELETE become slower", "Foreign keys stop working", "Rows are deleted"),
                    correctIndex = 1L,
                    explanation = "Every write must update the table AND keep all associated index structures synchronized."
                ),
                QuizQuestion(
                    prompt = "A composite index on (last_name, first_name) best accelerates which WHERE clause?",
                    options = listOf("WHERE first_name = 'Ada'", "WHERE last_name = 'Lovelace'", "WHERE age > 30", "WHERE last_name LIKE '%lace'"),
                    correctIndex = 1L,
                    explanation = "Due to the leftmost-prefix rule, an index on (last_name, first_name) directly speeds up queries filtering on last_name."
                ),
                QuizQuestion(
                    prompt = "Which index type is optimal ONLY for exact equality checks (=), not range queries (> or BETWEEN)?",
                    options = listOf("B+ Tree index", "Hash index", "Clustered index", "Bitmap range index"),
                    correctIndex = 1L,
                    explanation = "Hash indexes provide O(1) equality lookups but do not preserve key ordering for range scans."
                )
            )
        )
    )
}

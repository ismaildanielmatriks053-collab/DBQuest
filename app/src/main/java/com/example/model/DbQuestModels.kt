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
            id = "starter_sql_ddl_haida",
            creatorId = "lecturer_haida",
            lecturerName = "Madam Haida",
            lecturerTitle = "SQL & Database Lecturer",
            title = "SQL Essentials & DDL Blitz",
            topic = "SQL & DDL",
            difficulty = "Beginner",
            visibility = "public",
            timePerQuestionSec = 15L,
            passThresholdPercent = 60L,
            xpReward = 220L,
            questions = listOf(
                QuizQuestion(
                    prompt = "What does DDL stand for in SQL?",
                    options = listOf(
                        "Data Definition Language",
                        "Data Deletion Logic",
                        "Database Direct Link",
                        "Dynamic Data Layer"
                    ),
                    correctIndex = 0L,
                    explanation = "DDL stands for Data Definition Language, which defines and modifies database structures such as tables, schemas, and indexes."
                ),
                QuizQuestion(
                    prompt = "Which set lists the main DDL commands used to define or modify database structures?",
                    options = listOf(
                        "SELECT, INSERT, UPDATE",
                        "CREATE, ALTER, DROP, TRUNCATE",
                        "GRANT, REVOKE, DENY",
                        "COMMIT, ROLLBACK, SAVEPOINT"
                    ),
                    correctIndex = 1L,
                    explanation = "CREATE, ALTER, DROP, and TRUNCATE are the primary DDL commands for creating, changing, and removing tables or schemas."
                ),
                QuizQuestion(
                    prompt = "Which DDL command adds a new column to an existing SQL table?",
                    options = listOf(
                        "UPDATE TABLE",
                        "MODIFY COLUMN",
                        "ALTER TABLE",
                        "INSERT COLUMN"
                    ),
                    correctIndex = 2L,
                    explanation = "ALTER TABLE updates an existing table's schema, such as adding, renaming, or removing columns and constraints."
                ),
                QuizQuestion(
                    prompt = "What is the key difference between DROP TABLE and TRUNCATE TABLE?",
                    options = listOf(
                        "DROP removes both table structure and data; TRUNCATE deletes all rows but keeps the table",
                        "TRUNCATE deletes the whole database; DROP deletes one row",
                        "DROP only works on views; TRUNCATE only works on indexes",
                        "They perform the exact same action"
                    ),
                    correctIndex = 0L,
                    explanation = "DROP TABLE permanently removes the table definition and its data, whereas TRUNCATE TABLE removes all rows while preserving the table structure."
                ),
                QuizQuestion(
                    prompt = "Which SQL category handles querying and modifying table rows (SELECT, INSERT, UPDATE, DELETE)?",
                    options = listOf(
                        "DDL (Data Definition Language)",
                        "DCL (Data Control Language)",
                        "DML (Data Manipulation Language)",
                        "TCL (Transaction Control Language)"
                    ),
                    correctIndex = 2L,
                    explanation = "DML (Data Manipulation Language) is used to retrieve and manipulate data stored inside existing tables."
                )
            )
        )
    )
}

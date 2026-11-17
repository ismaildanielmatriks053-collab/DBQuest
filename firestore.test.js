const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_lecturer_123";
const BOB_UID = "bob_player_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Alice SQL",
    totalPoints: 0,
    quizzesCompleted: 0,
    quizzesWon: 0,
    diagramsSolved: 0,
    currentStreak: 0,
    bestStreak: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validQuiz(creatorId, ts) {
  return {
    creatorId,
    lecturerName: "Prof. Elena Vance",
    lecturerTitle: "Database Architecture",
    title: "Relational Joins Blitz",
    topic: "SQL Joins",
    difficulty: "Intermediate",
    visibility: "public",
    timePerQuestionSec: 15,
    passThresholdPercent: 60,
    xpReward: 200,
    questions: [
      {
        prompt: "Which join returns all rows from the left table and matched rows from the right?",
        options: ["INNER JOIN", "LEFT JOIN", "CROSS JOIN", "SELF JOIN"],
        correctIndex: 1,
        explanation: "LEFT JOIN preserves every row from the left table.",
      },
    ],
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("quizzes").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, ts)));
  await assertFails(unauthDb.collection("quizzes").doc("q1").set(validQuiz(ALICE_UID, ts)));
});

test("User profile: owner create, partial update, cross-user denial, and ghost field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, ts)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Valid partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      totalPoints: 250,
      quizzesCompleted: 1,
      quizzesWon: 1,
      currentStreak: 1,
      bestStreak: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ totalPoints: 9999, updatedAt: ts }));
});

test("Lecturer quizzes: creator can publish, others can list/play public quizzes, non-creator cannot edit or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(aliceDb.collection("quizzes").doc("quiz_joins_1").set(validQuiz(ALICE_UID, ts)));

  // Bob can list with visibility == 'public' filter (matches Kotlin QuizRepository)
  await assertSucceeds(bobDb.collection("quizzes").where("visibility", "==", "public").get());
  await assertSucceeds(bobDb.collection("quizzes").doc("quiz_joins_1").get());

  // Unfiltered list query on top-level collection fails
  await assertFails(bobDb.collection("quizzes").get());

  // Bob cannot update or delete Alice's quiz
  await assertFails(
    bobDb.collection("quizzes").doc("quiz_joins_1").update({
      title: "Hijacked Quiz",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("quizzes").doc("quiz_joins_1").delete());

  // Alice can update her own quiz
  await assertSucceeds(
    aliceDb.collection("quizzes").doc("quiz_joins_1").update({
      title: "Relational Joins Mastery",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId 'id' leakage
  await assertFails(
    aliceDb.collection("quizzes").doc("quiz_bad_id").set({
      ...validQuiz(ALICE_UID, ts),
      id: "quiz_bad_id",
    })
  );
});

test("User quizResults and diagramAttempts: owner-scoped subcollection rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quizResults").doc("res_1").set({
      userId: ALICE_UID,
      quizId: "quiz_joins_1",
      quizTitle: "Relational Joins Blitz",
      lecturerName: "Prof. Elena Vance",
      score: 450,
      correctCount: 4,
      totalQuestions: 5,
      accuracyPercent: 80,
      outcome: "WIN",
      pointsEarned: 250,
      maxStreak: 3,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("diagramAttempts").doc("diag_1").set({
      userId: ALICE_UID,
      challengeId: "challenge_university",
      challengeTitle: "University Enrollment ER",
      verdictTitle: "Solid 3NF Schema!",
      feedbackSummary: "Entities and junction table correctly normalized.",
      score: 92,
      passed: true,
      xpAwarded: 180,
      createdAt: ts,
    })
  );

  // List subcollections as owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("quizResults").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("diagramAttempts").get());

  // Cross-user access denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("quizResults").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("diagramAttempts").get());

  // Reject future tampered timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("diagramAttempts").doc("diag_future").set({
      userId: ALICE_UID,
      challengeId: "challenge_university",
      challengeTitle: "Future Test",
      verdictTitle: "Invalid",
      feedbackSummary: "Future timestamp should fail.",
      score: 50,
      passed: false,
      xpAwarded: 0,
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

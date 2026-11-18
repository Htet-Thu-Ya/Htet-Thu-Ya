const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_cust_1";
const BOB_UID = "bob_cust_2";
const FRIEND_UID = "friend_admin_3";
const OWNER_UID = "owner_admin_0";
const OWNER_EMAIL = "onimusha.033310@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("matches").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User registration: 0 Ks / 0 pts starter succeeds, self-minting or ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate();

  // Valid Kotlin create payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-minting non-zero balance (e.g. 10000 Ks)
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      balanceKs: 10000,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-minting non-zero points
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      balanceKs: 0,
      rewardPoints: 100,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject Ghost field (Shadow Update test)
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User isolation & PII protection: Bob cannot read or list Alice's wallet", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Customer order placement & debit conservation: valid order succeeds, fake status or payout fails", async () => {
  const created = pastDate(10000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Simulate admin top-up credit so Alice can place an order
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      balanceKs: 10000,
      updatedAt: pastDate(5000),
    });
  });

  // Valid balanceKs -> activeStakeKs debit
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balanceKs: 8000,
      activeStakeKs: 2000,
      updatedAt: pastDate(),
    })
  );

  // Reject invalid inflation of balanceKs by customer
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balanceKs: 50000,
      activeStakeKs: 2000,
      updatedAt: pastDate(),
    })
  );

  // Valid Kotlin create order payload
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "body_goal",
      selections: ["Arsenal (Home -0.5) vs Chelsea"],
      matchIds: ["sample_match_1"],
      drawRound: "ဘော်ဒီ/ဂိုးပေါင်း",
      stakeKs: 2000,
      multiplier: 1.92,
      potentialPayoutKs: 3840,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Customer cannot self-approve order or mark won
  await assertFails(
    aliceDb.collection("orders").doc("ord_1").update({
      status: "won",
      actualPayoutKs: 3840,
      updatedAt: pastDate(),
    })
  );

  // Client filtered query succeeds; unfiltered query fails for non-admin
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("orders").get());
});

test("Email spoofing defense & Bootstrapped Admin + Invite Code sharing", async () => {
  // Spoofed unverified admin email MUST fail
  const spoofDb = testEnv.authenticatedContext("spoof_1", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("admin_invites").doc("HTY-FAKE11").set({
      code: "HTY-FAKE11",
      label: "Spoof",
      active: true,
      createdBy: "spoof_1",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Verified owner email succeeds
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    ownerDb.collection("admin_invites").doc("HTY-FRIEND1").set({
      code: "HTY-FRIEND1",
      label: "Ko Mg",
      active: true,
      createdBy: OWNER_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Random user without valid invite code cannot become admin
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("admins").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      role: "admin",
      addedBy: BOB_UID,
      inviteCodeUsed: "NOPE-999",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Friend with valid active invite code can join admins and then list all orders
  const friendDb = testEnv.authenticatedContext(FRIEND_UID).firestore();
  await assertSucceeds(
    friendDb.collection("admins").doc(FRIEND_UID).set({
      userId: FRIEND_UID,
      displayName: "Ko Mg",
      role: "admin",
      addedBy: OWNER_UID,
      inviteCodeUsed: "HTY-FRIEND1",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(friendDb.collection("orders").get());
});

test("Public matches & 3D draws: customer filtered visibility query succeeds, hidden draft blocked", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    ownerDb.collection("matches").doc("m_public").set({
      league: "Premier League",
      homeTeam: "Arsenal",
      awayTeam: "Chelsea",
      kickoffLabel: "Tonight 21:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.92,
      goalOdds: 1.90,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      createdBy: OWNER_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    ownerDb.collection("matches").doc("m_hidden").set({
      league: "Draft League",
      homeTeam: "Team A",
      awayTeam: "Team B",
      kickoffLabel: "TBD",
      handicapLine: "0",
      goalLine: "2.5",
      bodyOdds: 1.9,
      goalOdds: 1.9,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "hidden",
      createdBy: OWNER_UID,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("matches").where("visibility", "==", "public").get());
  await assertFails(aliceDb.collection("matches").doc("m_hidden").get());
  await assertFails(aliceDb.collection("matches").get());
});

test("Point exchange & future timestamp rejection", async () => {
  const created = pastDate(10000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Simulate admin-settled credit/points before point exchange
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      balanceKs: 10000,
      rewardPoints: 100,
      updatedAt: pastDate(5000),
    });
  });

  // Valid point exchange: 20 pts -> +200 Ks
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balanceKs: 10200,
      rewardPoints: 80,
      updatedAt: pastDate(),
    })
  );

  // Reject invalid exchange ratio (10 pts -> +99999 Ks)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      balanceKs: 110199,
      rewardPoints: 70,
      updatedAt: pastDate(),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("orders").doc("ord_future").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "three_d",
      selections: ["3D: 555 (500 Ks)"],
      matchIds: [],
      drawRound: "3D Round",
      stakeKs: 500,
      multiplier: 550.0,
      potentialPayoutKs: 275000,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Admin match editing/deletion and processed wallet request deletion", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    ownerDb.collection("matches").doc("m_edit").set({
      league: "Premier League",
      homeTeam: "A",
      awayTeam: "B",
      kickoffLabel: "Tonight 21:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.92,
      goalOdds: 1.90,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Admin edits full match details
  await assertSucceeds(
    ownerDb.collection("matches").doc("m_edit").update({
      league: "La Liga",
      homeTeam: "Real Madrid",
      awayTeam: "Barcelona",
      kickoffLabel: "02:00 AM",
      handicapLine: "Home -1.0",
      goalLine: "3.0",
      bodyOdds: 1.95,
      goalOdds: 1.88,
      status: "finished",
      homeScore: 2,
      awayScore: 1,
      updatedAt: pastDate(),
    })
  );

  // Non-admin cannot edit or delete match
  await assertFails(aliceDb.collection("matches").doc("m_edit").delete());

  // Admin can delete old match
  await assertSucceeds(ownerDb.collection("matches").doc("m_edit").delete());

  // Wallet requests: pending cannot be deleted, processed (approved/rejected) can be deleted by admin only
  await assertSucceeds(
    aliceDb.collection("wallet_requests").doc("wr_pending").set({
      userId: ALICE_UID,
      customerName: "Alice",
      requestType: "topup",
      amountKs: 5000,
      channel: "KBZPay",
      referenceNote: "123456",
      status: "pending",
      adminNote: "",
      processedBy: "",
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  await assertFails(ownerDb.collection("wallet_requests").doc("wr_pending").delete());

  await assertSucceeds(
    ownerDb.collection("wallet_requests").doc("wr_pending").update({
      status: "approved",
      adminNote: "Approved",
      processedBy: OWNER_UID,
      updatedAt: pastDate(),
    })
  );

  await assertFails(aliceDb.collection("wallet_requests").doc("wr_pending").delete());
  await assertSucceeds(ownerDb.collection("wallet_requests").doc("wr_pending").delete());
});

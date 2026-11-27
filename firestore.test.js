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
      win1Odds: 3.38,
      drawOdds: 3.74,
      win2Odds: 1.975,
      overOdds: 1.88,
      underOdds: 1.94,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Invalid out-of-range 1xBET odds must fail
  await assertFails(
    ownerDb.collection("matches").doc("m_edit_bad").set({
      league: "Premier League",
      homeTeam: "A",
      awayTeam: "B",
      kickoffLabel: "Tonight 21:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.92,
      goalOdds: 1.90,
      win1Odds: 0.5,
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
      win1Odds: 2.15,
      drawOdds: 3.40,
      win2Odds: 3.10,
      overOdds: 1.85,
      underOdds: 1.95,
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

  // Pending wallet requests cannot be deleted by customer or second admin, while Main Admin can manage/edit/delete
  await assertFails(aliceDb.collection("wallet_requests").doc("wr_pending").delete());

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

test("User email registration, gift points, completed order deletion, and app settings config", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice creates account with Gmail email
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@gmail.com",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Admin can list users and see Alice's email, Bob cannot
  await assertSucceeds(ownerDb.collection("users").get());
  await assertFails(bobDb.collection("users").get());

  // Admin can gift reward points & write point_gift transaction
  await assertSucceeds(
    ownerDb.collection("users").doc(ALICE_UID).update({
      rewardPoints: 150,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("transactions").doc("tx_gift_1").set({
      userId: ALICE_UID,
      txType: "point_gift",
      direction: "credit",
      amountKs: 0,
      title: "Admin လက်ဆောင်ပွိုင့် +150 Pts",
      balanceAfterKs: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Only Admin can configure wallet_config and board_config; signed-in customers can read
  await assertFails(
    aliceDb.collection("app_settings").doc("wallet_config").set({
      depositPercent: 10.0,
      depositMode: "bonus",
      withdrawPercent: 2.0,
      withdrawMode: "fee",
      updatedBy: ALICE_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("wallet_config").set({
      depositPercent: 5.0,
      depositMode: "bonus",
      withdrawPercent: 2.0,
      withdrawMode: "fee",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("board_config").set({
      autoSyncDailyMatches: true,
      autoSyncLiveResults: true,
      lastSyncedDate: "2026-11-19",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(aliceDb.collection("app_settings").doc("wallet_config").get());

  // Completed orders (won/lost/rejected) can be deleted by owner or Admin; pending orders cannot
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("orders").doc("ord_pending_del").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "body_goal",
      selections: ["Arsenal"],
      matchIds: ["m1"],
      drawRound: "Round 1",
      stakeKs: 1000,
      multiplier: 1.9,
      potentialPayoutKs: 1900,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
    await db.collection("orders").doc("ord_won_del").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "body_goal",
      selections: ["Arsenal"],
      matchIds: ["m1"],
      drawRound: "Round 1",
      stakeKs: 1000,
      multiplier: 1.9,
      potentialPayoutKs: 1900,
      actualPayoutKs: 1900,
      status: "won",
      adminNote: "Won",
      settledBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
  });

  await assertFails(aliceDb.collection("orders").doc("ord_pending_del").delete());
  await assertFails(bobDb.collection("orders").doc("ord_won_del").delete());
  await assertSucceeds(aliceDb.collection("orders").doc("ord_won_del").delete());
  await assertSucceeds(ownerDb.collection("orders").doc("ord_pending_del").delete());
});

test("Betting commission config, order pickKeys, and commission settlement", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();

  // Only Admin can configure app_settings/betting_config
  await assertFails(
    aliceDb.collection("app_settings").doc("betting_config").set({
      winCommissionPercent: 5.0,
      winCommissionBasis: "payout",
      autoSettleOrders: true,
      updatedBy: ALICE_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("betting_config").set({
      winCommissionPercent: 5.0,
      winCommissionBasis: "payout",
      autoSettleOrders: true,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(aliceDb.collection("app_settings").doc("betting_config").get());

  // Customer creates order with pickKeys
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_comm_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "body_goal",
      selections: ["Arsenal (Home -0.5) vs Chelsea"],
      matchIds: ["m1"],
      pickKeys: ["home"],
      drawRound: "ဘော်ဒီ/ဂိုးပေါင်း",
      stakeKs: 10000,
      multiplier: 1.9,
      potentialPayoutKs: 19000,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );

  // Customer cannot self-settle with commission
  await assertFails(
    aliceDb.collection("orders").doc("ord_comm_1").update({
      status: "won",
      actualPayoutKs: 18050,
      commissionPercent: 5.0,
      commissionKs: 950,
      updatedAt: pastDate(),
    })
  );

  // Admin settles order as won with 5% commission deduction
  await assertSucceeds(
    ownerDb.collection("orders").doc("ord_comm_1").update({
      status: "won",
      actualPayoutKs: 18050,
      commissionPercent: 5.0,
      commissionKs: 950,
      adminNote: "အနိုင်ရ (အကောက် 5% နှုတ်ပြီး)",
      settledBy: OWNER_UID,
      updatedAt: pastDate(),
    })
  );
});

test("APK update config, 1-on-1 direct chat, group chat, and quality rating rules", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();

  // 1. app_update config: only Admin writes, Customer can read
  await assertFails(
    aliceDb.collection("app_settings").doc("app_update").set({
      latestVersionCode: 3,
      latestVersionName: "2.1",
      downloadUrl: "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/HtetThuYa.apk",
      releaseNotes: "Spoof",
      forceUpdate: false,
      updatedBy: ALICE_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("app_update").set({
      latestVersionCode: 3,
      latestVersionName: "2.1",
      downloadUrl: "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/HtetThuYa.apk",
      releaseNotes: "Chatbox & Quality Update",
      forceUpdate: false,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(aliceDb.collection("app_settings").doc("app_update").get());

  // 2. Direct 1-on-1 chat isolation
  await assertSucceeds(
    aliceDb.collection("direct_messages").doc("dm_1").set({
      threadUserId: ALICE_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRole: "customer",
      text: "Hello Admin!",
      createdAt: pastDate(2000),
    })
  );
  // Customer cannot impersonate admin role or write into Bob's thread
  await assertFails(
    aliceDb.collection("direct_messages").doc("dm_spoof").set({
      threadUserId: ALICE_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRole: "admin",
      text: "Fake Admin",
      createdAt: pastDate(1500),
    })
  );
  await assertFails(
    bobDb.collection("direct_messages").doc("dm_cross").set({
      threadUserId: ALICE_UID,
      senderId: BOB_UID,
      senderName: "Bob",
      senderRole: "customer",
      text: "Hi Alice",
      createdAt: pastDate(1500),
    })
  );
  // Admin can reply to Alice's thread & read all; Bob cannot read Alice's DM
  await assertSucceeds(
    ownerDb.collection("direct_messages").doc("dm_2").set({
      threadUserId: ALICE_UID,
      senderId: OWNER_UID,
      senderName: "Admin",
      senderRole: "admin",
      text: "Hello Alice!",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(aliceDb.collection("direct_messages").where("threadUserId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("direct_messages").where("threadUserId", "==", ALICE_UID).get());
  await assertSucceeds(ownerDb.collection("direct_messages").get());

  // 3. Group chat rules
  await assertSucceeds(
    aliceDb.collection("group_messages").doc("gm_1").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRole: "customer",
      text: "မင်္ဂလာပါ အားလုံး!",
      createdAt: pastDate(2000),
    })
  );
  await assertFails(
    aliceDb.collection("group_messages").doc("gm_spoof").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      senderRole: "admin",
      text: "I am admin",
      createdAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    ownerDb.collection("group_messages").doc("gm_2").set({
      senderId: OWNER_UID,
      senderName: "Admin",
      senderRole: "admin",
      text: "Welcome!",
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(bobDb.collection("group_messages").get());

  // 4. Quality reviews (1-5 stars, self-owned)
  await assertFails(
    aliceDb.collection("app_reviews").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      stars: 6,
      qualityTier: "Invalid",
      comment: "Out of range",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("app_reviews").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      stars: 5,
      qualityTier: "အထူးကောင်းမွန်",
      comment: "အရမ်းကောင်းပါတယ်",
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    bobDb.collection("app_reviews").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      stars: 1,
      qualityTier: "အားနည်း",
      comment: "Tamper",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(bobDb.collection("app_reviews").get());
});

test("Voice & Video call signaling: participant isolation, admin call, SDP answer, and ICE candidates", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@gmail.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@gmail.com",
    email_verified: true,
  }).firestore();

  // Customer Alice initiates a video call to Admin
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_1").set({
      threadUserId: ALICE_UID,
      callerId: ALICE_UID,
      callerName: "Alice",
      callerRole: "customer",
      calleeName: "Admin",
      callType: "video",
      status: "ringing",
      offerSdp: "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\n",
      answerSdp: "",
      answeredBy: "",
      answeredByName: "",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Customer cannot spoof callerRole == 'admin' or start call inside Bob's thread
  await assertFails(
    aliceDb.collection("call_sessions").doc("call_spoof").set({
      threadUserId: ALICE_UID,
      callerId: ALICE_UID,
      callerName: "Alice",
      callerRole: "admin",
      calleeName: "Bob",
      callType: "voice",
      status: "ringing",
      offerSdp: "v=0",
      answerSdp: "",
      answeredBy: "",
      answeredByName: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );
  await assertFails(
    bobDb.collection("call_sessions").doc("call_cross").set({
      threadUserId: ALICE_UID,
      callerId: BOB_UID,
      callerName: "Bob",
      callerRole: "customer",
      calleeName: "Alice",
      callType: "voice",
      status: "ringing",
      offerSdp: "v=0",
      answerSdp: "",
      answeredBy: "",
      answeredByName: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Bob cannot read Alice's call session or add ICE candidates to it
  await assertFails(bobDb.collection("call_sessions").doc("call_1").get());
  await assertFails(bobDb.collection("call_sessions").where("threadUserId", "==", ALICE_UID).get());
  await assertFails(
    bobDb.collection("call_sessions").doc("call_1").collection("candidates").doc("cand_bob").set({
      senderId: BOB_UID,
      sdpMid: "0",
      sdpMLineIndex: 0,
      candidate: "candidate:1 1 UDP 2122252543 192.168.1.1 50000 typ host",
      createdAt: pastDate(1000),
    })
  );

  // Alice and Admin can query call_1 and exchange ICE candidates
  await assertSucceeds(aliceDb.collection("call_sessions").where("threadUserId", "==", ALICE_UID).get());
  await assertSucceeds(ownerDb.collection("call_sessions").get());
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_1").collection("candidates").doc("cand_a1").set({
      senderId: ALICE_UID,
      sdpMid: "0",
      sdpMLineIndex: 0,
      candidate: "candidate:1 1 UDP 2122252543 192.168.1.2 50001 typ host",
      createdAt: pastDate(1500),
    })
  );

  // Admin answers Alice's call with answerSdp
  await assertSucceeds(
    ownerDb.collection("call_sessions").doc("call_1").update({
      status: "accepted",
      answerSdp: "v=0\r\no=- 67890 2 IN IP4 127.0.0.1\r\n",
      answeredBy: OWNER_UID,
      answeredByName: "Admin",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("call_sessions").doc("call_1").collection("candidates").doc("cand_o1").set({
      senderId: OWNER_UID,
      sdpMid: "0",
      sdpMLineIndex: 0,
      candidate: "candidate:2 1 UDP 2122252543 192.168.1.3 50002 typ host",
      createdAt: pastDate(900),
    })
  );
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_1").collection("candidates").get()
  );

  // Alice or Admin can end the call
  await assertSucceeds(
    aliceDb.collection("call_sessions").doc("call_1").update({
      status: "ended",
      updatedAt: pastDate(500),
    })
  );
});

test("Main Admin vs Second Admin RBAC permissions, suspension, and invite defaultPermissions enforcement", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const secondDb = testEnv.authenticatedContext("sub_admin_1", {
    email: "subadmin@gmail.com",
    email_verified: true,
  }).firestore();

  const walletOnlyPerms = {
    canApproveWallet: true,
    canManageOrders: false,
    canViewUsers: true,
    canLockUsers: false,
    canManageBoard: false,
    canChat: true,
    canEditWalletRates: false,
    canEditGameConfig: false,
    canViewAllLogs: false,
    canAdjustBalance: false,
  };

  // Owner creates scoped invite with defaultPermissions
  await assertSucceeds(
    ownerDb.collection("admin_invites").doc("SUB-WALLET1").set({
      code: "SUB-WALLET1",
      label: "Wallet Cashier",
      active: true,
      assignedEmail: "subadmin@gmail.com",
      defaultPermissions: walletOnlyPerms,
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  // Sub-admin CANNOT escalate permissions on self-create beyond defaultPermissions
  await assertFails(
    secondDb.collection("admins").doc("sub_admin_1").set({
      userId: "sub_admin_1",
      displayName: "Cashier",
      email: "subadmin@gmail.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "SUB-WALLET1",
      permissions: { ...walletOnlyPerms, canManageOrders: true },
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );

  // Sub-admin succeeds when permissions match defaultPermissions
  await assertSucceeds(
    secondDb.collection("admins").doc("sub_admin_1").set({
      userId: "sub_admin_1",
      displayName: "Cashier",
      email: "subadmin@gmail.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "SUB-WALLET1",
      permissions: walletOnlyPerms,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );

  // Sub-admin CANNOT read or modify security_config or create payment_accounts or update orders
  await assertFails(
    secondDb.collection("app_settings").doc("security_config").set({
      requireAdminPin: false,
      ipWhitelistEnabled: false,
      allowedIps: [],
      globalForceLogoutAtMs: 0,
      updatedBy: "sub_admin_1",
      updatedAt: pastDate(3000),
    })
  );
  await assertFails(
    secondDb.collection("payment_accounts").doc("kpay_1").set({
      providerName: "KBZPay",
      accountName: "Cashier",
      accountNumber: "099999999",
      note: "",
      active: true,
      createdBy: "sub_admin_1",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Owner creates a pending order & a match
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("orders").doc("ord_rbac_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "two_d",
      selections: ["45"],
      matchIds: [],
      drawRound: "2D Morning",
      stakeKs: 1000,
      multiplier: 80.0,
      potentialPayoutKs: 80000,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    });
  });

  // Sub-admin without canManageOrders CANNOT update order
  await assertFails(
    secondDb.collection("orders").doc("ord_rbac_1").update({
      status: "accepted",
      adminNote: "OK",
      settledBy: "sub_admin_1",
      updatedAt: pastDate(2000),
    })
  );

  // Owner suspends sub_admin_1 -> all admin actions denied immediately
  await assertSucceeds(
    ownerDb.collection("admins").doc("sub_admin_1").update({
      status: "suspended",
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(secondDb.collection("orders").get());
});

test("Dual approval wallet flow, payment accounts, immutable audit logs, and system/security configs", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const secondDb = testEnv.authenticatedContext("sub_admin_2", {
    email: "sub2@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Seed active Second Admin with canApproveWallet=true and a large pending withdrawal
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc("sub_admin_2").set({
      userId: "sub_admin_2",
      displayName: "Sub Wallet",
      email: "sub2@gmail.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "CODE-1",
      permissions: { canApproveWallet: true, canChat: true, canViewUsers: true },
      createdAt: pastDate(9000),
      updatedAt: pastDate(9000),
    });
    await db.collection("wallet_requests").doc("wr_big_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      requestType: "withdraw",
      amountKs: 500000,
      channel: "KBZPay",
      referenceNote: "091234567",
      status: "pending",
      adminNote: "",
      processedBy: "",
      createdAt: pastDate(8000),
      updatedAt: pastDate(8000),
    });
  });

  // Second admin moves large withdraw from pending -> awaiting_main_approval
  await assertSucceeds(
    secondDb.collection("wallet_requests").doc("wr_big_1").update({
      status: "awaiting_main_approval",
      adminNote: "Needs Owner review",
      processedBy: "sub_admin_2",
      updatedAt: pastDate(7000),
    })
  );

  // Second admin CANNOT finalize awaiting_main_approval -> approved
  await assertFails(
    secondDb.collection("wallet_requests").doc("wr_big_1").update({
      status: "approved",
      adminNote: "Self override",
      processedBy: "sub_admin_2",
      updatedAt: pastDate(6000),
    })
  );

  // Owner CAN finalize awaiting_main_approval -> approved
  await assertSucceeds(
    ownerDb.collection("wallet_requests").doc("wr_big_1").update({
      status: "approved",
      adminNote: "Owner Approved",
      processedBy: OWNER_UID,
      updatedAt: pastDate(5000),
    })
  );

  // Payment accounts: Owner creates, customer reads active only, second admin cannot edit
  await assertSucceeds(
    ownerDb.collection("payment_accounts").doc("pay_kpay").set({
      providerName: "KBZPay",
      accountName: "LoTaYa Official",
      accountNumber: "09777888999",
      note: "Main KPay",
      active: true,
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
  await assertSucceeds(aliceDb.collection("payment_accounts").where("active", "==", true).get());
  await assertFails(aliceDb.collection("payment_accounts").get());
  await assertFails(secondDb.collection("payment_accounts").doc("pay_kpay").delete());

  // Audit logs: sub_admin_2 writes own log, cannot view/delete Owner logs, can delete own personal logs
  await assertSucceeds(
    ownerDb.collection("audit_logs").doc("log_owner_1").set({
      actorId: OWNER_UID,
      actorName: "Main Admin",
      actorRole: "owner",
      action: "Approved big withdrawal",
      category: "wallet",
      targetId: "wr_big_1",
      targetName: "Alice",
      details: "500,000 Ks",
      createdAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    secondDb.collection("audit_logs").doc("log_sub_1").set({
      actorId: "sub_admin_2",
      actorName: "Sub Wallet",
      actorRole: "admin",
      action: "Escalated withdrawal",
      category: "wallet",
      targetId: "wr_big_1",
      targetName: "Alice",
      details: "500,000 Ks",
      createdAt: pastDate(3500),
    })
  );
  await assertSucceeds(
    secondDb.collection("audit_logs").doc("log_sub_2").set({
      actorId: "sub_admin_2",
      actorName: "Sub Wallet",
      actorRole: "admin",
      action: "Approved small topup",
      category: "wallet",
      targetId: "wr_small_1",
      targetName: "Alice",
      details: "5,000 Ks",
      createdAt: pastDate(3400),
    })
  );
  await assertFails(secondDb.collection("audit_logs").doc("log_owner_1").get());
  await assertFails(secondDb.collection("audit_logs").doc("log_owner_1").delete());
  await assertSucceeds(secondDb.collection("audit_logs").where("actorId", "==", "sub_admin_2").get());
  await assertSucceeds(ownerDb.collection("audit_logs").get());
  await assertSucceeds(secondDb.collection("audit_logs").doc("log_sub_1").delete());
  await assertSucceeds(ownerDb.collection("audit_logs").doc("log_sub_2").delete());

  // System & Security Configs
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("system_config").set({
      enabledGames: ["parlay", "body_goal", "two_d", "three_d", "dice", "six_animals"],
      dualApprovalThresholdKs: 300000,
      minStakeKs: 500,
      maxStakeKs: 5000000,
      twoDPayoutMultiplier: 80.0,
      threeDPayoutMultiplier: 550.0,
      dicePayoutMultiplier: 1.95,
      sixAnimalsPayoutMultiplier: 2.0,
      bonusPromoText: "Welcome Bonus",
      maintenanceMode: false,
      maintenanceMessage: "",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(3000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("security_config").set({
      requireAdminPin: true,
      ipWhitelistEnabled: false,
      allowedIps: [],
      globalForceLogoutAtMs: 0,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(3000),
    })
  );
});

test("Locked user blocked from betting/wallet, online presence update allowed, atomic mini-game round batch", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      balanceKs: 0,
      rewardPoints: 0,
      activeStakeKs: 0,
      totalWonKs: 0,
      isOnline: true,
      lastSeenAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Presence & PIN self-update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isOnline: false,
      lastSeenAt: pastDate(2000),
      securityPinHash: "abcd1234",
      updatedAt: pastDate(2000),
    })
  );

  // Credit Alice 5000 Ks then test atomic mini-game batch (stake 1000, payout 1950 -> balance +950, totalWon +1950)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      balanceKs: 5000,
      updatedAt: pastDate(1500),
    });
  });

  const roundRef = aliceDb.collection("mini_game_rounds").doc("rnd_1");
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const winTxRef = aliceDb.collection("transactions").doc("tx_rnd_1");
  const batch = aliceDb.batch();
  batch.set(roundRef, {
    userId: ALICE_UID,
    customerName: "Alice",
    gameType: "dice",
    betLabel: "ကြီး (Big)",
    outcomeLabel: "4 + 5 + 3 = 12",
    stakeKs: 1000,
    payoutKs: 1950,
    createdAt: pastDate(1000),
  });
  batch.update(userRef, {
    balanceKs: 5950,
    totalWonKs: 1950,
    lastRoundId: "rnd_1",
    updatedAt: pastDate(1000),
  });
  batch.set(winTxRef, {
    userId: ALICE_UID,
    txType: "mini_game_win",
    direction: "credit",
    amountKs: 1950,
    title: "အံစာတုံး အနိုင် (ကြီး (Big) • 4 + 5 + 3 = 12)",
    balanceAfterKs: 5950,
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Losing 6-animals 3-doc atomic batch (stake 1000, payout 0 -> balance -1000, totalWon unchanged)
  const lossBatch = aliceDb.batch();
  lossBatch.set(aliceDb.collection("mini_game_rounds").doc("rnd_2"), {
    userId: ALICE_UID,
    customerName: "Alice",
    gameType: "six_animals",
    betLabel: "သမင် (Deer)",
    outcomeLabel: "ကျား • ဆင် • ငါး",
    stakeKs: 1000,
    payoutKs: 0,
    createdAt: pastDate(900),
  });
  lossBatch.update(userRef, {
    balanceKs: 4950,
    totalWonKs: 1950,
    lastRoundId: "rnd_2",
    updatedAt: pastDate(900),
  });
  lossBatch.set(aliceDb.collection("transactions").doc("tx_rnd_2"), {
    userId: ALICE_UID,
    txType: "mini_game_stake",
    direction: "debit",
    amountKs: 1000,
    title: "၆ကောင်ဂျင် (သမင် (Deer) • ကျား • ဆင် • ငါး)",
    balanceAfterKs: 4950,
    createdAt: pastDate(900),
    updatedAt: pastDate(900),
  });
  await assertSucceeds(lossBatch.commit());

  // Fake balance win mismatch on mini-game batch MUST fail
  const fakeBatch = aliceDb.batch();
  fakeBatch.set(aliceDb.collection("mini_game_rounds").doc("rnd_fake"), {
    userId: ALICE_UID,
    customerName: "Alice",
    gameType: "dice",
    betLabel: "ကြီး (Big)",
    outcomeLabel: "1 + 1 + 1 = 3",
    stakeKs: 1000,
    payoutKs: 0,
    createdAt: pastDate(800),
  });
  fakeBatch.update(userRef, {
    balanceKs: 99999,
    totalWonKs: 1950,
    lastRoundId: "rnd_fake",
    updatedAt: pastDate(800),
  });
  await assertFails(fakeBatch.commit());

  // Lock Alice -> cannot place orders or mini-game bets
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      isLocked: true,
      updatedAt: pastDate(500),
    });
  });

  await assertFails(
    aliceDb.collection("orders").doc("ord_locked").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "two_d",
      selections: ["88"],
      matchIds: [],
      drawRound: "2D Evening",
      stakeKs: 500,
      multiplier: 80.0,
      potentialPayoutKs: 40000,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      createdAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );
});

test("Branding config, mini-game control config, auto-accept orders, support agents, Second Admin payment accounts, and Main Admin ledger edits", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const secondDb = testEnv.authenticatedContext("sub_admin_ctrl", {
    email: "subctrl@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc("sub_admin_ctrl").set({
      userId: "sub_admin_ctrl",
      displayName: "Agent Ko Lin",
      email: "subctrl@gmail.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "CODE-CTRL",
      permissions: { canApproveWallet: true, canManageOrders: true, canChat: true, canViewUsers: true },
      createdAt: pastDate(9000),
      updatedAt: pastDate(9000),
    });
  });

  // Main Admin writes branding_config & mini_game_control; Second Admin & Customer cannot write
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("branding_config").set({
      appName: "လိုတရ VIP",
      logoVariant: "crest",
      customLogoUrl: "",
      themePreset: "royal_blue",
      showRulesOnStartup: true,
      rulesText: "စည်းမျဉ်းစည်းကမ်းများ",
      maxContactMainAdminKs: 500000,
      mainAdminContactNote: "Main Admin ဆက်သွယ်ရန်",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(4000),
    })
  );
  await assertFails(
    secondDb.collection("app_settings").doc("branding_config").set({
      appName: "Hijack",
      logoVariant: "crest",
      customLogoUrl: "",
      themePreset: "emerald_gold",
      showRulesOnStartup: false,
      rulesText: "",
      maxContactMainAdminKs: 0,
      mainAdminContactNote: "",
      updatedBy: "sub_admin_ctrl",
      updatedAt: pastDate(3500),
    })
  );
  await assertSucceeds(aliceDb.collection("app_settings").doc("branding_config").get());

  await assertSucceeds(
    ownerDb.collection("app_settings").doc("mini_game_control").set({
      diceStandardMult: 2.0,
      diceTripleMult: 30.0,
      diceExactMult: 8.0,
      sixAnimalsMult: 2.5,
      diceAllowedTotals: [7, 8, 9, 10, 11],
      diceMaxBigPerDay: 20,
      diceMaxSmallPerDay: 20,
      diceMaxOddPerDay: 15,
      diceMaxEvenPerDay: 15,
      diceTotalDailyQuotas: { "10": 5 },
      animalDailyQuotas: { "tiger": 10 },
      globalDirective: "stealth_house",
      stealthHouseWinPct: 78,
      userDirectives: { [ALICE_UID]: "stealth_house" },
      updatedBy: OWNER_UID,
      updatedAt: pastDate(3500),
    })
  );
  await assertFails(
    ownerDb.collection("app_settings").doc("mini_game_control").update({
      stealthHouseWinPct: 99,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(3200),
    })
  );
  await assertFails(
    ownerDb.collection("app_settings").doc("mini_game_control").update({
      globalDirective: "rigged",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(3100),
    })
  );
  await assertFails(
    secondDb.collection("app_settings").doc("mini_game_control").update({
      globalDirective: "force_loss",
      updatedBy: "sub_admin_ctrl",
      updatedAt: pastDate(3000),
    })
  );

  // Auto-Accept Orders enabled on betting_config allows customer order creation with status == 'accepted'
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("betting_config").set({
      winCommissionPercent: 0.0,
      winCommissionBasis: "payout",
      autoSettleOrders: false,
      autoAcceptOrders: true,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(3000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_auto_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "two_d",
      selections: ["45"],
      matchIds: [],
      drawRound: "2D Evening",
      stakeKs: 1000,
      multiplier: 80.0,
      potentialPayoutKs: 80000,
      actualPayoutKs: 0,
      status: "accepted",
      adminNote: "",
      settledBy: "",
      isHidden: false,
      matchKickoffMs: 0,
      scanAtMs: 0,
      createdAt: pastDate(2500),
      updatedAt: pastDate(2500),
    })
  );

  // Second Admin can update own support_agents profile & own payment account, but NOT other admin's
  await assertSucceeds(
    secondDb.collection("support_agents").doc("sub_admin_ctrl").set({
      adminUid: "sub_admin_ctrl",
      displayName: "Agent Ko Lin",
      role: "admin",
      phone: "0912345678",
      messagingHandle: "@kolin_agent",
      paymentSummary: "KBZPay / WavePay",
      note: "24/7 Service",
      active: true,
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(aliceDb.collection("support_agents").get());

  await assertSucceeds(
    secondDb.collection("payment_accounts").doc("sub_kpay_1").set({
      providerName: "KBZPay",
      accountName: "Agent Ko Lin",
      accountNumber: "09888777666",
      note: "Second Admin KPay",
      active: true,
      createdBy: "sub_admin_ctrl",
      ownerAdminId: "sub_admin_ctrl",
      ownerAdminName: "Agent Ko Lin",
      createdAt: pastDate(1800),
      updatedAt: pastDate(1800),
    })
  );
  await assertFails(
    secondDb.collection("payment_accounts").doc("sub_spoof_pay").set({
      providerName: "WavePay",
      accountName: "Spoof",
      accountNumber: "09000000",
      note: "",
      active: true,
      createdBy: "sub_admin_ctrl",
      ownerAdminId: OWNER_UID,
      ownerAdminName: "Owner",
      createdAt: pastDate(1700),
      updatedAt: pastDate(1700),
    })
  );

  // Main Admin can edit & delete ledger transactions; Second Admin cannot
  await assertSucceeds(
    ownerDb.collection("transactions").doc("tx_edit_1").set({
      userId: ALICE_UID,
      txType: "admin_adjust",
      direction: "credit",
      amountKs: 5000,
      title: "Adjust",
      balanceAfterKs: 5000,
      isHidden: false,
      createdAt: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    ownerDb.collection("transactions").doc("tx_edit_1").update({
      title: "Corrected Adjust",
      isHidden: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    secondDb.collection("transactions").doc("tx_edit_1").delete()
  );
  await assertSucceeds(
    ownerDb.collection("transactions").doc("tx_edit_1").delete()
  );
});

test("One Express & 555 boards, orders with kickoff text, wallet receipt image create/edit/clear, and pause schedule configs", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      balanceKs: 50000,
      rewardPoints: 100,
      activeStakeKs: 0,
      totalWonKs: 0,
      isLocked: false,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  // Owner creates One Express & 555 matches + updates board_config auto-sync flags
  await assertSucceeds(
    ownerDb.collection("matches").doc("ext_oneexp_1").set({
      league: "Premier League",
      homeTeam: "Arsenal",
      awayTeam: "Liverpool",
      kickoffLabel: "Tonight 22:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.95,
      goalOdds: 1.92,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      boardSource: "one_express",
      oneExpressKyay: "Home (1-30) • 2-80",
      fiveFiveFiveKyay: "",
      kickoffEpochMs: 1700003600000,
      autoUpdatedAtMs: 1700000000000,
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("matches").doc("ext_555_1").set({
      league: "La Liga",
      homeTeam: "Real Madrid",
      awayTeam: "Barcelona",
      kickoffLabel: "Tomorrow 01:30",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.95,
      goalOdds: 1.95,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      boardSource: "five_five_five",
      oneExpressKyay: "",
      fiveFiveFiveKyay: "555: H 1-30 | G 2-80",
      kickoffEpochMs: 1700007200000,
      autoUpdatedAtMs: 1700000000000,
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
  await assertFails(
    ownerDb.collection("matches").doc("bad_source_match").set({
      league: "PL",
      homeTeam: "A",
      awayTeam: "B",
      kickoffLabel: "21:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.95,
      goalOdds: 1.95,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      boardSource: "unknown_source",
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );
  await assertFails(
    ownerDb.collection("matches").doc("bad_kickoff_epoch").set({
      league: "PL",
      homeTeam: "A",
      awayTeam: "B",
      kickoffLabel: "21:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.95,
      goalOdds: 1.95,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      kickoffEpochMs: -1,
      createdBy: OWNER_UID,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    })
  );

  await assertSucceeds(
    ownerDb.collection("app_settings").doc("board_config").set({
      autoSyncDailyMatches: true,
      autoSyncLiveResults: true,
      lastSyncedDate: "2026-11-23",
      allowedLeagues: [],
      autoSyncOneExpress: true,
      autoSyncFiveFiveFive: true,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(4500),
    })
  );
  await assertFails(
    aliceDb.collection("app_settings").doc("board_config").update({
      autoSyncOneExpress: false,
      updatedBy: ALICE_UID,
      updatedAt: pastDate(4400),
    })
  );

  await assertSucceeds(
    ownerDb.collection("app_settings").doc("system_config").set({
      enabledGames: ["parlay", "body_goal", "two_d", "three_d", "dice", "six_animals"],
      dualApprovalThresholdKs: 500000,
      minStakeKs: 500,
      maxStakeKs: 5000000,
      twoDPayoutMultiplier: 80.0,
      threeDPayoutMultiplier: 500.0,
      dicePayoutMultiplier: 1.95,
      sixAnimalsPayoutMultiplier: 2.0,
      bonusPromoText: "Bonus 5%",
      maintenanceMode: false,
      maintenanceMessage: "",
      hideTwoD: false,
      hideThreeD: false,
      footballPaused: true,
      footballReopenAtMs: Date.now() + 1800000,
      gamesPaused: true,
      gamesReopenAtMs: Date.now() + 900000,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(4000),
    })
  );

  // Customer creates One Express & 555 orders with matchKickoffText
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_oneexp_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "one_express",
      selections: ["Arsenal -0.5 [Home (1-30)]"],
      matchIds: ["ext_oneexp_1"],
      pickKeys: ["ext_oneexp_1:home"],
      drawRound: "",
      stakeKs: 5000,
      multiplier: 1.95,
      potentialPayoutKs: 9750,
      actualPayoutKs: 0,
      commissionPercent: 0,
      commissionKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      isHidden: false,
      matchKickoffMs: Date.now() + 3600000,
      matchKickoffText: "Arsenal vs Liverpool (Tonight 22:00)",
      scanAtMs: 0,
      createdAt: pastDate(3500),
      updatedAt: pastDate(3500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_555_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      orderType: "five_five_five",
      selections: ["Real Madrid Over 2.5 [555: G 2-80]"],
      matchIds: ["ext_555_1"],
      pickKeys: ["ext_555_1:over"],
      drawRound: "",
      stakeKs: 2000,
      multiplier: 1.95,
      potentialPayoutKs: 3900,
      actualPayoutKs: 0,
      status: "pending",
      adminNote: "",
      settledBy: "",
      matchKickoffMs: Date.now() + 7200000,
      matchKickoffText: "Real Madrid vs Barcelona (Tomorrow 01:30)",
      createdAt: pastDate(3200),
      updatedAt: pastDate(3200),
    })
  );

  // Customer submits wallet topup with device receipt image Base64; Owner can edit/clear; customer cannot update
  await assertSucceeds(
    aliceDb.collection("wallet_requests").doc("wr_receipt_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      requestType: "topup",
      amountKs: 10000,
      channel: "KBZPay",
      referenceNote: "Slip #998877",
      receiptImageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
      status: "pending",
      adminNote: "",
      processedBy: "",
      ratePercent: 0,
      rateMode: "none",
      netAmountKs: 10000,
      createdAt: pastDate(2500),
      updatedAt: pastDate(2500),
    })
  );
  await assertFails(
    aliceDb.collection("wallet_requests").doc("wr_receipt_1").update({
      receiptImageBase64: "",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    ownerDb.collection("wallet_requests").doc("wr_receipt_1").update({
      receiptImageBase64: "",
      adminNote: "Cleared duplicate receipt",
      processedBy: OWNER_UID,
      updatedAt: pastDate(1800),
    })
  );
});

test("Dragon Tiger, Lucky Wheel, Multi-Pick Six Animals, 40+ Leagues + Sheet CSV URL, and Second Admin Personal Audit Log Deletion", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const secondDb = testEnv.authenticatedContext("sub_admin_logs", {
    email: "sublogs@example.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      balanceKs: 100000,
      rewardPoints: 100,
      activeStakeKs: 0,
      totalWonKs: 0,
      isLocked: false,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await ctx.firestore().collection("admins").doc("sub_admin_logs").set({
      userId: "sub_admin_logs",
      displayName: "Sub Logs",
      email: "sublogs@example.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "INV_LOGS",
      permissions: { canViewAllLogs: false, canManageBoard: true },
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await ctx.firestore().collection("audit_logs").doc("log_own_1").set({
      actorId: "sub_admin_logs",
      actorName: "Sub Logs",
      actorRole: "admin",
      action: "MATCH_UPDATE",
      category: "board",
      targetId: "m1",
      targetName: "Troyes vs Le Havre",
      details: "Updated score",
      createdAt: pastDate(5000),
    });
    await ctx.firestore().collection("audit_logs").doc("log_owner_1").set({
      actorId: OWNER_UID,
      actorName: "Owner",
      actorRole: "owner",
      action: "CONFIG",
      category: "settings",
      targetId: "cfg",
      targetName: "Config",
      details: "Owner action",
      createdAt: pastDate(5000),
    });
  });

  // Dragon Tiger & Lucky Wheel & Multi-Pick Six Animals mini_game_rounds allowed
  await assertSucceeds(
    aliceDb.collection("mini_game_rounds").doc("mg_dt_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      gameType: "dragon_tiger",
      betLabel: "🐉 နဂါး • 1,000 Ks",
      outcomeLabel: "🐉 Q♠ vs 🐯 9♥",
      stakeKs: 1000,
      payoutKs: 1950,
      createdAt: pastDate(3000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("mini_game_rounds").doc("mg_wheel_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      gameType: "lucky_wheel",
      betLabel: "🎡 ကံစမ်းဘီး • 1,000 Ks",
      outcomeLabel: "2.0x",
      stakeKs: 1000,
      payoutKs: 2000,
      createdAt: pastDate(2900),
    })
  );
  await assertSucceeds(
    aliceDb.collection("mini_game_rounds").doc("mg_6a_multi_1").set({
      userId: ALICE_UID,
      customerName: "Alice",
      gameType: "six_animals",
      betLabel: "ကျား (Tiger), ဆင် (Elephant), လိပ် (Turtle), မြင်း (Horse), ငါး (Fish), ကြက် (Rooster) • 1,000 Ks x 6",
      outcomeLabel: "ကျား (Tiger), လိပ် (Turtle), ငါး (Fish)",
      stakeKs: 6000,
      payoutKs: 12000,
      createdAt: pastDate(2800),
    })
  );
  await assertFails(
    aliceDb.collection("mini_game_rounds").doc("mg_bad_type").set({
      userId: ALICE_UID,
      customerName: "Alice",
      gameType: "roulette",
      betLabel: "Red",
      outcomeLabel: "Black",
      stakeKs: 1000,
      payoutKs: 0,
      createdAt: pastDate(2700),
    })
  );

  // Board config accepts 40+ leagues & googleSheetCsvUrl, rejects > 80 leagues
  const fortyLeagues = Array.from({ length: 42 }, (_, i) => `League ${i + 1}`);
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("board_config").set({
      autoSyncDailyMatches: true,
      autoSyncLiveResults: true,
      lastSyncedDate: "2026-11-26",
      allowedLeagues: fortyLeagues,
      autoSyncOneExpress: true,
      autoSyncFiveFiveFive: true,
      googleSheetCsvUrl: "https://docs.google.com/spreadsheets/d/e/sample/pub?output=csv",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2600),
    })
  );
  const eightyOneLeagues = Array.from({ length: 81 }, (_, i) => `League ${i + 1}`);
  await assertFails(
    ownerDb.collection("app_settings").doc("board_config").update({
      allowedLeagues: eightyOneLeagues,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2500),
    })
  );

  // MiniGameControl accepts dragonTigerMult and luckyWheelMult
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("mini_game_control").set({
      diceStandardMult: 1.95,
      diceTripleMult: 24.0,
      diceExactMult: 6.0,
      sixAnimalsMult: 2.0,
      dragonTigerMult: 1.95,
      luckyWheelMult: 2.0,
      diceAllowedTotals: [],
      diceMaxBigPerDay: 0,
      diceMaxSmallPerDay: 0,
      diceMaxOddPerDay: 0,
      diceMaxEvenPerDay: 0,
      diceTotalDailyQuotas: {},
      animalDailyQuotas: {},
      globalDirective: "stealth_house",
      stealthHouseWinPct: 72,
      userDirectives: {},
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2400),
    })
  );
  await assertFails(
    ownerDb.collection("app_settings").doc("mini_game_control").update({
      dragonTigerMult: 99.0,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2300),
    })
  );

  // Second Admin can delete ONLY own audit log, NOT Owner's audit log
  await assertFails(secondDb.collection("audit_logs").doc("log_owner_1").delete());
  await assertSucceeds(secondDb.collection("audit_logs").doc("log_own_1").delete());
  await assertSucceeds(ownerDb.collection("audit_logs").doc("log_owner_1").delete());
});

test("2D/3D Control Room, Lottery Calendar, Board Sheet RBAC, editable app_update fields, and Ledger clear permissions", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const second2dSheetDb = testEnv.authenticatedContext("sub_2d_sheet", {
    email: "sub2dsheet@example.com",
    email_verified: true,
  }).firestore();
  const secondNoPermDb = testEnv.authenticatedContext("sub_basic", {
    email: "subbasic@example.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc("sub_2d_sheet").set({
      userId: "sub_2d_sheet",
      displayName: "2D Sheet Admin",
      email: "sub2dsheet@example.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "INV_2D_SHEET",
      permissions: {
        canManage2D3DRoom: true,
        canManageBoardSheet: true,
        canManageBoard: false,
      },
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("admins").doc("sub_basic").set({
      userId: "sub_basic",
      displayName: "Basic Admin",
      email: "subbasic@example.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "INV_BASIC",
      permissions: {
        canManage2D3DRoom: false,
        canManageBoardSheet: false,
        canManageBoard: false,
      },
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
    await db.collection("transactions").doc("tx_ledger_clear_1").set({
      userId: ALICE_UID,
      txType: "admin_adjust",
      direction: "credit",
      amountKs: 2000,
      title: "Bonus",
      balanceAfterKs: 2000,
      createdAt: pastDate(5000),
      updatedAt: pastDate(5000),
    });
  });

  // 1. 2D/3D Control Room & Calendar: granted second admin & Owner succeed; basic admin & customer fail writes
  const roomPayload = {
    autoMode: false,
    autoNotifyOnResult: true,
    liveTwod: "49",
    liveSet: "1414.44",
    liveVal: "64009.86",
    liveUpdatedText: "26/11/2026 4:30:00 PM",
    m1201Set: "1432.01",
    m1201Val: "26395.72",
    m1201Twod: "15",
    e430Set: "1414.44",
    e430Val: "64009.86",
    e430Twod: "49",
    am930Modern: "678",
    am930Internet: "696",
    pm200Modern: "638",
    pm200Internet: "539",
    presentPakokku: "Pakokku Special 49",
    presentDailyCard: "Daily 15",
    presentWeeklyCard: "Weekly 4x",
    presentThaiCard: "Thai 49",
    presentAtakeCard: "Atake 15/49",
    lastNotificationTitle: "2D Result",
    lastNotificationBody: "4:30 PM -> 49",
    lastNotificationNonce: 1700000000000,
    updatedBy: "sub_2d_sheet",
    updatedAt: pastDate(3000),
  };
  await assertSucceeds(
    second2dSheetDb.collection("app_settings").doc("lottery_2d3d_config").set(roomPayload)
  );
  await assertFails(
    secondNoPermDb.collection("app_settings").doc("lottery_2d3d_config").set({ ...roomPayload, updatedBy: "sub_basic" })
  );
  await assertFails(
    aliceDb.collection("app_settings").doc("lottery_2d3d_config").set({ ...roomPayload, updatedBy: ALICE_UID })
  );
  await assertSucceeds(aliceDb.collection("app_settings").doc("lottery_2d3d_config").get());

  // Lottery calendar 2D & 3D writes
  await assertSucceeds(
    second2dSheetDb.collection("lottery_calendar").doc("2d_2026_11").set({
      docType: "2d_month",
      year: 2026,
      month: 11,
      days: { "26": { m1201Twod: "15", e430Twod: "49" } },
      updatedBy: "sub_2d_sheet",
      updatedAt: pastDate(2900),
    })
  );
  await assertSucceeds(
    ownerDb.collection("lottery_calendar").doc("3d_2026").set({
      docType: "3d_year",
      year: 2026,
      months: { "11": { firstDay: "01", firstNum: "123", secondDay: "16", secondNum: "456" } },
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2800),
    })
  );
  await assertFails(
    aliceDb.collection("lottery_calendar").doc("2d_2026_11").set({
      docType: "2d_month",
      year: 2026,
      month: 11,
      days: {},
      updatedBy: ALICE_UID,
      updatedAt: pastDate(2700),
    })
  );
  await assertSucceeds(aliceDb.collection("lottery_calendar").get());

  // 2. Board Sheet RBAC: canManageBoardSheet can create/update matches and board_config, cannot delete matches
  await assertSucceeds(
    second2dSheetDb.collection("matches").doc("m_sheet_1").set({
      league: "Premier League",
      homeTeam: "Arsenal",
      awayTeam: "Chelsea",
      kickoffLabel: "21:30",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.95,
      goalOdds: 1.90,
      win1Odds: 1.92,
      drawOdds: 3.40,
      win2Odds: 4.10,
      overOdds: 1.88,
      underOdds: 1.95,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      boardSource: "one_express",
      createdBy: "sub_2d_sheet",
      createdAt: pastDate(2600),
      updatedAt: pastDate(2600),
    })
  );
  await assertSucceeds(
    second2dSheetDb.collection("matches").doc("m_sheet_1").update({
      homeScore: 2,
      awayScore: 1,
      updatedAt: pastDate(2500),
    })
  );
  await assertFails(second2dSheetDb.collection("matches").doc("m_sheet_1").delete());
  await assertFails(
    secondNoPermDb.collection("matches").doc("m_sheet_2").set({
      league: "PL",
      homeTeam: "A",
      awayTeam: "B",
      kickoffLabel: "21:00",
      handicapLine: "Home -0.5",
      goalLine: "2.5",
      bodyOdds: 1.95,
      goalOdds: 1.90,
      status: "open",
      homeScore: 0,
      awayScore: 0,
      visibility: "public",
      createdBy: "sub_basic",
      createdAt: pastDate(2400),
      updatedAt: pastDate(2400),
    })
  );
  await assertSucceeds(
    second2dSheetDb.collection("app_settings").doc("board_config").set({
      autoSyncDailyMatches: true,
      autoSyncLiveResults: true,
      lastSyncedDate: "2026-11-26",
      googleSheetCsvUrl: "https://docs.google.com/spreadsheets/d/e/test/pub?output=csv",
      updatedBy: "sub_2d_sheet",
      updatedAt: pastDate(2300),
    })
  );

  // 3. Editable app_update text fields succeed for Owner
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("app_update").set({
      latestVersionCode: 6,
      latestVersionName: "6.0",
      downloadUrl: "https://github.com/Htet-Thu-Ya/Htet-Thu-Ya/releases/download/latest/LoTaYa.apk",
      releaseNotes: "New 2D/3D Live Hub",
      forceUpdate: false,
      bannerTitle: "Update Ready!",
      bannerSubtitle: "Tap to get 2D/3D Live & A-P Sheet",
      dialogTitle: "LoTaYa New Version",
      installButtonLabel: "Install Now",
      laterButtonLabel: "Later",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2200),
    })
  );

  // 4. Ledger delete permissions: Second Admin cannot delete, Owner can clear/delete
  await assertFails(second2dSheetDb.collection("transactions").doc("tx_ledger_clear_1").delete());
  await assertSucceeds(ownerDb.collection("transactions").doc("tx_ledger_clear_1").delete());
});

test("Second Admin Wallet Float Ledger Parity, Board Sheet Two-Way Fields, Privacy Flags, and Custom Upload Logo", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const secondCashierDb = testEnv.authenticatedContext("sub_cashier", {
    email: "cashier@example.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc("sub_cashier").set({
      userId: "sub_cashier",
      displayName: "Cashier 1",
      email: "cashier@example.com",
      role: "admin",
      status: "active",
      addedBy: OWNER_UID,
      inviteCodeUsed: "INV_CASHIER",
      permissions: { canApproveWallet: true, canAdjustBalance: true },
      walletBalanceKs: 500000,
      totalAllocatedKs: 500000,
      totalTopupsGivenKs: 0,
      totalWithdrawalsTakenKs: 0,
      createdAt: pastDate(10000),
      updatedAt: pastDate(10000),
    });
  });

  // 1. Main Admin allocates extra float to Second Admin
  await assertSucceeds(
    ownerDb.collection("admins").doc("sub_cashier").update({
      walletBalanceKs: 700000,
      totalAllocatedKs: 700000,
      updatedAt: pastDate(4000),
    })
  );

  // 2. Second Admin cannot mint arbitrary float without matching withdrawal counter
  await assertFails(
    secondCashierDb.collection("admins").doc("sub_cashier").update({
      walletBalanceKs: 900000,
      updatedAt: pastDate(3500),
    })
  );

  // 3. Second Admin deducts 50,000 Ks for a customer topup with exact parity
  await assertSucceeds(
    secondCashierDb.collection("admins").doc("sub_cashier").update({
      walletBalanceKs: 650000,
      totalTopupsGivenKs: 50000,
      totalWithdrawalsTakenKs: 0,
      updatedAt: pastDate(3000),
    })
  );

  // 4. Second Admin adds 20,000 Ks back from a customer withdrawal with exact parity
  await assertSucceeds(
    secondCashierDb.collection("admins").doc("sub_cashier").update({
      walletBalanceKs: 670000,
      totalTopupsGivenKs: 50000,
      totalWithdrawalsTakenKs: 20000,
      updatedAt: pastDate(2500),
    })
  );

  // 5. BoardConfig accepts googleSheetEditUrl, googleSheetWebhookUrl, autoTwoWaySheetSync, activeMatchDateFilter
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("board_config").set({
      autoSyncDailyMatches: true,
      autoSyncLiveResults: true,
      lastSyncedDate: "2026-11-27",
      allowedLeagues: ["Thai League 1", "English Premier League"],
      autoSyncOneExpress: true,
      autoSyncFiveFiveFive: true,
      googleSheetCsvUrl: "https://docs.google.com/spreadsheets/d/abc123/export?format=csv",
      googleSheetEditUrl: "https://docs.google.com/spreadsheets/d/abc123/edit#gid=0",
      googleSheetWebhookUrl: "https://script.google.com/macros/s/sample/exec",
      autoTwoWaySheetSync: true,
      activeMatchDateFilter: "2026-11-27",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(2000),
    })
  );

  // 6. SecurityConfig accepts privacy & screenshot protection flags
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("security_config").set({
      requireAdminPin: false,
      ipWhitelistEnabled: false,
      allowedIps: [],
      globalForceLogoutAtMs: 0,
      hidePublicIpDisplay: true,
      maskCustomerPiiForSubAdmins: true,
      screenshotProtectionEnabled: true,
      updatedBy: OWNER_UID,
      updatedAt: pastDate(1800),
    })
  );

  // 7. BrandingConfig accepts custom_upload & customLogoBase64 (customer cannot modify)
  await assertSucceeds(
    ownerDb.collection("app_settings").doc("branding_config").set({
      appName: "လိုတရ",
      logoVariant: "custom_upload",
      customLogoUrl: "",
      customLogoBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      themePreset: "emerald_gold",
      showRulesOnStartup: true,
      rulesText: "Rules",
      maxContactMainAdminKs: 500000,
      mainAdminContactNote: "Contact owner",
      updatedBy: OWNER_UID,
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(
    aliceDb.collection("app_settings").doc("security_config").update({
      screenshotProtectionEnabled: false,
      updatedBy: ALICE_UID,
      updatedAt: pastDate(1200),
    })
  );
});











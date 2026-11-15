const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const serverTimestamp = () =>
  testEnv.authenticatedContext("probe").firestore().app.firebase.firestore.FieldValue.serverTimestamp();
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_client_1";
const BOB_UID = "bob_seller_2";
const ADMIN_UID = "admin_gnanvi_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validFedaPayOrder(userId, overrides = {}) {
  return {
    userId,
    customerName: "Aïcha Koné",
    customerEmail: "aicha@example.com",
    sellerName: "Nexora Officiel",
    itemsSummary: "Phantom X3 Pro 5G (x1)",
    items: [
      {
        productId: "p1",
        name: "Phantom X3 Pro 5G",
        quantity: 1,
        unitPriceXof: 180000,
      },
    ],
    subtotalXof: 180000,
    discountXof: 0,
    shippingFeeXof: 3000,
    totalAmountXof: 183000,
    platformCommissionXof: 18300,
    sellerShareXof: 164700,
    commissionRate: 0.1,
    paymentMethod: "FedaPay",
    status: "En attente de paiement",
    shippingAddress: "Cocody, Abidjan, Côte d'Ivoire",
    shippingMethod: "Livraison Express à Domicile",
    trackingProgress: 0.1,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
}

function validSeller(ownerId, overrides = {}) {
  return {
    ownerId,
    businessName: "AfriCraft Co.",
    sellerName: "Amadou Diop",
    category: "Mode",
    email: "amadou@africraft.com",
    status: "En attente",
    isVerified: false,
    balanceXof: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides,
  };
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

test("Unauthenticated user cannot read or write users, sellers, orders, or webhook events", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("sellers").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("processed_webhook_events").doc("evt_1").get());
  await assertFails(
    unauthDb.collection("orders").doc("order_1").set(validFedaPayOrder(ALICE_UID))
  );
});

test("Authenticated client can create own client profile, cannot self-assign admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Traoré",
      email: "alice@example.com",
      role: "client",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Traoré",
      email: "alice@example.com",
      role: "admin",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );
});

test("Client can create FedaPay order ONLY with status 'En attente de paiement' and NEVER 'Payée'", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_ok").set(validFedaPayOrder(ALICE_UID))
  );

  // Attempting to create an already paid order must be rejected
  await assertFails(
    aliceDb.collection("orders").doc("ord_forged_paid").set(
      validFedaPayOrder(ALICE_UID, { status: "Payée" })
    )
  );

  // Attempting to create COD order as Paid must be rejected
  await assertFails(
    aliceDb.collection("orders").doc("ord_forged_cod").set(
      validFedaPayOrder(ALICE_UID, {
        paymentMethod: "Paiement à la livraison",
        status: "Payée",
      })
    )
  );

  // Valid COD order with 'En attente' succeeds
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_cod_ok").set(
      validFedaPayOrder(ALICE_UID, {
        paymentMethod: "Paiement à la livraison",
        status: "En attente",
      })
    )
  );
});

test("Client cannot mark their own pending order as 'Payée' via update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_pending").set(validFedaPayOrder(ALICE_UID))
  );

  await assertFails(
    aliceDb.collection("orders").doc("ord_pending").update({
      status: "Payée",
      updatedAt: serverTimestamp(),
    })
  );

  // Client CAN cancel their own pending order
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_pending").update({
      status: "Annulée",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Commission math invariant is enforced in rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("orders").doc("ord_bad_comm").set(
      validFedaPayOrder(ALICE_UID, {
        platformCommissionXof: 1000,
        sellerShareXof: 1000,
      })
    )
  );
});

test("Cross-user isolation: Bob cannot read Alice's order or list orders without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("alice_ord").set(validFedaPayOrder(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("orders").doc("alice_ord").get());
  await assertFails(bobDb.collection("orders").get());

  // Matching owner filter succeeds
  await assertSucceeds(
    aliceDb.collection("orders").where("userId", "==", ALICE_UID).get()
  );
});

test("Seller lifecycle: seller creates pending store; cannot self-approve; admin approves and suspends", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("sellers").doc("sel_1").set(validSeller(BOB_UID))
  );

  // Seller cannot self-approve or inflate balance
  await assertFails(
    bobDb.collection("sellers").doc("sel_1").update({
      status: "Approuvé",
      isVerified: true,
      updatedAt: serverTimestamp(),
    })
  );

  // Verified admin (via gnanvidavid@gmail.com) can approve & suspend seller and list all sellers
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "gnanvidavid@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(adminDb.collection("sellers").get());
  await assertSucceeds(
    adminDb.collection("sellers").doc("sel_1").update({
      status: "Approuvé",
      isVerified: true,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    adminDb.collection("sellers").doc("sel_1").update({
      status: "Suspendu",
      isVerified: false,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Admin can update fulfillment status (Expédiée/Livrée) but cannot forge 'Payée'", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_fulfill").set(validFedaPayOrder(ALICE_UID))
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "gnanvidavid@gmail.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    adminDb.collection("orders").doc("ord_fulfill").update({
      status: "Payée",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    adminDb.collection("orders").doc("ord_fulfill").update({
      status: "Expédiée",
      trackingProgress: 0.75,
      updatedAt: serverTimestamp(),
    })
  );
});

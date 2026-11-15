const { test } = require("node:test");
const assert = require("node:assert/strict");
const crypto = require("node:crypto");
const {
  verifyFedaPaySignature,
  processFedaPayWebhookEvent,
} = require("./webhookHandler");

function signPayload(payloadStr, secret, timestampSec) {
  const sig = crypto
    .createHmac("sha256", secret)
    .update(`${timestampSec}.${payloadStr}`, "utf8")
    .digest("hex");
  return `t=${timestampSec},s=${sig}`;
}

function createInMemoryDb(initialOrders = {}) {
  const collections = {
    orders: new Map(Object.entries(initialOrders)),
    processed_webhook_events: new Map(),
  };

  function makeDocRef(colName, docId) {
    return {
      colName,
      docId,
      get: async () => {
        const data = collections[colName].get(docId);
        return {
          exists: data !== undefined,
          data: () => data,
        };
      },
    };
  }

  return {
    collections,
    collection: (colName) => ({
      doc: (docId) => makeDocRef(colName, docId),
    }),
    runTransaction: async (fn) => {
      const tx = {
        get: async (ref) => ref.get(),
        set: (ref, data) => {
          collections[ref.colName].set(ref.docId, data);
        },
        update: (ref, patch) => {
          const prev = collections[ref.colName].get(ref.docId) || {};
          collections[ref.colName].set(ref.docId, { ...prev, ...patch });
        },
      };
      return fn(tx);
    },
  };
}

test("verifyFedaPaySignature accepts valid X-FEDAPAY-SIGNATURE and rejects tampered payload or secret", () => {
  const secret = "wh_test_secret_123";
  const now = 1700000000;
  const body = JSON.stringify({ id: "evt_1", name: "transaction.approved" });
  const header = signPayload(body, secret, now);

  assert.equal(
    verifyFedaPaySignature(body, header, secret, { nowSeconds: now + 10 }).valid,
    true
  );
  assert.equal(
    verifyFedaPaySignature(body, header, "wrong_secret", { nowSeconds: now + 10 }).valid,
    false
  );
  assert.equal(
    verifyFedaPaySignature(body + " ", header, secret, { nowSeconds: now + 10 }).valid,
    false
  );
  assert.equal(
    verifyFedaPaySignature(body, header, secret, { nowSeconds: now + 1000, toleranceSeconds: 300 }).valid,
    false
  );
});

test("processFedaPayWebhookEvent marks order Payée on transaction.approved and ignores duplicate event", async () => {
  const db = createInMemoryDb({
    "NEX-1001": { status: "En attente de paiement", totalAmountXof: 50000 },
  });

  const event = {
    id: "evt_approved_1",
    name: "transaction.approved",
    entity: {
      id: 98765,
      custom_metadata: { order_id: "NEX-1001" },
    },
  };

  const firstRes = await processFedaPayWebhookEvent({ db, event });
  assert.equal(firstRes.statusCode, 200);
  assert.equal(firstRes.body.duplicate, false);
  assert.equal(firstRes.body.appliedStatus, "Payée");
  assert.equal(db.collections.orders.get("NEX-1001").status, "Payée");
  assert.equal(db.collections.orders.get("NEX-1001").fedapayTransactionId, "98765");

  // Même événement envoyé une seconde fois : ne doit pas retraiter la commande
  db.collections.orders.get("NEX-1001").marker = "untouched";
  const secondRes = await processFedaPayWebhookEvent({ db, event });
  assert.equal(secondRes.statusCode, 200);
  assert.equal(secondRes.body.duplicate, true);
  assert.equal(db.collections.orders.get("NEX-1001").marker, "untouched");
});

test("processFedaPayWebhookEvent marks order Paiement échoué on transaction.declined and transaction.canceled", async () => {
  const db = createInMemoryDb({
    "NEX-1002": { status: "En attente de paiement", totalAmountXof: 25000 },
    "NEX-1003": { status: "En attente de paiement", totalAmountXof: 12000 },
  });

  const declinedRes = await processFedaPayWebhookEvent({
    db,
    event: {
      id: "evt_declined_1",
      name: "transaction.declined",
      entity: { id: 111, custom_metadata: { order_id: "NEX-1002" } },
    },
  });
  assert.equal(declinedRes.statusCode, 200);
  assert.equal(db.collections.orders.get("NEX-1002").status, "Paiement échoué");

  const canceledRes = await processFedaPayWebhookEvent({
    db,
    event: {
      id: "evt_canceled_1",
      name: "transaction.canceled",
      entity: { id: 222, custom_metadata: { order_id: "NEX-1003" } },
    },
  });
  assert.equal(canceledRes.statusCode, 200);
  assert.equal(db.collections.orders.get("NEX-1003").status, "Paiement échoué");
});

const crypto = require("node:crypto");

const DEFAULT_DATABASE_ID =
  process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-nexorama-5478da35-409e-4322-967c-e47b25abc8c1";

/**
 * Analyse l'en-tête X-FEDAPAY-SIGNATURE au format "t=<timestamp>,s=<hmac_hex>".
 */
function parseSignatureHeader(headerValue) {
  if (!headerValue || typeof headerValue !== "string") {
    return null;
  }
  const parts = headerValue.split(",").map((p) => p.trim());
  let timestamp = null;
  const signatures = [];

  for (const part of parts) {
    const eqIndex = part.indexOf("=");
    if (eqIndex <= 0) continue;
    const key = part.slice(0, eqIndex).trim();
    const val = part.slice(eqIndex + 1).trim();
    if (key === "t") {
      timestamp = val;
    } else if (key === "s" && val) {
      signatures.push(val);
    }
  }

  if (!timestamp || signatures.length === 0) {
    return null;
  }
  return { timestamp, signatures };
}

/**
 * Vérifie la signature HMAC-SHA256 de l'en-tête X-FEDAPAY-SIGNATURE
 * avec la clé secrète du webhook stockée dans process.env.FEDAPAY_WEBHOOK_SECRET.
 */
function verifyFedaPaySignature(
  rawBody,
  signatureHeader,
  webhookSecret,
  options = {}
) {
  if (!webhookSecret) {
    return { valid: false, reason: "Missing FEDAPAY_WEBHOOK_SECRET" };
  }
  const parsed = parseSignatureHeader(signatureHeader);
  if (!parsed) {
    return { valid: false, reason: "Invalid or missing X-FEDAPAY-SIGNATURE header" };
  }

  const toleranceSeconds =
    options.toleranceSeconds ??
    Number(process.env.FEDAPAY_SIGNATURE_TOLERANCE_SECONDS || 300);
  const nowSeconds = options.nowSeconds ?? Math.floor(Date.now() / 1000);
  const tsNum = Number(parsed.timestamp);
  if (!Number.isFinite(tsNum)) {
    return { valid: false, reason: "Invalid signature timestamp" };
  }
  if (toleranceSeconds > 0 && Math.abs(nowSeconds - tsNum) > toleranceSeconds) {
    return { valid: false, reason: "Webhook signature timestamp expired" };
  }

  const payloadString = Buffer.isBuffer(rawBody)
    ? rawBody.toString("utf8")
    : String(rawBody || "");
  const signedPayload = `${parsed.timestamp}.${payloadString}`;
  const expectedHex = crypto
    .createHmac("sha256", webhookSecret)
    .update(signedPayload, "utf8")
    .digest("hex");

  const expectedBuf = Buffer.from(expectedHex, "utf8");
  const matched = parsed.signatures.some((sig) => {
    const sigBuf = Buffer.from(sig, "utf8");
    if (sigBuf.length !== expectedBuf.length) return false;
    return crypto.timingSafeEqual(sigBuf, expectedBuf);
  });

  if (!matched) {
    return { valid: false, reason: "Signature mismatch" };
  }
  return { valid: true };
}

/**
 * Détermine le nouveau statut de commande selon l'événement FedaPay.
 */
function mapEventToOrderUpdate(eventName) {
  switch (eventName) {
    case "transaction.approved":
      return {
        status: "Payée",
        trackingProgress: 0.5,
        isPaid: true,
      };
    case "transaction.declined":
    case "transaction.canceled":
      return {
        status: "Paiement échoué",
        trackingProgress: 0.0,
        isPaid: false,
      };
    default:
      return null;
  }
}

/**
 * Extrait l'identifiant de commande et de transaction depuis le payload webhook FedaPay.
 */
function extractOrderMetadata(event) {
  const entity = event?.entity || event?.data?.object || event?.data || {};
  const customMeta =
    entity.custom_metadata || entity.metadata || event?.custom_metadata || {};
  const orderId =
    customMeta.order_id ||
    customMeta.orderId ||
    entity.merchant_reference ||
    entity.reference ||
    null;
  const transactionId =
    entity.id != null ? String(entity.id) : event?.object_id != null ? String(event.object_id) : null;
  return { orderId, transactionId, entity };
}

/**
 * Traite un événement FedaPay de manière idempotente dans Firestore :
 * si eventId a déjà été traité, la commande n'est modifiée qu'une seule fois.
 */
async function processFedaPayWebhookEvent({ db, event, FieldValue }) {
  const eventId = event?.id != null ? String(event.id) : null;
  const eventName = event?.name || event?.type || "";

  if (!eventId || !eventName) {
    return { statusCode: 400, body: { error: "Invalid FedaPay event payload" } };
  }

  const updateSpec = mapEventToOrderUpdate(eventName);
  if (!updateSpec) {
    return {
      statusCode: 200,
      body: { ignored: true, reason: `Unhandled event ${eventName}` },
    };
  }

  const { orderId, transactionId } = extractOrderMetadata(event);
  if (!orderId) {
    return {
      statusCode: 400,
      body: { error: "Missing order_id in transaction custom_metadata" },
    };
  }

  const eventRef = db.collection("processed_webhook_events").doc(eventId);
  const orderRef = db.collection("orders").doc(String(orderId));
  const serverTimestamp = FieldValue
    ? FieldValue.serverTimestamp()
    : new Date();

  const outcome = await db.runTransaction(async (tx) => {
    const existingEventSnap = await tx.get(eventRef);
    if (existingEventSnap.exists) {
      return { duplicate: true, orderId, eventId, status: "already_processed" };
    }

    const orderSnap = await tx.get(orderRef);
    if (!orderSnap.exists) {
      return { notFound: true, orderId, eventId };
    }

    const currentOrder = orderSnap.data() || {};
    // Ne pas rétrograder une commande déjà payée si un événement obsolète arrive
    if (currentOrder.status === "Payée" && !updateSpec.isPaid) {
      tx.set(eventRef, {
        eventId,
        eventName,
        orderId: String(orderId),
        transactionId,
        ignoredReason: "order_already_paid",
        processedAt: serverTimestamp,
      });
      return { duplicate: false, ignored: true, orderId, status: currentOrder.status };
    }

    const orderUpdate = {
      status: updateSpec.status,
      trackingProgress: updateSpec.trackingProgress,
      lastWebhookEventId: eventId,
      updatedAt: serverTimestamp,
    };
    if (transactionId) {
      orderUpdate.fedapayTransactionId = transactionId;
    }
    if (updateSpec.isPaid) {
      orderUpdate.paidAt = serverTimestamp;
    }

    tx.update(orderRef, orderUpdate);
    tx.set(eventRef, {
      eventId,
      eventName,
      orderId: String(orderId),
      transactionId,
      appliedStatus: updateSpec.status,
      processedAt: serverTimestamp,
    });

    return {
      duplicate: false,
      orderId: String(orderId),
      eventId,
      appliedStatus: updateSpec.status,
    };
  });

  if (outcome.notFound) {
    return {
      statusCode: 404,
      body: { error: `Order ${orderId} not found` },
    };
  }

  return {
    statusCode: 200,
    body: {
      received: true,
      ...outcome,
    },
  };
}

module.exports = {
  DEFAULT_DATABASE_ID,
  parseSignatureHeader,
  verifyFedaPaySignature,
  mapEventToOrderUpdate,
  extractOrderMetadata,
  processFedaPayWebhookEvent,
};

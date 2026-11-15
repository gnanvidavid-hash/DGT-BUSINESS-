const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const {
  DEFAULT_DATABASE_ID,
  verifyFedaPaySignature,
  processFedaPayWebhookEvent,
} = require("./webhookHandler");

if (!admin.apps.length) {
  admin.initializeApp();
}

const databaseId = process.env.FIRESTORE_DATABASE_ID || DEFAULT_DATABASE_ID;
const db = getFirestore(admin.app(), databaseId);

/**
 * Endpoint HTTPS recevant les webhooks FedaPay.
 * Secrets attendus dans les variables d'environnement :
 *  - FEDAPAY_WEBHOOK_SECRET
 *  - FEDAPAY_SECRET_KEY (si appels API serveur complémentaires)
 *  - FIRESTORE_DATABASE_ID
 */
exports.fedapayWebhook = onRequest(
  {
    region: "europe-west1",
    cors: false,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      return res.status(405).json({ error: "Method Not Allowed" });
    }

    const webhookSecret = process.env.FEDAPAY_WEBHOOK_SECRET;
    if (!webhookSecret) {
      console.error("FEDAPAY_WEBHOOK_SECRET non configuré.");
      return res.status(500).json({ error: "Webhook secret not configured" });
    }

    const signatureHeader =
      req.get("X-FEDAPAY-SIGNATURE") || req.get("x-fedapay-signature") || "";
    const rawBody = req.rawBody || JSON.stringify(req.body || {});

    const verification = verifyFedaPaySignature(
      rawBody,
      signatureHeader,
      webhookSecret
    );
    if (!verification.valid) {
      console.warn("Signature webhook FedaPay invalide:", verification.reason);
      return res.status(401).json({ error: verification.reason });
    }

    let event = req.body;
    if (!event || typeof event !== "object") {
      try {
        event = JSON.parse(rawBody.toString("utf8"));
      } catch (err) {
        return res.status(400).json({ error: "Invalid JSON payload" });
      }
    }

    try {
      const result = await processFedaPayWebhookEvent({
        db,
        event,
        FieldValue,
      });
      return res.status(result.statusCode).json(result.body);
    } catch (error) {
      console.error("Erreur traitement webhook FedaPay:", error);
      return res.status(500).json({ error: "Internal server error" });
    }
  }
);

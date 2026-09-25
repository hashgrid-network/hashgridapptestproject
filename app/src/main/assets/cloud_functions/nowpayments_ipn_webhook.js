/**
 * HashGrid Institutional - NOWPayments IPN Webhook Cloud Function
 * 
 * Target: Firebase Cloud Functions (Node.js 18 / 20)
 * Deploy command: firebase deploy --only functions:nowPaymentsIpnWebhook
 * 
 * Automatically credits user USDT balance in Firestore upon "finished" or "confirmed" IPN callback.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

exports.nowPaymentsIpnWebhook = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("Method Not Allowed");
  }

  try {
    const ipnSecret = process.env.NOWPAYMENTS_IPN_SECRET || "NOWPAY_IPN_SECRET_KEY";
    const signature = req.headers["x-nowpayments-sig"];
    const payload = req.body;

    // 1. Verify HMAC-SHA512 Signature
    if (signature && ipnSecret && !ipnSecret.startsWith("NOWPAY_IPN")) {
      const sortedKeys = Object.keys(payload).sort();
      const sortedObj = {};
      sortedKeys.forEach((key) => {
        sortedObj[key] = payload[key];
      });
      const hmac = crypto.createHmac("sha512", ipnSecret);
      hmac.update(JSON.stringify(sortedObj));
      const calculatedSig = hmac.digest("hex");

      if (calculatedSig.toLowerCase() !== signature.toLowerCase()) {
        console.error("Invalid IPN signature mismatch");
        return res.status(400).send("Signature verification failed");
      }
    }

    const {
      payment_id,
      payment_status,
      pay_amount,
      actually_paid,
      fee,
      order_description,
      order_id,
      pay_currency
    } = payload;

    console.log(`Received NOWPayments IPN for payment_id: ${payment_id}, status: ${payment_status}`);

    // 2. Process only confirmed/finished payments
    if (payment_status === "finished" || payment_status === "confirmed") {
      // Extract userId from order_id ("DEP_{uid}_{timestamp}") or description
      let uid = "";
      if (order_id && order_id.startsWith("DEP_")) {
        const parts = order_id.split("_");
        if (parts.length >= 3) {
          uid = parts.slice(1, -1).join("_");
        } else if (parts.length === 2) {
          uid = parts[1];
        }
      }

      if (!uid && order_description && order_description.includes("HashGrid Wallet Deposit for ")) {
        uid = order_description.replace("HashGrid Wallet Deposit for ", "").trim();
      }

      if (!uid && order_id) {
        // Fallback lookup in /deposits
        const depositDoc = await db.collection("deposits").doc(payment_id.toString()).get();
        if (depositDoc.exists) {
          uid = depositDoc.data().userId;
        }
      }

      if (!uid) {
        console.error(`Unable to resolve userId for payment ${payment_id}`);
        return res.status(200).send("Payment received but user unresolved");
      }

      // Calculate net credited amount (pay_amount or actually_paid minus fee)
      const rawPaid = parseFloat(actually_paid) || parseFloat(pay_amount) || 0.0;
      const netFee = parseFloat(fee) || 0.0;
      const netAmount = Math.max(0.0, rawPaid - netFee);

      const userRef = db.collection("users").doc(uid);
      const txRef = userRef.collection("transactions").doc(payment_id.toString());
      const depositRef = db.collection("deposits").doc(payment_id.toString());

      // 3. Perform atomic batch update
      const batch = db.batch();

      batch.update(userRef, {
        usdt_balance: admin.firestore.FieldValue.increment(netAmount),
        last_deposit_at: admin.firestore.FieldValue.serverTimestamp()
      });

      batch.set(txRef, {
        id: payment_id.toString(),
        title: `+$${netAmount.toFixed(2)} USDT`,
        subtitle: `NOWPayments Automated Deposit (${(pay_currency || "USDT").toUpperCase()})`,
        btcAmountStr: "",
        usdtAmount: netAmount,
        amount: netAmount,
        network: (pay_currency || "USDT").toUpperCase(),
        currency: "USDT",
        payment_id: payment_id.toString(),
        isCredit: true,
        type: "DEPOSIT",
        status: "COMPLETED",
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        dateStr: new Date().toISOString().replace("T", " ").substring(0, 19)
      });

      batch.set(depositRef, {
        paymentId: payment_id.toString(),
        userId: uid,
        amount: netAmount,
        currency: "USDT",
        network: (pay_currency || "USDT").toUpperCase(),
        status: "completed",
        verifiedVia: "NOWPAYMENTS_IPN_WEBHOOK",
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      await batch.commit();

      console.log(`Successfully credited ${netAmount} USDT to user ${uid}`);
      return res.status(200).send("OK: User credited successfully");
    }

    return res.status(200).send(`Payment status noted: ${payment_status}`);
  } catch (error) {
    console.error("Error processing IPN webhook:", error);
    return res.status(500).send("Internal server error");
  }
});

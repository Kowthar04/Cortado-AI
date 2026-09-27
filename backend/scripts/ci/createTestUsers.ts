/**
 * Creates two throwaway Firebase Auth users (a customer and an admin) plus
 * their users/{uid} Firestore docs, for the Android emulator screenshot CI
 * job. Writes their credentials to a local JSON file so the job's shell
 * script can use them without printing them anywhere. Run its counterpart,
 * deleteTestUsers.ts, in a workflow `if: always()` step to clean up.
 *
 * Usage: ts-node scripts/ci/createTestUsers.ts <output-json-path>
 */
import * as crypto from "crypto";
import * as fs from "fs";
import * as admin from "firebase-admin";

function randomPassword(): string {
  return crypto.randomBytes(18).toString("base64").replace(/[^A-Za-z0-9]/g, "") + "!A1";
}

async function main() {
  const outputPath = process.argv[2];
  if (!outputPath) {
    throw new Error("Usage: createTestUsers.ts <output-json-path>");
  }

  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!serviceAccountJson) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is not set");
  }
  admin.initializeApp({ credential: admin.credential.cert(JSON.parse(serviceAccountJson)) });

  const auth = admin.auth();
  const db = admin.firestore();
  const runId = crypto.randomBytes(4).toString("hex");

  const customerEmail = `ci-customer-${runId}@example.com`;
  const customerPassword = randomPassword();
  const adminEmail = `ci-admin-${runId}@example.com`;
  const adminPassword = randomPassword();

  const customer = await auth.createUser({ email: customerEmail, password: customerPassword });
  await db.collection("users").doc(customer.uid).set({
    firstname: "CI",
    surname: "Customer",
    email: customerEmail,
    role: "customer",
  });

  const adminUser = await auth.createUser({ email: adminEmail, password: adminPassword });
  await db.collection("users").doc(adminUser.uid).set({
    firstname: "CI",
    surname: "Admin",
    email: adminEmail,
    role: "admin",
  });

  fs.writeFileSync(
    outputPath,
    JSON.stringify({
      customerUid: customer.uid,
      customerEmail,
      customerPassword,
      adminUid: adminUser.uid,
      adminEmail,
      adminPassword,
    }),
    { mode: 0o600 },
  );

  // eslint-disable-next-line no-console
  console.log(`Created CI test users: customer=${customer.uid}, admin=${adminUser.uid}`);
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error("Failed to create CI test users:", err.message);
  process.exit(1);
});

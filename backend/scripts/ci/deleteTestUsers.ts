/**
 * Deletes the throwaway CI test users created by createTestUsers.ts (both
 * their Firebase Auth accounts and users/{uid} Firestore docs). Run as an
 * `if: always()` cleanup step so a failed/cancelled workflow run never
 * leaves test accounts behind in the live project.
 *
 * Usage: ts-node scripts/ci/deleteTestUsers.ts <creds-json-path>
 */
import * as fs from "fs";
import * as admin from "firebase-admin";

async function main() {
  const credsPath = process.argv[2];
  if (!credsPath || !fs.existsSync(credsPath)) {
    // eslint-disable-next-line no-console
    console.log("No creds file found, nothing to clean up.");
    return;
  }

  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!serviceAccountJson) {
    throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is not set");
  }
  admin.initializeApp({ credential: admin.credential.cert(JSON.parse(serviceAccountJson)) });

  const { customerUid, adminUid } = JSON.parse(fs.readFileSync(credsPath, "utf-8"));
  const auth = admin.auth();
  const db = admin.firestore();

  for (const uid of [customerUid, adminUid]) {
    if (!uid) continue;
    await db.collection("users").doc(uid).delete().catch(() => undefined);
    await auth.deleteUser(uid).catch(() => undefined);
  }

  fs.unlinkSync(credsPath);
  // eslint-disable-next-line no-console
  console.log(`Deleted CI test users: customer=${customerUid}, admin=${adminUid}`);
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error("Failed to delete CI test users:", err.message);
  process.exit(1);
});

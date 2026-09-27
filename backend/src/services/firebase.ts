import admin from "firebase-admin";
import { config } from "../config";

let app: admin.app.App | undefined;

/**
 * Initializes (once) and returns the firebase-admin App using credentials
 * from either FIREBASE_SERVICE_ACCOUNT_JSON (a full service account JSON
 * string) or GOOGLE_APPLICATION_CREDENTIALS (a path to a service account
 * JSON file). Never reads a committed key file.
 */
export function getFirebaseApp(): admin.app.App {
  if (app) {
    return app;
  }

  if (admin.apps.length > 0 && admin.apps[0]) {
    app = admin.apps[0];
    return app;
  }

  if (config.firebaseServiceAccountJson) {
    let serviceAccount: admin.ServiceAccount;
    try {
      serviceAccount = JSON.parse(config.firebaseServiceAccountJson);
    } catch (err) {
      throw new Error(`FIREBASE_SERVICE_ACCOUNT_JSON is not valid JSON: ${(err as Error).message}`);
    }
    app = admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId: config.firebaseProjectId,
    });
    return app;
  }

  // Falls back to GOOGLE_APPLICATION_CREDENTIALS (a file path) or, on a
  // hosted GCP-adjacent environment, application default credentials.
  app = admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    projectId: config.firebaseProjectId,
  });
  return app;
}

export function getFirestore(): admin.firestore.Firestore {
  return getFirebaseApp().firestore();
}

export function getAuth(): admin.auth.Auth {
  return getFirebaseApp().auth();
}

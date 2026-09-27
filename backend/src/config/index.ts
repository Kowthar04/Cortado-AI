import dotenv from "dotenv";

dotenv.config();

export interface AppConfig {
  port: number;
  nodeEnv: string;
  isProduction: boolean;
  firebaseProjectId: string;
  firebaseServiceAccountJson: string | undefined;
  googleApplicationCredentials: string | undefined;
  geminiApiKey: string;
  geminiModel: string;
  chatOrderHistoryLimit: number;
}

/**
 * A required env var that is missing in production throws immediately
 * (fail fast) instead of allowing the server to start in a broken state.
 * In non-production environments a warning is logged and an empty string is
 * used, which keeps local development and tests usable without secrets.
 */
function requireEnv(name: string, isProduction: boolean): string {
  const value = process.env[name];
  if (value && value.trim().length > 0) {
    return value;
  }
  if (isProduction) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  // eslint-disable-next-line no-console
  console.warn(`[config] Warning: environment variable ${name} is not set.`);
  return "";
}

function loadConfig(): AppConfig {
  const nodeEnv = process.env.NODE_ENV ?? "development";
  const isProduction = nodeEnv === "production";

  return {
    port: Number.parseInt(process.env.PORT ?? "3000", 10),
    nodeEnv,
    isProduction,
    firebaseProjectId: process.env.FIREBASE_PROJECT_ID ?? "cafeshopassignment",
    firebaseServiceAccountJson: process.env.FIREBASE_SERVICE_ACCOUNT_JSON,
    googleApplicationCredentials: process.env.GOOGLE_APPLICATION_CREDENTIALS,
    geminiApiKey: requireEnv("GEMINI_API_KEY", isProduction),
    geminiModel: process.env.GEMINI_MODEL ?? "gemini-3.8-flash",
    chatOrderHistoryLimit: Number.parseInt(process.env.CHAT_ORDER_HISTORY_LIMIT ?? "5", 10),
  };
}

export const config = loadConfig();

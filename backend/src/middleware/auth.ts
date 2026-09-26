import { NextFunction, Request, Response } from "express";
import { getAuth, getFirestore } from "../services/firebase";
import { ApiError, asyncHandler } from "./errors";
import { UserRole } from "../types/models";

export interface AuthenticatedRequest extends Request {
  uid?: string;
  userRole?: UserRole;
}

function extractBearerToken(req: Request): string | undefined {
  const header = req.headers.authorization;
  if (!header || !header.startsWith("Bearer ")) {
    return undefined;
  }
  const token = header.slice("Bearer ".length).trim();
  return token.length > 0 ? token : undefined;
}

/**
 * Verifies the Firebase ID token on `Authorization: Bearer <idToken>`.
 * Any authenticated user (any role) passes this check.
 */
export const requireAuth = asyncHandler(
  async (req: AuthenticatedRequest, _res: Response, next: NextFunction): Promise<void> => {
    const token = extractBearerToken(req);
    if (!token) {
      throw ApiError.unauthorized("Missing Authorization: Bearer <idToken> header.");
    }

    let decoded;
    try {
      decoded = await getAuth().verifyIdToken(token);
    } catch {
      throw ApiError.unauthorized("The provided Firebase ID token is invalid or expired.");
    }

    req.uid = decoded.uid;
    next();
  },
);

/**
 * Must run after requireAuth. Additionally checks that the caller's
 * users/{uid}.role Firestore field is "admin".
 */
export const requireAdmin = asyncHandler(
  async (req: AuthenticatedRequest, _res: Response, next: NextFunction): Promise<void> => {
    if (!req.uid) {
      throw ApiError.unauthorized();
    }

    const userDoc = await getFirestore().collection("users").doc(req.uid).get();
    const role = userDoc.exists ? (userDoc.data()?.role as UserRole | undefined) : undefined;

    if (role !== "admin") {
      throw ApiError.forbidden("This endpoint requires an admin account.");
    }

    req.userRole = role;
    next();
  },
);

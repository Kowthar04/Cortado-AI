import { Response } from "express";

const verifyIdToken = jest.fn();
const userDocGet = jest.fn();

jest.mock("../src/services/firebase", () => ({
  getAuth: () => ({ verifyIdToken }),
  getFirestore: () => ({
    collection: () => ({
      doc: () => ({ get: userDocGet }),
    }),
  }),
}));

import { requireAdmin, requireAuth, AuthenticatedRequest } from "../src/middleware/auth";
import { ApiError } from "../src/middleware/errors";

function mockReq(headers: Record<string, string> = {}): AuthenticatedRequest {
  return { headers } as unknown as AuthenticatedRequest;
}

function mockRes(): Response {
  return {} as Response;
}

describe("requireAuth", () => {
  it("rejects a request with no Authorization header", async () => {
    const req = mockReq();
    const next = jest.fn();
    await requireAuth(req, mockRes(), next);
    expect(next).toHaveBeenCalledWith(expect.any(ApiError));
    expect((next.mock.calls[0][0] as ApiError).statusCode).toBe(401);
  });

  it("rejects a malformed Authorization header", async () => {
    const req = mockReq({ authorization: "Basic abc123" });
    const next = jest.fn();
    await requireAuth(req, mockRes(), next);
    expect((next.mock.calls[0][0] as ApiError).statusCode).toBe(401);
  });

  it("rejects an invalid token", async () => {
    verifyIdToken.mockRejectedValueOnce(new Error("invalid token"));
    const req = mockReq({ authorization: "Bearer bad-token" });
    const next = jest.fn();
    await requireAuth(req, mockRes(), next);
    expect((next.mock.calls[0][0] as ApiError).statusCode).toBe(401);
  });

  it("attaches uid and calls next for a valid token", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-123" });
    const req = mockReq({ authorization: "Bearer good-token" });
    const next = jest.fn();
    await requireAuth(req, mockRes(), next);
    expect(req.uid).toBe("user-123");
    expect(next).toHaveBeenCalledWith();
  });
});

describe("requireAdmin", () => {
  it("rejects when the caller is not authenticated first", async () => {
    const req = mockReq();
    const next = jest.fn();
    await requireAdmin(req, mockRes(), next);
    expect((next.mock.calls[0][0] as ApiError).statusCode).toBe(401);
  });

  it("rejects a non-admin user", async () => {
    userDocGet.mockResolvedValueOnce({ exists: true, data: () => ({ role: "customer" }) });
    const req = mockReq();
    req.uid = "user-123";
    const next = jest.fn();
    await requireAdmin(req, mockRes(), next);
    expect((next.mock.calls[0][0] as ApiError).statusCode).toBe(403);
  });

  it("allows an admin user", async () => {
    userDocGet.mockResolvedValueOnce({ exists: true, data: () => ({ role: "admin" }) });
    const req = mockReq();
    req.uid = "admin-1";
    const next = jest.fn();
    await requireAdmin(req, mockRes(), next);
    expect(next).toHaveBeenCalledWith();
  });
});

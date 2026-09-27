import request from "supertest";

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

const getMenuItems = jest.fn();
jest.mock("../src/services/menuService", () => ({
  getMenuItems: () => getMenuItems(),
}));

const listOrders = jest.fn();
const getOrderById = jest.fn();
const getOrdersByUserId = jest.fn();
jest.mock("../src/services/orderService", () => ({
  listOrders: () => listOrders(),
  getOrderById: (id: string) => getOrderById(id),
  getOrdersByUserId: (userId: string, limit: number) => getOrdersByUserId(userId, limit),
}));

const askGemini = jest.fn();
jest.mock("../src/services/geminiService", () => ({
  askGemini: (systemPrompt: string, messages: unknown) => askGemini(systemPrompt, messages),
}));

// eslint-disable-next-line @typescript-eslint/no-var-requires
import { createApp } from "../src/app";

const app = createApp();

beforeEach(() => {
  verifyIdToken.mockReset();
  userDocGet.mockReset();
  getMenuItems.mockReset();
  listOrders.mockReset();
  getOrderById.mockReset();
  getOrdersByUserId.mockReset();
  askGemini.mockReset();
});

describe("GET /api/menu", () => {
  it("returns 401 with no auth header", async () => {
    const res = await request(app).get("/api/menu");
    expect(res.status).toBe(401);
    expect(res.body.error.code).toBe("UNAUTHORIZED");
  });

  it("returns the menu for an authenticated user", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });
    getMenuItems.mockResolvedValueOnce([{ id: "1", name: "Latte" }]);

    const res = await request(app).get("/api/menu").set("Authorization", "Bearer good-token");

    expect(res.status).toBe(200);
    expect(res.body.menuItems).toHaveLength(1);
  });
});

describe("GET /api/orders", () => {
  it("returns 401 with no auth header", async () => {
    const res = await request(app).get("/api/orders");
    expect(res.status).toBe(401);
  });

  it("returns 403 for a non-admin user", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });
    userDocGet.mockResolvedValueOnce({ exists: true, data: () => ({ role: "customer" }) });

    const res = await request(app).get("/api/orders").set("Authorization", "Bearer good-token");

    expect(res.status).toBe(403);
    expect(res.body.error.code).toBe("FORBIDDEN");
  });

  it("returns orders for an admin user", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "admin-1" });
    userDocGet.mockResolvedValueOnce({ exists: true, data: () => ({ role: "admin" }) });
    listOrders.mockResolvedValueOnce([{ id: "order-1", status: "Pending" }]);

    const res = await request(app).get("/api/orders").set("Authorization", "Bearer good-token");

    expect(res.status).toBe(200);
    expect(res.body.orders).toHaveLength(1);
  });
});

describe("GET /api/orders/:id", () => {
  it("returns 404 when the order does not exist", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });
    getOrderById.mockResolvedValueOnce(null);

    const res = await request(app)
      .get("/api/orders/missing-id")
      .set("Authorization", "Bearer good-token");

    expect(res.status).toBe(404);
    expect(res.body.error.code).toBe("NOT_FOUND");
  });

  it("returns the order when found", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });
    getOrderById.mockResolvedValueOnce({ id: "order-1", status: "Ready" });

    const res = await request(app)
      .get("/api/orders/order-1")
      .set("Authorization", "Bearer good-token");

    expect(res.status).toBe(200);
    expect(res.body.order.status).toBe("Ready");
  });
});

describe("POST /api/chat", () => {
  it("returns 400 when the message is missing", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });

    const res = await request(app)
      .post("/api/chat")
      .set("Authorization", "Bearer good-token")
      .send({});

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe("BAD_REQUEST");
  });

  it("grounds the reply in Firestore menu data and returns Gemini's reply", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });
    getMenuItems.mockResolvedValueOnce([
      { id: "1", name: "Latte", category: "Coffee", price: 4.5, availability: true },
    ]);
    askGemini.mockResolvedValueOnce("We have a delicious Latte for $4.50!");

    const res = await request(app)
      .post("/api/chat")
      .set("Authorization", "Bearer good-token")
      .send({ message: "What coffee do you have?" });

    expect(res.status).toBe(200);
    expect(res.body.reply).toBe("We have a delicious Latte for $4.50!");
    expect(askGemini).toHaveBeenCalledTimes(1);
    const [systemPrompt] = askGemini.mock.calls[0];
    expect(systemPrompt).toContain("Latte");
  });

  it("fetches order history when a userId is provided", async () => {
    verifyIdToken.mockResolvedValueOnce({ uid: "user-1" });
    getMenuItems.mockResolvedValueOnce([]);
    getOrdersByUserId.mockResolvedValueOnce([
      { id: "order-1", totalPrice: 5, status: "Delivered", items: [] },
    ]);
    askGemini.mockResolvedValueOnce("Your last order was delivered.");

    const res = await request(app)
      .post("/api/chat")
      .set("Authorization", "Bearer good-token")
      .send({ message: "What was my last order?", userId: "user-1" });

    expect(res.status).toBe(200);
    expect(getOrdersByUserId).toHaveBeenCalledWith("user-1", expect.any(Number));
  });
});

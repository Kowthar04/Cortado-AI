import {
  buildMessages,
  buildSystemPrompt,
  formatMenuForPrompt,
  formatOrderHistoryForPrompt,
  formatOrderStatusForPrompt,
} from "../src/services/promptBuilder";
import { MenuItem, Order } from "../src/types/models";

const sampleMenu: MenuItem[] = [
  { id: "1", name: "Latte", category: "Coffee", price: 4.5, imageUrl: "", availability: true },
  { id: "2", name: "Espresso", category: "Coffee", price: 3, imageUrl: "", availability: false },
  { id: "3", name: "Croissant", category: "Bakery", price: 3.25, imageUrl: "", availability: true },
];

const sampleOrder: Order = {
  id: "order-1",
  userId: "user-1",
  customerName: "Jane Doe",
  items: [{ name: "Latte" }, { name: "Croissant" }],
  subtotal: 7.75,
  serviceFee: 0.5,
  discount: 0,
  totalPrice: 8.25,
  status: "Preparing",
  paymentStatus: "Paid",
};

describe("formatMenuForPrompt", () => {
  it("groups items by category and marks availability", () => {
    const result = formatMenuForPrompt(sampleMenu);
    expect(result).toContain("Coffee:");
    expect(result).toContain("Bakery:");
    expect(result).toContain("Latte ($4.50) — available");
    expect(result).toContain("Espresso ($3.00) — currently unavailable");
  });

  it("handles an empty menu", () => {
    expect(formatMenuForPrompt([])).toBe("The menu is currently empty.");
  });
});

describe("formatOrderHistoryForPrompt", () => {
  it("summarizes past orders", () => {
    const result = formatOrderHistoryForPrompt([sampleOrder]);
    expect(result).toContain("order-1");
    expect(result).toContain("Latte, Croissant");
    expect(result).toContain("$8.25");
    expect(result).toContain("Preparing");
  });

  it("handles no history", () => {
    expect(formatOrderHistoryForPrompt([])).toBe("This customer has no past orders on file.");
  });
});

describe("formatOrderStatusForPrompt", () => {
  it("describes a found order", () => {
    const result = formatOrderStatusForPrompt(sampleOrder);
    expect(result).toContain("Jane Doe");
    expect(result).toContain("Preparing");
    expect(result).toContain("Paid");
  });

  it("handles a missing order", () => {
    expect(formatOrderStatusForPrompt(null)).toBe(
      "No matching order was found for the reference provided.",
    );
  });
});

describe("buildSystemPrompt", () => {
  it("includes the menu and grounds the assistant", () => {
    const prompt = buildSystemPrompt({ menuItems: sampleMenu });
    expect(prompt).toContain("CafeShop");
    expect(prompt).toContain("CURRENT MENU:");
    expect(prompt).toContain("Latte");
    expect(prompt).not.toContain("ORDER STATUS BEING ASKED ABOUT:");
  });

  it("includes order history when provided", () => {
    const prompt = buildSystemPrompt({ menuItems: sampleMenu, orderHistory: [sampleOrder] });
    expect(prompt).toContain("CUSTOMER'S RECENT ORDER HISTORY:");
    expect(prompt).toContain("order-1");
  });

  it("includes current order status when provided, even if null", () => {
    const prompt = buildSystemPrompt({ menuItems: sampleMenu, currentOrder: null });
    expect(prompt).toContain("ORDER STATUS BEING ASKED ABOUT:");
    expect(prompt).toContain("No matching order was found");
  });
});

describe("buildMessages", () => {
  it("appends the new user message to prior history", () => {
    const messages = buildMessages(
      [
        { role: "user", content: "Hi" },
        { role: "assistant", content: "Hello!" },
      ],
      "What's on the menu?",
    );
    expect(messages).toEqual([
      { role: "user", content: "Hi" },
      { role: "assistant", content: "Hello!" },
      { role: "user", content: "What's on the menu?" },
    ]);
  });

  it("works with no prior history", () => {
    const messages = buildMessages(undefined, "Hello");
    expect(messages).toEqual([{ role: "user", content: "Hello" }]);
  });
});

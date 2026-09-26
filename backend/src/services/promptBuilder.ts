import { ChatTurn, MenuItem, Order } from "../types/models";

/**
 * Pure, side-effect-free prompt construction so it can be unit tested
 * without hitting Firestore or the Anthropic API.
 */

function formatPrice(price: number): string {
  return `$${price.toFixed(2)}`;
}

export function formatMenuForPrompt(menuItems: MenuItem[]): string {
  if (menuItems.length === 0) {
    return "The menu is currently empty.";
  }

  const byCategory = new Map<string, MenuItem[]>();
  for (const item of menuItems) {
    const category = item.category || "Uncategorized";
    const list = byCategory.get(category) ?? [];
    list.push(item);
    byCategory.set(category, list);
  }

  const lines: string[] = [];
  for (const [category, items] of byCategory) {
    lines.push(`${category}:`);
    for (const item of items) {
      const availability = item.availability ? "available" : "currently unavailable";
      lines.push(`  - ${item.name} (${formatPrice(item.price)}) — ${availability}`);
    }
  }
  return lines.join("\n");
}

export function formatOrderHistoryForPrompt(orders: Order[]): string {
  if (orders.length === 0) {
    return "This customer has no past orders on file.";
  }

  return orders
    .map((order) => {
      const itemNames = order.items
        .map((item) => (item as { name?: string }).name)
        .filter((name): name is string => Boolean(name));
      const itemsSummary = itemNames.length > 0 ? itemNames.join(", ") : "items unavailable";
      return `  - Order ${order.id}: ${itemsSummary} — total ${formatPrice(order.totalPrice)}, status: ${order.status}`;
    })
    .join("\n");
}

export function formatOrderStatusForPrompt(order: Order | null): string {
  if (!order) {
    return "No matching order was found for the reference provided.";
  }
  return [
    `Order ${order.id}:`,
    `  Customer: ${order.customerName}`,
    `  Status: ${order.status}`,
    `  Payment status: ${order.paymentStatus ?? "unknown"}`,
    `  Total: ${formatPrice(order.totalPrice)}`,
  ].join("\n");
}

export interface SystemPromptInput {
  menuItems: MenuItem[];
  orderHistory?: Order[];
  currentOrder?: Order | null;
}

export function buildSystemPrompt(input: SystemPromptInput): string {
  const sections = [
    "You are the friendly AI ordering assistant for CafeShop, a cafe ordering app.",
    "Answer menu questions, recommend items, help customize an order, and explain order status in natural, concise language.",
    "Only recommend items that are marked available. Only reference menu items and prices from the CURRENT MENU below — never invent items or prices.",
    "",
    "CURRENT MENU:",
    formatMenuForPrompt(input.menuItems),
  ];

  if (input.orderHistory) {
    sections.push(
      "",
      "CUSTOMER'S RECENT ORDER HISTORY:",
      formatOrderHistoryForPrompt(input.orderHistory),
    );
  }

  if (input.currentOrder !== undefined) {
    sections.push(
      "",
      "ORDER STATUS BEING ASKED ABOUT:",
      formatOrderStatusForPrompt(input.currentOrder),
    );
  }

  return sections.join("\n");
}

export interface AnthropicMessage {
  role: "user" | "assistant";
  content: string;
}

export function buildMessages(
  conversationHistory: ChatTurn[] | undefined,
  message: string,
): AnthropicMessage[] {
  const history = (conversationHistory ?? []).map((turn) => ({
    role: turn.role,
    content: turn.content,
  }));
  return [...history, { role: "user" as const, content: message }];
}

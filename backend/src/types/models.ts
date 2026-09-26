/**
 * These mirror the Kotlin data classes used by the Android app so the API
 * reads/writes the same Firestore documents:
 *   app/src/main/java/com/example/cafeshopassignment/models/{MenuItem,Order,User,Review}.kt
 */

export interface MenuItem {
  id?: string;
  name: string;
  category: string;
  price: number;
  imageUrl: string;
  availability: boolean;
}

export interface OrderItem {
  [key: string]: unknown;
}

export interface Order {
  id: string;
  userId: string;
  customerName: string;
  items: OrderItem[];
  subtotal: number;
  serviceFee: number;
  discount: number;
  totalPrice: number;
  status: string;
  paymentMethod?: string;
  paymentStatus?: string;
  createdAt?: FirestoreTimestampLike | null;
}

export interface FirestoreTimestampLike {
  seconds: number;
  nanoseconds: number;
}

export type UserRole = "admin" | "customer";

export interface AppUser {
  userid: string;
  firstname: string;
  surname?: string;
  email: string;
  role: UserRole;
}

export interface Review {
  reviewId: string;
  orderId: string;
  customerId: string;
  customerName: string;
  rating: number;
  comment: string;
}

export interface ChatRequestBody {
  message: string;
  conversationHistory?: ChatTurn[];
  userId?: string;
  orderId?: string;
}

export interface ChatTurn {
  role: "user" | "assistant";
  content: string;
}

export interface ChatResponseBody {
  reply: string;
}

export interface ApiErrorBody {
  error: {
    message: string;
    code: string;
  };
}

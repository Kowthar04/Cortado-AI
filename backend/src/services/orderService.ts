import { firestore } from "firebase-admin";
import { getFirestore } from "./firebase";
import { FirestoreTimestampLike, Order } from "../types/models";

const ORDERS_COLLECTION = "orders";

function toTimestampLike(value: unknown): FirestoreTimestampLike | null {
  if (value instanceof firestore.Timestamp) {
    return { seconds: value.seconds, nanoseconds: value.nanoseconds };
  }
  return null;
}

function mapOrderDoc(
  doc: FirebaseFirestore.QueryDocumentSnapshot | FirebaseFirestore.DocumentSnapshot,
): Order {
  const data = doc.data() ?? {};
  return {
    id: doc.id,
    userId: (data.userId as string) ?? "",
    customerName: (data.customerName as string) ?? "",
    items: Array.isArray(data.items) ? data.items : [],
    subtotal: (data.subtotal as number) ?? 0,
    serviceFee: (data.serviceFee as number) ?? 0,
    discount: (data.discount as number) ?? 0,
    totalPrice: (data.totalPrice as number) ?? 0,
    status: (data.status as string) ?? "Pending",
    paymentMethod: data.paymentMethod as string | undefined,
    paymentStatus: data.paymentStatus as string | undefined,
    createdAt: toTimestampLike(data.createdAt),
  };
}

export async function listOrders(limit = 100): Promise<Order[]> {
  const snapshot = await getFirestore()
    .collection(ORDERS_COLLECTION)
    .orderBy("createdAt", "desc")
    .limit(limit)
    .get();
  return snapshot.docs.map(mapOrderDoc);
}

export async function getOrderById(orderId: string): Promise<Order | null> {
  const doc = await getFirestore().collection(ORDERS_COLLECTION).doc(orderId).get();
  if (!doc.exists) {
    return null;
  }
  return mapOrderDoc(doc);
}

export async function getOrdersByUserId(userId: string, limit = 5): Promise<Order[]> {
  const snapshot = await getFirestore()
    .collection(ORDERS_COLLECTION)
    .where("userId", "==", userId)
    .orderBy("createdAt", "desc")
    .limit(limit)
    .get();
  return snapshot.docs.map(mapOrderDoc);
}

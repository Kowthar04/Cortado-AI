import { getFirestore } from "./firebase";
import { MenuItem } from "../types/models";

const MENU_COLLECTION = "menuItems";

export async function getMenuItems(): Promise<MenuItem[]> {
  const snapshot = await getFirestore().collection(MENU_COLLECTION).get();
  return snapshot.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      name: (data.name as string) ?? "",
      category: (data.category as string) ?? "",
      price: (data.price as number) ?? 0,
      imageUrl: (data.imageUrl as string) ?? "",
      availability: (data.availability as boolean) ?? true,
    };
  });
}

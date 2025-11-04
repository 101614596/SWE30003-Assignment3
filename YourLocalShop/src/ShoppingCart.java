import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    private List<CartItem> items = new ArrayList<>();
    private InventoryManager inventory;

    public ShoppingCart(InventoryManager inventory) {
        this.inventory = inventory;
    }

    public void addItem(Product product, int quantity) {

        cleanupExpiredReservation();
        int available = inventory.getStock(product.getId());

        if (available < quantity) {
            System.out.println("Not enough stock for: " + product.getName());
            return;
        }

        // Try to reserve stock first
        if (inventory.reduceStock(product.getId(), quantity)) {
            for (CartItem item : items) {
                if (item.getProduct().getId().equals(product.getId())) {
                    item.setQuantity(item.getQuantity() + quantity);
                    System.out.println(quantity + "x " + product.getName() + " added to cart.");
                    return;
                }
            }
            items.add(new CartItem(product, quantity));
            System.out.println(quantity + "x " + product.getName() + " added to cart.");
        } else {
            System.out.println("Unable to reserve stock for: " + product.getName());
        }
    }

    private void cleanupExpiredReservation() {
        List<CartItem> expired = new  ArrayList<>();

        for (CartItem item:items){
            if (item.isReservationExpired()){
                expired.add(item) ;
            }
        } 

    }

    public InventoryManager getInventory(){
        return inventory; 
    }


    public void removeItem(String productId) {
        CartItem target = null;

        for (CartItem item : items) {
            if (item.getProduct().getId().equals(productId)) {
                target = item;
                break;
            }
        }

        if (target != null) {
            items.remove(target);
            inventory.restock(productId, target.getQuantity());
            System.out.println("Removed " + target.getProduct().getName() + " and restored stock.");
        } else {
            System.out.println("Item not found in cart.");
        }
    }

    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    public void displayCartAndCheckout() {
        System.out.println("\n=== Shopping Cart ===");
        if (items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        System.out.printf("%-8s %-20s %-10s %-10s%n", "ID", "Product", "Qty", "Subtotal");
        System.out.println("-----------------------------------------------------");

        for (CartItem item : items) {
            System.out.printf("%-8s %-20s %-10d $%-10.2f%n",
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getSubtotal());
        }

        System.out.println("-----------------------------------------------------");
        System.out.printf("Total: $%.2f%n", getTotal());
    }

    public void clearCart() {
        items.clear();
    }

    public List<CartItem> getItems() {
        return items;
    }
}
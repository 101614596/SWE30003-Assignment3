import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {

    private final List<CartItem> items = new ArrayList<>();
    private final InventoryManager inventory;

    public ShoppingCart(InventoryManager inventory) {
        this.inventory = inventory;
    }

    public void addItem(Product product, int quantity) {
        if (product == null) {
            System.out.println("Invalid product.");
            return;
        }

        int available = inventory.getStock(product.getId());
        if (available <= 0) {
            System.out.println("Product out of stock.");
            return;
        }

        if (quantity > available) {
            System.out.println("Only " + available + " units available. Adding " + available + " instead.");
            quantity = available;
        }

        // Check if item already exists in cart
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                item.setQuantity(item.getQuantity() + quantity);
                System.out.println(quantity + "x " + product.getName() + " added to cart.");
                return;
            }
        }

        // Add as new item
        items.add(new CartItem(product, quantity));
        System.out.println("Added " + quantity + " x " + product.getName() + " to cart.");
    }

    public void removeItem(String productId) {
        CartItem toRemove = null;
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(productId)) {
                toRemove = item;
                break;
            }
        }

        if (toRemove != null) {
            items.remove(toRemove);
            System.out.println("Removed " + toRemove.getProduct().getName() + " from cart.");
        } else {
            System.out.println("Item not found in cart.");
        }
    }

    public void displayCartAndCheckout() {
        System.out.println("\n=== Shopping Cart ===");
        if (items.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }

        System.out.printf("%-8s %-20s %-10s %-10s%n", "ID", "Product", "Qty", "Subtotal");
        System.out.println("-----------------------------------------------------");

        double total = 0;
        for (CartItem item : items) {
            double subtotal = item.getProduct().getPrice() * item.getQuantity();
            total += subtotal;
            System.out.printf("%-8s %-20s %-10d $%-10.2f%n",
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    subtotal);
        }

        System.out.println("-----------------------------------------------------");
        System.out.printf("Total: $%.2f%n", total);
    }

    public double getTotal() {
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void clearCart() {
        items.clear();
    }

    public InventoryManager getInventory() {
        return inventory;
    }
}

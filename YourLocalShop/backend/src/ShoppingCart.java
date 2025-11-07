import java.util.ArrayList;
import java.util.List;
import exceptions.InsufficientStockException;

public class ShoppingCart {

    private final List<CartItem> items = new ArrayList<>();
    private final InventoryManager inventory;

    public ShoppingCart(InventoryManager inventory) {
        this.inventory = inventory;
    }


    public void addItem(Product product, int quantity) throws InsufficientStockException {
        if (product == null) {
            throw new IllegalArgumentException("Invalid product.");
        }

        cleanExpiredItems();

        int available = inventory.getStock(product.getId());


        if (available <= 0) {
            throw new InsufficientStockException(product.getId(), quantity, 0);
        }

        if (quantity > available) {
            throw new InsufficientStockException(product.getId(), quantity, available);
        }

        // Check if item already exists in cart
        for (CartItem item : items) {
            if (item.getProduct().getId().equals(product.getId())) {
                if (item.isReservationExpired()) {
                    inventory.releaseReservation(product.getId(), item.getQuantity());
                    items.remove(item);
                    break;
                }

                int additionalQuantity = quantity;
                int newTotal = item.getQuantity() + additionalQuantity;

                if (inventory.reserveStock(product.getId(), additionalQuantity)) {
                    item.setQuantity(newTotal);
                    item.refreshReservation();
                    System.out.println(quantity + "x " + product.getName() + " added to cart.");
                    return;
                } else {
                    throw new InsufficientStockException(product.getId(), additionalQuantity,
                            inventory.getStock(product.getId()));
                }
            }
        }

        // Add as new item
        if (inventory.reserveStock(product.getId(), quantity)) {
            items.add(new CartItem(product, quantity));
            System.out.println("Added " + quantity + " x " + product.getName() + " to cart.");
        } else {
            throw new InsufficientStockException(product.getId(), quantity,
                    inventory.getStock(product.getId()));
        }
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
            // Release reservation
            inventory.releaseReservation(productId, toRemove.getQuantity());
            items.remove(toRemove);
            System.out.println("Removed " + toRemove.getProduct().getName() + " from cart.");
        } else {
            System.out.println("Item not found in cart.");
        }
    }

    public void cleanExpiredItems() {
        List<CartItem> expired = new ArrayList<>();
        for (CartItem item : items) {
            if (item.isReservationExpired()) {
                expired.add(item);
                // Release reserved stock
                inventory.releaseReservation(item.getProduct().getId(), item.getQuantity());
                System.out.println("Removed expired item: " + item.getProduct().getName());
            }
        }
        items.removeAll(expired);
    }

    public void displayCartAndCheckout() {
        cleanExpiredItems();

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
        cleanExpiredItems();
        double total = 0;
        for (CartItem item : items) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }
        return total;
    }

    public List<CartItem> getItems() {return items;}

    public void clearCart() {
        for ( CartItem item : items) {
            inventory.restock(item.getProduct().getId(), item.getQuantity());
        }
        items.clear();
    }

    public InventoryManager getInventory() {
        return inventory;
    }
}
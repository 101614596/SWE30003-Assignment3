import java.util.HashMap;
import java.util.Map;

public class InventoryManager {
    private Map<String, Integer> stockLevels = new HashMap<>();
    private ProductCatalog catalog;

    public InventoryManager(ProductCatalog catalog) {
        this.catalog = catalog;
        initializeStock();
    }

    // Load initial stock from ProductCatalog
    private void initializeStock() {
        for (Product p : catalog.getAllProducts()) {
            stockLevels.put(p.getId(), p.getQuantity());
        }
    }

    // Get current stock level
    public int getStock(String productId) {
        return stockLevels.getOrDefault(productId, 0);
    }

    // Reduce stock when an order is placed
    public boolean reduceStock(String productId, int quantity) {
        int current = getStock(productId);
        if (current >= quantity) {
            stockLevels.put(productId, current - quantity);
            catalog.getProductById(productId).setQuantity(current - quantity);
            return true;
        }
        return false;
    }

    // Restock or add more units
    public void restock(String productId, int amount) {
        int current = getStock(productId);
        stockLevels.put(productId, current + amount);
        catalog.getProductById(productId).setQuantity(current + amount);
    }

    // Print inventory report
    public void displayInventory() {
        System.out.println("\n=== Inventory Report ===");
        for (Product p : catalog.getAllProducts()) {
            System.out.printf("%s - %d in stock%n", p.getName(), getStock(p.getId()));
        }
    }
}

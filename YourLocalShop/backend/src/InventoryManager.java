import java.util.HashMap;
import java.util.Map;

public class InventoryManager {
    private Map<String, Integer> stockLevels = new HashMap<>();
    private Map<String, Integer> reservedStock = new HashMap<>();
    private ProductCatalog catalog;

    public InventoryManager(ProductCatalog catalog) {
        this.catalog = catalog;
        initializeStock();
    }

    // Load initial stock from ProductCatalog
    private void initializeStock() {
        for (Product p : catalog.getAllProducts()) {
            stockLevels.put(p.getId(), p.getQuantity());
            reservedStock.put(p.getId(), 0);
        }
    }

    // Get current stock level
    public int getStock(String productId) {

        int total = stockLevels.getOrDefault(productId, 0);
        int reserved = reservedStock.getOrDefault(productId, 0);

        return Math.max(0 ,total- reserved);
    }

    public int getTotalStock(String productId) {
        return stockLevels.getOrDefault(productId, 0);
    }

    public boolean reserveStock(String productId, int quantity) {
        int available = getStock(productId);
        if (available >= quantity) {
            int currentReserved = reservedStock.getOrDefault(productId, 0);
            reservedStock.put(productId, currentReserved + quantity);
            System.out.printf("Reserved %d units of %s%n", quantity, productId);
            return true;

        }
        return false;
    }

    public void releaseReservation(String productId, int quantity) {
        int currentReserved = reservedStock.getOrDefault(productId, 0);
        int newReserved = Math.max(0, currentReserved - quantity);
        reservedStock.put(productId, newReserved);
        System.out.printf("Released %d units of %s%n", quantity, productId);
    }


    // Reduce stock when an order is placed
    public boolean reduceStock(String productId, int quantity) {
        int total = getTotalStock(productId);
        if (total >= quantity) {
            // Reduce both total and reserved
            stockLevels.put(productId, total - quantity);

            int reserved = reservedStock.getOrDefault(productId, 0);
            reservedStock.put(productId, Math.max(0, reserved - quantity));

            // Update product object
            Product product = catalog.getProductById(productId);
            if (product != null) {
                product.setQuantity(total - quantity);
            }
            return true;
        }
        return false;
    }

    // Restock or add more units
    public void restock(String productId, int amount) {
        int current = getTotalStock(productId);
        stockLevels.put(productId, current + amount);

        Product product = catalog.getProductById(productId);
        if (product != null) {
            product.setQuantity(current + amount);
        }
    }
    // Print inventory report
    public void displayInventory() {
        System.out.println("\n=== Inventory Report ===");
        System.out.printf("%-10s %-20s %-10s %-10s %-10s%n",
                "ID", "Product", "Total", "Reserved", "Available");
        System.out.println("----------------------------------------------------------------");

        for (Product p : catalog.getAllProducts()) {
            int total = getTotalStock(p.getId());
            int reserved = reservedStock.getOrDefault(p.getId(), 0);
            int available = getStock(p.getId());

            System.out.printf("%-10s %-20s %-10d %-10d %-10d%n",
                    p.getId(), p.getName(), total, reserved, available);
        }
        System.out.println("================================================================");
    }
}

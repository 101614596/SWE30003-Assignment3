import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticReport {
    private List<Order> orders;
    private ProductCatalog catalog;
    private InventoryManager inventory;

    public StatisticReport(List<Order> orders, ProductCatalog catalog, InventoryManager inventory) {
        this.orders = orders;
        this.catalog = catalog;
        this.inventory = inventory;
    }

    // Generates total sales and order count
    public void generateSalesReport() {
        double totalRevenue = 0;
        int totalOrders = orders.size();
        Map<String, Integer> productSales = new HashMap<>();

        for (Order order : orders) {
            totalRevenue += order.getTotalPrice();
            for (OrderItem item : order.getItems()) {
                String productName = item.getProduct().getName();
                productSales.put(productName,
                        productSales.getOrDefault(productName, 0) + item.getQuantity());
            }
        }

        System.out.println("=== Sales Report ===");
        System.out.println("Total Orders: " + totalOrders);
        System.out.println("Total Revenue: $" + String.format("%.2f", totalRevenue));
        System.out.println("\nTop Selling Products:");
        productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> System.out.println(e.getKey() + " - " + e.getValue() + " sold"));
    }

    // Show inventory insights
    public void generateStockSummary() {
        System.out.println("\n=== Inventory Summary ===");
        for (Product p : catalog.getProducts()) {
            int stock = inventory.getStock(p.getId());
            if (stock < 5)
                System.out.println(p.getName() + " - LOW STOCK (" + stock + ")");
        }
    }
}

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;


public class OrderProcessor {


    // === Observer pattern support ===
    private final List<Observer> observers = new ArrayList<>();


    public void registerObserver(Observer observer) {
        observers.add(observer);
    }


    public void notifyObservers(Object event) {
        for (Observer observer : observers) {
            observer.notifyUpdate(event);
        }
    }


    // === Core functionality ===
    private final InventoryManager inventory;
    private final ProductCatalog catalog;


    public OrderProcessor(InventoryManager inventory, ProductCatalog catalog) {
        this.inventory = inventory;
        this.catalog = catalog;
    }


    public boolean processOrder(Order order) {
        System.out.println("\n=== Processing Order " + order.getOrderId() + " ===");


        // Step 1: Check stock availability
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            int currentStock = inventory.getStock(p.getId());


            if (currentStock < item.getQuantity()) {
                System.out.println("❌ Not enough stock for " + p.getName());
                return false;
            }
        }


        // Step 2: Deduct stock
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            inventory.decreaseStock(p.getId(), item.getQuantity());
        }


        // Step 3: Calculate total and confirm payment (simulation)
        double total = order.getTotalPrice();
        System.out.println("💳 Payment of $" + total + " processed successfully.");


        // Step 4: Generate invoice and shipment
        Invoice invoice = new Invoice(order);
        Shipment shipment = new Shipment(order);


        System.out.println("📄 Invoice generated: " + invoice.getInvoiceId());
        System.out.println("🚚 Shipment created for order " + order.getOrderId());


        // Step 5: Notify observers (StatisticsGenerator, etc.)
        notifyObservers(order);


        System.out.println("✅ Order " + order.getOrderId() + " completed.\n");
        return true;
    }
}
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private CustomerAccount customer;
    private List<OrderItem> items;
    private double subtotal;
    private double tax;
    private double total;
    private ShipmentStatus status;
    private LocalDateTime orderDate;

    public Order(String orderId, CustomerAccount customer) {
        this.orderId = orderId;
        this.customer = customer;
        this.items = new ArrayList<>();
        this.status = ShipmentStatus.PENDING;
        this.orderDate = LocalDateTime.now();
    }

    // Add or remove products
    public void addProduct(Product product, int quantity) {
        if (!product.isAvailable() || product.getQuantity() < quantity) {
            throw new IllegalStateException("Product not available or insufficient stock.");
        }
        items.add(new OrderItem(product, quantity));
        updateTotals();
    }

    public void removeProduct(Product product) {
        items.removeIf(item -> item.getProduct().getId().equals(product.getId()));
        updateTotals();
    }

    // Calculations
    private void updateTotals() {
        subtotal = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        tax = subtotal * 0.10; // Example: 10% tax
        total = subtotal + tax;
    }

    //Confirm and generate invoice
    public Invoice generateInvoice() {
        if (status != ShipmentStatus.CONFIRMED)
            throw new IllegalStateException("Order must be confirmed before generating invoice.");

        return new InvoiceBuilder()
                .setOrder(this)
                .setCustomer(customer)
                .build();
    }

    public void confirmOrder() {
        if (items.isEmpty()) throw new IllegalStateException("Cannot confirm an empty order.");
        this.status = ShipmentStatus.CONFIRMED;
    }

    //Getters
    public String getOrderId() { return orderId; }
    public List<OrderItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public double getTax() { return tax; }
    public double getTotal() { return total; }
    public ShipmentStatus getStatus() { return status; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public CustomerAccount getCustomer() { return customer; }

    @Override
    public String toString() {
        return String.format(
            "Order [%s] - Customer: %s | Status: %s | Total: $%.2f | Date: %s",
            orderId, customer.getUsername(), status, total, orderDate
        );
    }
}


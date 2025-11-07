import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import exceptions.InsufficientStockException;
import exceptions.PaymentProcessException;

public class OrderProcessor {

    private final InventoryManager inventory;

    public OrderProcessor(InventoryManager inventory) {
        this.inventory = inventory;
    }

    private final List<Observer> observers = new ArrayList<>();

    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyObservers(Object event) {
        for (Observer observer : observers) {
            observer.notifyUpdate(event);
        }
    }

    public void processOrder(Order order) {
        // your normal order processing logic here…
        System.out.println("Processing order: " + order.getOrderId());
        // Notify observers (invoice, shipment, statistics, etc.)
        notifyObservers(order);
    }

    /**
     * Process the current cart into a paid order with shipment and invoice.
     * - Validates stock
     * - Builds and confirms Order
     * - Charges PaymentMethod
     * - Reduces stock
     * - Creates Shipment and Invoice
     * - Clears cart
     */
    // MODIFIED METHOD - now throws exceptions
    public Invoice process(ShoppingCart cart, CustomerAccount customer, PaymentMethod paymentMethod)
            throws InsufficientStockException, PaymentProcessException {

        if (cart == null || customer == null || paymentMethod == null) {
            throw new IllegalArgumentException("Cart, customer or payment method cannot be null.");
        }

        cart.cleanExpiredItems();

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty.");
        }

        // 1) Validate stock first - REPLACE lines 47-52 with exception throwing
        for (CartItem ci : cart.getItems()) {
            int available = cart.getInventory().getStock(ci.getProduct().getId());
            if (available < ci.getQuantity()) {
                throw new InsufficientStockException(
                        ci.getProduct().getId(),
                        ci.getQuantity(),
                        available
                );
            }
        }

        // 2) Create Order using Builder pattern
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrderBuilder orderBuilder = new OrderBuilder()
                .setOrderId(orderId)
                .setCustomer(customer);

        // Add all cart items to the builder
        for (CartItem ci : cart.getItems()) {
            orderBuilder.addItem(ci.getProduct(), ci.getQuantity());
        }

        Order order;
        try {
            order = orderBuilder.build();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to create order: " + e.getMessage());
        }

        // 3) Confirm order (required before invoice)
        try {
            order.confirmOrder();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("Failed to confirm order: " + e.getMessage());
        }

        // 4) Charge payment on final total - REPLACE lines 70-73 with exception throwing
        double amount = order.getTotal();
        boolean paid = paymentMethod.processPayment(amount);
        if (!paid) {
            throw new PaymentProcessException("Payment failed for amount: $" +
                    String.format("%.2f", amount));
        }

        // 5) Reduce stock after successful payment
        for (CartItem ci : cart.getItems()) {
            boolean reduced = inventory.reduceStock(ci.getProduct().getId(), ci.getQuantity());
            if (!reduced) {
                System.out.println("Stock reduction failed: " + ci.getProduct().getName());
            } else {
                System.out.printf("Reduced stock: %s - %d%n", ci.getProduct().getName(), ci.getQuantity());
            }
        }

        // 6) Create shipment using Builder pattern
        String shipmentId = "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Shipment shipment = new ShipmentBuilder()
                .setShipmentId(shipmentId)
                .setOrder(order)
                .setDeliveryAddress(customer.getAddress())
                .setCarrier("AUSPOST")
                .build();

        shipment.updateStatus(ShipmentStatus.CONFIRMED);

        // 7) Create invoice and attach to customer history
        Invoice invoice = new InvoiceBuilder()
                .setOrder(order)
                .setCustomer(customer)
                .setShipment(shipment)
                .build();

        customer.addOrder(order);
        customer.addInvoice(invoice);

        // 8) Clear cart
        cart.clearCart();

        return invoice;
    }
}
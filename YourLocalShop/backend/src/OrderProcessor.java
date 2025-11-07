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
    public void processOrder(Order order, Shipment shipment, Invoice invoice) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        System.out.println("Processing order: " + order.getOrderId());

        // 1. Save order to database
        saveOrderToDatabase(order);

        // 2. Save order items to database
        saveOrderItemsToDatabase(order);

        // 3. Update/save shipment
        if (shipment != null) {
            saveShipmentToDatabase(shipment);
            shipment.dispatch(); // Move to dispatched status
            updateShipmentInDatabase(shipment);
        }

        // 4. Save invoice
        if (invoice != null) {
            saveInvoiceToDatabase(invoice);
        }

        // 5. Notify observers (statistics generator, email service, etc.)
        notifyObservers(order);

        System.out.println("Order " + order.getOrderId() + " processed successfully");
    }

    private void saveOrderToDatabase(Order order) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String sql = "INSERT INTO orders (order_id, customer_username, subtotal, tax, total, status, order_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, order.getOrderId());
            ps.setString(2, order.getCustomer().getUsername());
            ps.setDouble(3, order.getSubtotal());
            ps.setDouble(4, order.getTax());
            ps.setDouble(5, order.getTotal());
            ps.setString(6, order.getStatus().toString());
            ps.setString(7, order.getOrderDate().toString());
            ps.executeUpdate();
            System.out.println("Order saved to database: " + order.getOrderId());
        } catch (Exception e) {
            System.err.println("Failed to save order: " + e.getMessage());
            throw new RuntimeException("Database error while saving order", e);
        }
    }

    private void saveOrderItemsToDatabase(Order order) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price, subtotal) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            for (OrderItem item : order.getItems()) {
                ps.setString(1, order.getOrderId());
                ps.setString(2, item.getProduct().getId());
                ps.setString(3, item.getProduct().getName());
                ps.setInt(4, item.getQuantity());
                ps.setDouble(5, item.getProduct().getPrice());
                ps.setDouble(6, item.getSubtotal());
                ps.executeUpdate();
            }
            System.out.println("Order items saved to database");
        } catch (Exception e) {
            System.err.println("Failed to save order items: " + e.getMessage());
            throw new RuntimeException("Database error while saving order items", e);
        }
    }

    private void saveShipmentToDatabase(Shipment shipment) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String sql = "INSERT INTO shipments (shipment_id, order_id, tracking_number, carrier, status, " +
                "dispatch_date, delivery_date, delivery_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, shipment.getShipmentId());
            ps.setString(2, shipment.getOrder().getOrderId());
            ps.setString(3, shipment.getTrackingNumber());
            ps.setString(4, shipment.getCarrier());
            ps.setString(5, shipment.getStatus().toString());
            ps.setString(6, shipment.getDispatchDate() != null ? shipment.getDispatchDate().toString() : null);
            ps.setString(7, shipment.getDeliveryDate() != null ? shipment.getDeliveryDate().toString() : null);
            ps.setString(8, shipment.getDeliveryAddress());
            ps.executeUpdate();
            System.out.println("Shipment saved to database: " + shipment.getTrackingNumber());
        } catch (Exception e) {
            System.err.println("Failed to save shipment: " + e.getMessage());
            throw new RuntimeException("Database error while saving shipment", e);
        }
    }

    private void updateShipmentInDatabase(Shipment shipment) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String sql = "UPDATE shipments SET status = ?, dispatch_date = ?, delivery_date = ? WHERE shipment_id = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, shipment.getStatus().toString());
            ps.setString(2, shipment.getDispatchDate() != null ? shipment.getDispatchDate().toString() : null);
            ps.setString(3, shipment.getDeliveryDate() != null ? shipment.getDeliveryDate().toString() : null);
            ps.setString(4, shipment.getShipmentId());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update shipment: " + e.getMessage());
        }
    }

    private void saveInvoiceToDatabase(Invoice invoice) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String sql = "INSERT INTO invoices (order_id, customer_username, invoice_date) VALUES (?, ?, ?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, invoice.getOrder().getOrderId());
            ps.setString(2, invoice.getCustomer().getUsername());
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
            System.out.println("Invoice saved to database");
        } catch (Exception e) {
            System.err.println("Failed to save invoice: " + e.getMessage());
            throw new RuntimeException("Database error while saving invoice", e);
        }
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

            inventory.releaseReservation(ci.getProduct().getId(), ci.getQuantity());

            boolean reduced = inventory.reduceStock(ci.getProduct().getId(), ci.getQuantity());
            if (!reduced) {
                throw new IllegalStateException("Failed to reduce stock for product: " + ci.getProduct().getId());
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
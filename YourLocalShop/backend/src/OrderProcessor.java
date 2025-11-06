import java.util.UUID;

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
    public Invoice process(ShoppingCart cart, CustomerAccount customer, PaymentMethod paymentMethod) {
        if (cart == null || customer == null || paymentMethod == null) {
            System.out.println("OrderProcessor: missing cart, customer or payment method.");
            return null;
        }

        cart.cleanExpiredItems();

        if (cart.getItems().isEmpty()) {
            System.out.println("OrderProcessor: cart is empty.");
            return null;
        }

        // 1) Validate stock first
        for (CartItem ci : cart.getItems()) {
            int available = cart.getInventory().getStock(ci.getProduct().getId());
            if (available < ci.getQuantity()) {
                System.out.printf("OrderProcessor: insufficient stock for %s (want %d, have %d)%n",
                        ci.getProduct().getName(), ci.getQuantity(), available);
                return null;
            }
        }

        // 2) Create Order and add items
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderId, customer);

        try{
            for (CartItem ci : cart.getItems()) {
                order.addProduct(ci.getProduct(), ci.getQuantity());
            }
        }catch(IllegalStateException e){
            System.out.println("Failed to create order." + e.getMessage());
            return null;
        }


        // 3) Confirm order (required before invoice)
        try{
            order.confirmOrder();
        }catch (IllegalStateException e){
            System.out.println("Failed to create order." + e.getMessage());
            return null;
        }

        // 4) Charge payment on final total
        double amount = order.getTotal();
        boolean paid = paymentMethod.processPayment(amount);
        if (!paid) {
            System.out.println("OrderProcessor: payment failed.");
            return null;
        }

        // 5) Reduce stock after successful payment
        for (CartItem ci : cart.getItems()) {
            boolean reduced= inventory.reduceStock(ci.getProduct().getId(), ci.getQuantity());
            if(!reduced){
                System.out.println("Stock reduction failed." +ci.getProduct().getName());

            }else{
                System.out.printf("Reduced stock: %s - %d%n", ci.getProduct().getName(), ci.getQuantity());
            }


        }

        // 6) Create shipment
        String shipmentId = "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Shipment shipment = new Shipment(shipmentId, order, customer.getAddress(), "AUSPOST");
        shipment.updateStatus(ShipmentStatus.CONFIRMED);

        // 7) Create invoice and attach to customer history
        Invoice invoice = new Invoice(order, customer, shipment);
        customer.addOrder(order);
        customer.addInvoice(invoice);

        // 8) Clear cart
        cart.clearCart();

        return invoice;
    }
}

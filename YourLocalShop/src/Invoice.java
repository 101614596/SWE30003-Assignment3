import java.util.List;
import java.time.format.DateTimeFormatter;

public class Invoice {
    private Order order;
    private Customer customer;
    private Shipment shipment;

    public Invoice(Order order, Customer customer, Shipment shipment) {
        this.order = order;
        this.customer = customer;
        this.shipment = shipment;
    }

    public void displayInvoice() {
        System.out.println("========= INVOICE =========");
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Order Date: " + order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println();
        System.out.println("Customer: " + customer.getName());
        System.out.println("Email: " + customer.getEmail());
        System.out.println("Address: " + customer.getAddress());
        System.out.println();
        System.out.println("Items:");
        List<OrderItem> items = order.getItems();
        for (OrderItem item : items) {
            System.out.printf("- %s x%d  $%.2f%n",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getSubtotal());
        }
        System.out.println("----------------------------");
        System.out.printf("Subtotal: $%.2f%n", order.getSubtotal());
        System.out.println("Shipment: " + shipment.getTrackingNumber() + " via " + shipment.getCarrier());
        System.out.println("============================");
    }
}

    
}

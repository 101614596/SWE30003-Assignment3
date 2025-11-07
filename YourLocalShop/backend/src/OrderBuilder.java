import java.util.ArrayList;
import java.util.List;


public class OrderBuilder {
    private String orderId;
    private CustomerAccount customer;
    private List<OrderItemData> items = new ArrayList<>();

    // Helper class to hold item data before Order is created
    public static class OrderItemData {
        public Product product;
        public int quantity;

        public OrderItemData(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
    }

    public OrderBuilder setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderBuilder setCustomer(CustomerAccount customer) {
        this.customer = customer;
        return this;
    }

    public OrderBuilder addItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        items.add(new OrderItemData(product, quantity));
        return this;
    }

    public Order build() {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalStateException("Order ID is required");
        }
        if (customer == null) {
            throw new IllegalStateException("Customer is required");
        }

        Order order = new Order(orderId, customer);

        // Add all items to the order
        for (OrderItemData itemData : items) {
            order.addProduct(itemData.product, itemData.quantity);
        }

        return order;
    }
}
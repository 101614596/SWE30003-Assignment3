import java.time.LocalDateTime;

public class OrderItem {
    private Product product;
    private int quantity;
    private double subtotal;

    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.subtotal = product.getPrice() * quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getSubtotal() { return subtotal; }

    @Override
    public String toString() {
        return String.format("[%s] %s x%d = $%.2f",
                product.getId(), product.getName(), quantity, subtotal);
    }
}

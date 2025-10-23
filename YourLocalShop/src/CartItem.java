public class CartItem {
    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1.");
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s x%d = $%.2f",
                product.getId(), product.getName(), quantity, getSubtotal());
    }
}
public class Product {
    private String id;
    private String name;
    private String category;
    private String description;
    private double price;
    private int quantity;
    private boolean available;

    public Product(String id, String name, String category, String description, double price, int quantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.available = quantity > 0;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public boolean isAvailable() { return available; }

    // Setters
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.available = quantity > 0;
    }

    public void setPrice(double price) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative.");
        this.price = price;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - $%.2f [%d in stock]", name, category, price, quantity);
    }
}
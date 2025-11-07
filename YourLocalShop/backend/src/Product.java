import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Product {
    private String id;
    private String name;
    private String category;
    private String description;
    private double price;
    private int quantity;
    private boolean available;
    private double discountPercentage;

    public Product(String id, String name, String category, String description, double price, int quantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.available = quantity > 0;
        this.discountPercentage = 0.0;
    }


    public Product(String id, String name, String category, String description, double price, int quantity, double discountPercentage) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.available = quantity > 0;
        this.discountPercentage = discountPercentage;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public boolean isAvailable() { return available; }
    public double getDiscountPercentage() { return discountPercentage; }

    // Calculate discounted price
    public double getDiscountedPrice() {
        if (discountPercentage <= 0) {
            return price;
        }
        return price * (1 - discountPercentage / 100.0);
    }

    // Setters
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.available = quantity > 0;
        updateInDatabase();
    }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }

    public void setPrice(double price) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative.");
        this.price = price;
        updateInDatabase();
    }

    public void setDiscountPercentage(double discountPercentage) {
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100.");
        }
        this.discountPercentage = discountPercentage;
        updateInDatabase();
    }


    private void updateInDatabase() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        String updateSQL = "UPDATE products SET quantity = ?, available = ?, price = ?, discount_percentage = ? WHERE id = ?";
        try (PreparedStatement pstmt = db.getConnection().prepareStatement(updateSQL)){
            pstmt.setInt(1, this.quantity);
            pstmt.setInt(2, this.available ? 1: 0);
            pstmt.setDouble(3, this.price);
            pstmt.setDouble(4, this.discountPercentage);  // ADDED
            pstmt.setString(5, this.id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating product info in database: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        String priceInfo = String.format("$%.2f", price);
        if (discountPercentage > 0) {
            priceInfo = String.format("$%.2f (%.0f%% off - was $%.2f)",
                    getDiscountedPrice(), discountPercentage, price);
        }
        return String.format("[%s] %s (%s) - %s [%d in stock]",
                id, name, category, priceInfo, quantity);
    }
}
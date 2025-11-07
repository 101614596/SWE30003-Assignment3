import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.*;

public class Admin {

    private final ProductCatalog catalog;
    private final InventoryManager inventory;
    private final Scanner scanner = new Scanner(System.in);

    public Admin(ProductCatalog catalog, InventoryManager inventory) {
        this.catalog = catalog;
        this.inventory = inventory;
    }

    public void start() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== Admin ===");
            System.out.println("1. View items");
            System.out.println("2. Add item");
            System.out.println("3. Delete item");
            System.out.println("4. Edit item");
            System.out.println("5. Set discount");
            System.out.println("6. Remove discount");
            System.out.println("0. Back");
            System.out.print("Select: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "1" -> catalog.displayAll();
                case "2" -> adminAddItem();
                case "3" -> adminDeleteItem();
                case "4" -> adminEditItem();
                case "5" -> adminSetDiscount();
                case "6" -> adminRemoveDiscount();
                case "0" -> back = true;
                default -> System.out.println("Invalid.");
            }
        }
    }

    private void adminAddItem() {
        System.out.println("\n=== Add Item ===");

        String id = readNonEmpty("ID");
        if (catalog.getProductById(id) != null) {
            System.out.println("ID already exists.");
            return;
        }

        String name = readNonEmpty("Name");
        String category = readNonEmpty("Category");
        String description = readNonEmpty("Description");
        double price = readNonNegativeDouble("Price");
        int quantity = readNonNegativeInt("Quantity");

        Product p = new Product(id, name, category, description, price, quantity);
        catalog.adminAddProduct(p);
        DatabaseConnection.getInstance().insertNewProduct(p);
        System.out.println("Added: " + p.getName());

    }

    // Delete items in db
    private void adminDeleteItem() {
        System.out.println("\n=== Delete Item ===");
        System.out.print("Enter product ID to delete: ");
        String id = scanner.nextLine().trim();

        Product p = catalog.getProductById(id);
        if (p == null) {
            System.out.println("No product found with that ID.");
            return;
        }

        // remove from catalog list
        boolean removed = catalog.deleteProductById(id);

        // remove from DB
        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection()
                .prepareStatement("DELETE FROM products WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Database delete failed: " + e.getMessage());
        }

        if (removed) System.out.println("Deleted " + id);
    }

    private void adminEditItem() {
        System.out.println("\n=== Edit Item ===");
        System.out.print("Enter product ID: ");
        String id = scanner.nextLine().trim();

        Product p = catalog.getProductById(id);
        if (p == null) {
            System.out.println("No product found with that ID.");
            return;
        }

        System.out.println("Leave blank to keep the same value.");

        System.out.println("Current name: " + p.getName());
        System.out.print("New name: ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) p.setName(name);

        System.out.println("Current category: " + p.getCategory());
        System.out.print("New category: ");
        String cat = scanner.nextLine().trim();
        if (!cat.isEmpty()) p.setCategory(cat);

        System.out.println("Current description: " + p.getDescription());
        System.out.print("New description: ");
        String desc = scanner.nextLine().trim();
        if (!desc.isEmpty()) p.setDescription(desc);

        System.out.println("Current price: " + p.getPrice());
        System.out.print("New price: ");
        String priceStr = scanner.nextLine().trim();
        if (!priceStr.isEmpty()) {
            try {
                double price = Double.parseDouble(priceStr);
                if (price >= 0) p.setPrice(price);
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid price ignored.");
            }
        }

        System.out.println("Current quantity: " + p.getQuantity());
        System.out.print("New quantity: ");
        String qtyStr = scanner.nextLine().trim();
        if (!qtyStr.isEmpty()) {
            try {
                int qty = Integer.parseInt(qtyStr);
                if (qty >= 0) p.setQuantity(qty);
            } catch (NumberFormatException ignored) {
                System.out.println("Invalid quantity ignored.");
            }
        }

        // updates db after memory changes
        updateProductInDb(p);

        System.out.println("Updated: " + p.getId());
    }

    private void adminSetDiscount() {
        System.out.println("\n=== Set Discount ===");

        catalog.displayAll();

        System.out.print("\nEnter product ID: ");
        String id = scanner.nextLine().trim();

        Product p = catalog.getProductById(id);
        if (p == null) {
            System.out.println("No product found with that ID.");
            return;
        }

        System.out.println("\nProduct: " + p.getName());
        System.out.println("Current price: $" + String.format("%.2f", p.getPrice()));

        if (p.getDiscountPercentage() > 0) {
            System.out.println("Current discount: " + p.getDiscountPercentage() + "%");
            System.out.println("Current discounted price: $" + String.format("%.2f", p.getDiscountedPrice()));
        } else {
            System.out.println("No discount currently applied.");
        }

        System.out.print("\nEnter discount percentage (0-100): ");
        String discountStr = scanner.nextLine().trim();

        try {
            double discount = Double.parseDouble(discountStr);

            if (discount < 0 || discount > 100) {
                System.out.println("Discount must be between 0 and 100.");
                return;
            }

            p.setDiscountPercentage(discount);
            updateProductInDb(p);  // IMPORTANT: Save to database

            System.out.println("\n✓ Discount applied successfully!");
            System.out.println("Original price: $" + String.format("%.2f", p.getPrice()));
            System.out.println("Discount: " + discount + "%");
            System.out.println("New price: $" + String.format("%.2f", p.getDiscountedPrice()));
            System.out.println("Savings: $" + String.format("%.2f", p.getPrice() - p.getDiscountedPrice()));

        } catch (NumberFormatException e) {
            System.out.println("Invalid discount percentage.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void adminRemoveDiscount() {
        System.out.println("\n=== Remove Discount ===");

        System.out.println("Products with active discounts:");
        boolean hasDiscounts = false;

        for (Product p : catalog.getAllProducts()) {
            if (p.getDiscountPercentage() > 0) {
                System.out.printf("[%s] %s - Was $%.2f, Now $%.2f (%.0f%% off)%n",
                        p.getId(), p.getName(), p.getPrice(), p.getDiscountedPrice(), p.getDiscountPercentage());
                hasDiscounts = true;
            }
        }

        if (!hasDiscounts) {
            System.out.println("No products have active discounts.");
            return;
        }

        System.out.print("\nEnter product ID to remove discount: ");
        String id = scanner.nextLine().trim();

        Product p = catalog.getProductById(id);
        if (p == null) {
            System.out.println("No product found with that ID.");
            return;
        }

        if (p.getDiscountPercentage() == 0) {
            System.out.println("This product has no discount applied.");
            return;
        }

        double oldDiscountedPrice = p.getDiscountedPrice();
        p.setDiscountPercentage(0);
        updateProductInDb(p);  // IMPORTANT: Save to database

        System.out.println("\n✓ Discount removed successfully!");
        System.out.println("Product: " + p.getName());
        System.out.println("Price restored to: $" + String.format("%.2f", p.getPrice()));
    }

    private String readNonEmpty(String label) {
        while (true) {
            System.out.print(label + ": ");
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Required.");
        }
    }

    private int readNonNegativeInt(String label) {
        while (true) {
            System.out.print(label + ": ");
            String s = scanner.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= 0) return v;
            } catch (NumberFormatException ignored) {}
            System.out.println("Enter a whole number ≥ 0.");
        }
    }

    private double readNonNegativeDouble(String label) {
        while (true) {
            System.out.print(label + ": ");
            String s = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(s);
                if (v >= 0) return v;
            } catch (NumberFormatException ignored) {}
            System.out.println("Enter a number ≥ 0.");
        }
    }


    private void updateProductInDb(Product p) {
        String sql = "UPDATE products SET name=?, category=?, description=?, price=?, quantity=?, discount_percentage=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getCategory());
            ps.setString(3, p.getDescription());
            ps.setDouble(4, p.getPrice());
            ps.setInt(5, p.getQuantity());
            ps.setDouble(6, p.getDiscountPercentage());  // ADDED
            ps.setString(7, p.getId());
            ps.executeUpdate();
            System.out.println("Database updated with discount: " + p.getDiscountPercentage() + "%");
        } catch (Exception e) {
            System.out.println("Database update failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
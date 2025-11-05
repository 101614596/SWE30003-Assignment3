import java.util.Scanner;

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
            System.out.println("0. Back");
            System.out.print("Select: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "1" -> catalog.displayAll();
                case "2" -> adminAddItem();
                case "3" -> adminDeleteItem();
                case "4" -> adminEditItem();
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

    private void adminDeleteItem() {
    }

    private void adminEditItem() {
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

}

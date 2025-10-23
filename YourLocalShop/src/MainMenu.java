import java.util.Scanner;

public class MainMenu {
    private final ProductCatalog catalog;
    private final InventoryManager inventory;
    private final ShoppingCart cart;
    private final Scanner scanner = new Scanner(System.in);

    public MainMenu(ProductCatalog catalog, InventoryManager inventory, ShoppingCart cart) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.cart = cart;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n=== Convenience Store System ===");
            System.out.println("1. View Product Catalog");
            System.out.println("2. View Inventory");
            System.out.println("3. Add Product to Cart");
            System.out.println("4. View Cart");
            System.out.println("5. Remove Item from Cart");
            System.out.println("0. Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> catalog.displayAll();
                case "2" -> inventory.displayInventory();
                case "3" -> addToCart();
                case "4" -> cart.displayCart();
                case "5" -> removeFromCart();
                case "0" -> {
                    System.out.println("Exiting... Thank you!");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void addToCart() {
        System.out.print("Enter Product ID: ");
        String productId = scanner.nextLine();
        Product product = catalog.getProductById(productId);

        if (product == null) {
            System.out.println("Product not found.");
            return;
        }

        System.out.print("Enter Quantity: ");
        int qty;
        try {
            qty = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.");
            return;
        }

        cart.addItem(product, qty);
    }

    private void removeFromCart() {
        System.out.print("Enter Product ID to remove: ");
        String productId = scanner.nextLine();
        cart.removeItem(productId);
    }
}

import java.util.Scanner;
import exceptions.InsufficientStockException;
import exceptions.PaymentProcessException;
public class MainMenu {

    private ProductCatalog catalog;
    private InventoryManager inventory;
    private CustomerAccount customer;
    private ShoppingCart cart;
    private OrderProcessor orderProcessor;
    private StatisticsGenerator statsGen;
    private Scanner scanner;

    public MainMenu(ProductCatalog catalog, InventoryManager inventory, CustomerAccount customer) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.customer = customer;
        this.cart = new ShoppingCart(inventory);
        this.orderProcessor = new OrderProcessor(inventory);
        this.statsGen = new StatisticsGenerator();
        this.orderProcessor.registerObserver(statsGen);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        while (true) {
            System.out.println("\n=== Convenience Store System ===");
            System.out.println("1. View Product Catalog");
            System.out.println("2. View Inventory");
            System.out.println("3. Add Product to Cart");
            System.out.println("4. View Cart");
            System.out.println("5. Remove Item from Cart");
            System.out.println("6. Checkout");
            System.out.println("7. Admin Login");
            System.out.println("0. Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> catalog.displayAll();
                case "2" -> inventory.displayInventory();
                case "3" -> addToCart();
                case "4" -> cart.displayCartAndCheckout();
                case "5" -> removeFromCart();
                case "6" -> checkout();
                case "7" -> new Admin(catalog, inventory).start();
                case "0" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option, please try again.");
            }
        }
    }

    // MODIFIED - now handles exception
    private void addToCart() {
        catalog.displayAll();
        System.out.print("Enter Product ID to add: ");
        String productId = scanner.nextLine();

        Product product = catalog.getProductById(productId);
        if (product == null) {
            System.out.println("Invalid Product ID.");
            return;
        }

        System.out.print("Enter quantity: ");
        int qty;
        try {
            qty = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity entered.");
            return;
        }

        try {
            cart.addItem(product, qty);
        } catch (InsufficientStockException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Available stock: " + inventory.getStock(productId));
        }
    }

    private void removeFromCart() {
        cart.displayCartAndCheckout();
        System.out.print("Enter Product ID to remove: ");
        String productId = scanner.nextLine();
        cart.removeItem(productId);
    }

    // MODIFIED - now handles exceptions
    private void checkout() {
        if (cart.getItems().isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }

        System.out.println("\nSelect payment method:");
        System.out.println("1. Credit Card");
        System.out.print("Choice: ");
        String pmChoice = scanner.nextLine();

        PaymentMethod paymentMethod;
        if (pmChoice.equals("1")) {
            paymentMethod = new Creditcard();
        } else {
            System.out.println("Invalid choice. Cancelling checkout.");
            return;
        }

        System.out.println("\nProcessing order...");

        try {
            Invoice invoice = orderProcessor.process(cart, customer, paymentMethod);
            System.out.println("\n=== Order Completed ===");
            invoice.displayInvoice();
        } catch (InsufficientStockException e) {
            System.out.println("Order failed - Insufficient stock:");
            System.out.println(e.getMessage());
        } catch (PaymentProcessException e) {
            System.out.println("Order failed - Payment error:");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println("Order failed: " + e.getMessage());
        }
    }
}
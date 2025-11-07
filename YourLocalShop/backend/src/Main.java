import javax.xml.crypto.Data;
import java.util.Scanner;
import exceptions.InvalidCredentialsException;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Your Local Shop ===");
        System.out.println("Starting system...\n");

        // Initialize database
        DatabaseConnection db = null;
        try {
            db = DatabaseConnection.getInstance();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        // Load catalog and inventory
        ProductCatalog catalog = ProductCatalog.getInstance();
        catalog.loadProducts("src/data/products.json");
        InventoryManager inventory = new InventoryManager(catalog);

        // Shutdown hook
        final DatabaseConnection finalDb = db;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting Down ===");
            finalDb.close();
        }));


        Scanner scanner = new Scanner(System.in);
        System.out.println("Select mode:");
        System.out.println("1. Web Server (for frontend)");
        System.out.println("2. Console Mode (Customer Portal)");
        System.out.println("3. Console Mode (Admin Portal)");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();

        try {
            if (choice.equals("1")) {

                Webserver.start(catalog, inventory);
                System.out.println("\n✓ Web server is running!");
                System.out.println("✓ Open frontend/index.html in your browser");
                System.out.println("✓ Press Ctrl+C to stop\n");


                Thread.currentThread().join();

            } else if (choice.equals("3")) {
                // Admin console mode
                Admin admin = new Admin(catalog, inventory);
                admin.start();

            } else {
                // Customer console mode
                System.out.println("\n=== Customer Login ===");
                System.out.print("Username: ");
                String username = scanner.nextLine();

                CustomerAccount customer = CustomerAccount.loadFromDatabase(username);

                if (customer == null) {
                    System.out.println("Account not found. Creating new account...");
                    System.out.print("Password: ");
                    String password = scanner.nextLine();
                    System.out.print("Full Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    System.out.print("Phone: ");
                    String phone = scanner.nextLine();
                    System.out.print("Address: ");
                    String address = scanner.nextLine();

                    customer = new CustomerAccount(username, password, name, email, phone, address);
                    customer.saveToDatabase();
                    System.out.println("✓ Account created successfully!");
                } else {
                    System.out.print("Password: ");
                    String password = scanner.nextLine();

                    // MODIFIED - now handles exception
                    try {
                        customer.authenticate(password);
                        System.out.println("✓ Login successful! Welcome back, " + customer.getName());
                    } catch (InvalidCredentialsException e) {
                        System.out.println("Error: " + e.getMessage());
                        System.exit(0);
                    }
                }

                MainMenu menu = new MainMenu(catalog, inventory, customer);
                menu.start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        scanner.close();
    }
}
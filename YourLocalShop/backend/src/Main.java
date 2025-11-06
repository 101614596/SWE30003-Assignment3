import javax.xml.crypto.Data;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
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

        // Main menu selection
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Your Local Shop ===");
        System.out.println("1. Customer Portal");
        System.out.println("2. Admin Portal");
        System.out.print("Select portal: ");
        String choice = scanner.nextLine();

        if (choice.equals("2")) {
            // Admin portal
            Admin admin = new Admin(catalog, inventory);
            admin.start();
        } else {
            // Customer portal
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
                if (!customer.authenticate(password)) {
                    System.out.println("Invalid password!");
                    System.exit(0);
                }
                System.out.println("✓ Login successful! Welcome back, " + customer.getName());
            }

            MainMenu menu = new MainMenu(catalog, inventory, customer);
            menu.start();
        }

        scanner.close();
    }
}
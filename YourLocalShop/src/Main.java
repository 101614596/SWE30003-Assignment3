import javax.xml.crypto.Data;

public class Main {
    public static void main(String[] args) {

        DatabaseConnection db = null; 
        try{
            db = DatabaseConnection.getInstance();
        } catch (Exception e){
            System.err.println("Failed to initalize database: " + e.getMessage());
            System.exit(1);
        }

        ProductCatalog catalog =ProductCatalog.getInstance();
        catalog.loadProducts("src/data/products.json");

        InventoryManager inventory = new InventoryManager(catalog);
        ShoppingCart cart = new ShoppingCart(inventory);

        final DatabaseConnection finalDb = db; 
        Runtime.getRuntime().addShutdownHook(new Thread(()->{

            System.out.println("\nShutting down");
            finalDb.close();
        }));

        //TODO: change from dummy customer account to a login system
        CustomerAccount customer = new CustomerAccount(
                "CUST001",
                "password123",
                "John Doe",
                "john.doe@example.com",
                "0400000000",
                "123 Main Street, Hawthorn"
        );
        
        
        MainMenu menu = new MainMenu(catalog, inventory, customer);
        menu.start();
    }
}
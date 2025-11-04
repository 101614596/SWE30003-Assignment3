public class Main {
    public static void main(String[] args) {

        DatabaseConnection db = DatabaseConnection.getInstance(); 

        ProductCatalog catalog =ProductCatalog.getInstance();
        catalog.loadProducts("src/data/products.json");

        InventoryManager inventory = new InventoryManager(catalog);
        ShoppingCart cart = new ShoppingCart(inventory);

        MainMenu menu = new MainMenu(catalog, inventory, cart);
        menu.start();
    }
}
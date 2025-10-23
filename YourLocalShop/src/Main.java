public class Main {
    public static void main(String[] args) {
        ProductCatalog catalog = new ProductCatalog();
        catalog.loadProducts("src/data/products.json");

        InventoryManager inventory = new InventoryManager(catalog);
        ShoppingCart cart = new ShoppingCart(inventory);

        MainMenu menu = new MainMenu(catalog, inventory, cart);
        menu.start();
    }
}
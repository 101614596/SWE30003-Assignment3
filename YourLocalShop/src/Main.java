public class Main {
    public static void main(String[] args) {
        ProductCatalog catalog = new ProductCatalog();
        catalog.loadProducts("src/data/products.json");
        catalog.displayAll();
    }
}
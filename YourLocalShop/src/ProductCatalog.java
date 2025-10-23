import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProductCatalog {
    private List<Product> products = new ArrayList<>();

    public void loadProducts(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type productListType = new TypeToken<ArrayList<Product>>() {}.getType();
            products = gson.fromJson(reader, productListType);
        } catch (IOException e) {
            System.out.println("Error loading products: " + e.getMessage());
        }
    }

    public List<Product> getAllProducts() {
        return products;
    }

    public Product getProductById(String id) {
        for (Product p : products) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    public List<Product> searchByCategory(String category) {
        List<Product> results = new ArrayList<>();
        for (Product p : products) {
            if (p.getCategory().equalsIgnoreCase(category)) {
                results.add(p);
            }
        }
        return results;
    }

    public void displayAll() {
        System.out.println("\n=== Product Catalog ===");
        for (Product p : products) {
            System.out.println(p);
        }
    }
}

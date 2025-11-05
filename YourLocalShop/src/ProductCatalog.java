import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.beans.Statement;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductCatalog {
    private static ProductCatalog instance;
    private List<Product> products = new ArrayList<>();
    private DatabaseConnection dbConnection;

    private ProductCatalog() {
        this.dbConnection = DatabaseConnection.getInstance(); 
    }
    
    
    public static ProductCatalog getInstance() {
        if (instance == null) {
            instance = new ProductCatalog();
        }
        return instance;
    }

    public void loadProducts(String filePath) {

        loadProductsDatabase();

        if(products.isEmpty()){
            loadProductsJSON(filePath);
            syncProductsToDatabase();
        }
    }

    //Loading from json
    public void loadProductsJSON(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type productListType = new TypeToken<ArrayList<Product>>() {}.getType();
            products = gson.fromJson(reader, productListType);
        } catch (IOException e) {
            System.out.println("Error loading products: " + e.getMessage());
        }
    }

    public void loadProductsDatabase() {
        products.clear();
        String query = "SELECT * FROM products";

        try (java.sql.Statement stmt = dbConnection.getConnection().createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Product p = new Product(
                        rs.getString("ID"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                );
                products.add(p);


            }

            System.out.println("Loaded:  " + products.size() + " (from database)");

        } catch (SQLException e) {

            System.err.println("Error loading products from database: " + e.getMessage());
        }
    }

    private void syncProductsToDatabase() {
        String insertSQL= "INSERT OR REPLACE INTO products (id, name, category, description,price, quantity, available) VALUES (?,?,?,?,?,?,?)" ;
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(insertSQL)){

            for(Product p : products){

                pstmt.setString(1, p.getId());
                pstmt.setString(2, p.getName());
                pstmt.setString(3, p.getCategory());
                pstmt.setString(4, p.getDescription());
                pstmt.setDouble(6, p.getPrice());
                pstmt.setInt(7, p.getQuantity());
                pstmt.setInt(8, p.isAvailable()? 1:0);
                pstmt.executeUpdate();

            }
            System.out.println("Synced" +products.size() + "products to databse");
        } catch (Exception e) {
            System.err.println("Error syncing product: " +e.getMessage());
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
    public void adminAddProduct(Product p) {
        if (getProductById(p.getId()) != null) {

            throw new IllegalArgumentException("ID exists: " + p.getId());

        }

        products.add(p);
    }
}

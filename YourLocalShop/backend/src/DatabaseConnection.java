import java.sql.*;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:store.db";

    private DatabaseConnection() {

        try{

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Database connection established");
            initializeTables();

        }catch(SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }catch(ClassNotFoundException e){
            System.err.println("SQLite JDBC driver not found");
            System.err.println("Please add sqlite-jdbc JAR to classpath");
            e.printStackTrace();
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection ==null){
            throw new IllegalStateException("Database connection is not initialized");
        }
        return connection;
    }

    public void initializeTables(){
        try (Statement stmt = connection.createStatement()) {

            // Products table - UPDATED to include discount_percentage
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    price REAL NOT NULL,
                    quantity INTEGER NOT NULL,
                    available INTEGER NOT NULL,
                    discount_percentage REAL DEFAULT 0.0
                )
            """);

            // Add discount_percentage column if it doesn't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN discount_percentage REAL DEFAULT 0.0");
                System.out.println("Added discount_percentage column to products table");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // Customers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    username TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL,
                    phone TEXT,
                    address TEXT
                )
            """);

            // Orders table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id TEXT PRIMARY KEY,
                    customer_username TEXT NOT NULL,
                    subtotal REAL NOT NULL,
                    tax REAL NOT NULL,
                    total REAL NOT NULL,
                    status TEXT NOT NULL,
                    order_date TEXT NOT NULL,
                    FOREIGN KEY (customer_username) REFERENCES customers(username)
                )
            """);

            // Order Items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id TEXT NOT NULL,
                    product_id TEXT NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    price REAL NOT NULL,
                    subtotal REAL NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id),
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
            """);

            // Shipments table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shipments (
                    shipment_id TEXT PRIMARY KEY,
                    order_id TEXT NOT NULL,
                    tracking_number TEXT NOT NULL,
                    carrier TEXT NOT NULL,
                    status TEXT NOT NULL,
                    dispatch_date TEXT,
                    delivery_date TEXT,
                    delivery_address TEXT NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id)
                )
            """);

            // Invoices table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    invoice_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id TEXT NOT NULL,
                    customer_username TEXT NOT NULL,
                    invoice_date TEXT NOT NULL,
                    FOREIGN KEY (order_id) REFERENCES orders(order_id),
                    FOREIGN KEY (customer_username) REFERENCES customers(username)
                )
            """);

            System.out.println("Tables initialized successfully");
        }catch(SQLException e){
            System.err.println("Error initializing tables: " +e.getMessage());
        }

    }

    public void close() {
        try {
            if(connection != null && !connection.isClosed()){
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.out.println("Error closing connection: " +e.getMessage());
        }
    }


    public void insertNewProduct(Product p) {
        String sql = "INSERT INTO products (id, name, category, description, price, quantity, available, discount_percentage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getDescription());
            ps.setDouble(5, p.getPrice());
            ps.setInt(6, p.getQuantity());
            ps.setInt(7, 1); // available flag
            ps.setDouble(8, p.getDiscountPercentage()); // ADDED
            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println("DB Insert failed: " + e.getMessage());
        }
    }
}
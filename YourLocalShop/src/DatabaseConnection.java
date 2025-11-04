import java.sql.*;

public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:store.db"; 

    private DatabaseConnection() {

        try{

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            initializeTables();

        }catch(ClassNotFoundException | SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }


    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection(); 
        }
        return instance; 
    }

    public Connection getConnection() {
        return connection; 
    }

    public void initializeTables(){
        try (Statement stmt = connection.createStatement()) {
            
            // Products table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    price REAL NOT NULL,
                    quantity INTEGER NOT NULL,
                    available INTEGER NOT NULL
                )
            """);
            
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
        
            System.out.println("Table initialized succefully");
        }catch(SQLException e){
            System.err.println("Error initializing table: " +e.getMessage());
        }

    }

    public void close() {
        try {
            if(connection != null && !connection.isClosed()){
                connection.close();
                System.out.println("Database connection close");
            }
        } catch (SQLException e) {
            System.out.println("Error closign connection: " +e.getMessage());
        }
    }
}

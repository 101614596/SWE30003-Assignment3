import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.Data;

public class CustomerAccount {
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String address;

    private List<Order> orderHistory;
    private List<Invoice> invoices;

    public CustomerAccount(String username, String password, String name, String email, String phone, String address) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.orderHistory = new ArrayList<>();
        this.invoices = new ArrayList<>();
    }


    public void saveToDatabase(){
        DatabaseConnection db = DatabaseConnection.getInstance(); 
        String insertSQL = "INSERT OR REPLACE INTO customers (username, password, name, email, phone, address) VALUES (?,?,?,?,?,?)"; 

            try (PreparedStatement pstmt = db.getConnection().prepareStatement(insertSQL)){
                pstmt.setString(1, this.username);
                pstmt.setString(2, this.password);
                pstmt.setString(3, this.name); 
                pstmt.setString(4, this.email);
                pstmt.setString(5, this.phone);
                pstmt.setString(6, this.address);
                pstmt.executeUpdate();

                System.out.println("Customer info saved to database");

            }catch(SQLException e){

                System.err.println("Error saving customer info to database: " +e.getMessage());
                e.printStackTrace();
            }
    
    }

    public static CustomerAccount loadFromDatabase(String username){
        DatabaseConnection db = DatabaseConnection.getInstance();
        String query = "SELECT * FROM customers WHERE username = ?";

        try (PreparedStatement pstmt = db.getConnection().prepareStatement(query)){
            pstmt.setString(1, username);
            ResultSet rs= pstmt.executeQuery();

            if(rs.next()){
                return new CustomerAccount(
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("address")
                    
                    );
            }
        } catch (SQLException e) {
            System.err.println("Error loading customer from database: " + e.getMessage());
        }

        return null; 

    }


    // --- Authentication ---
    public boolean authenticate(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    // --- Personal Info ---
    public void updateContactInfo(String email, String phone, String address) {
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    // --- Orders & Invoices ---
    public void addOrder(Order order) {
        orderHistory.add(order);
    }

    public void addInvoice(Invoice invoice) {
        invoices.add(invoice);
    }

    public List<Order> getOrderHistory() {
        return orderHistory;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }


    // --- Getters ---
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

    @Override
    public String toString() {
        return String.format("Customer: %s (%s)\nEmail: %s\nPhone: %s\nAddress: %s",
                name, username, email, phone, address);
    }
}


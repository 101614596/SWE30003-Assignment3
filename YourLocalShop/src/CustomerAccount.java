import java.util.ArrayList;
import java.util.List;

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


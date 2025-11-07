import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import exceptions.InsufficientStockException;
import exceptions.PaymentProcessException;


public class Webserver {
    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private static ProductCatalog catalog;
    private static InventoryManager inventory;

    // Store carts per session
    private static Map<String, ShoppingCart> sessionCarts = new HashMap<>();

    public static void start(ProductCatalog cat, InventoryManager inv) throws IOException {
        catalog = cat;
        inventory = inv;

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Enable CORS for all endpoints
        server.createContext("/api/products", new ProductHandler());
        server.createContext("/api/cart", new CartHandler());
        server.createContext("/api/orders", new OrderHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("✓ Web server started on http://localhost:" + PORT);
        System.out.println("✓ Frontend can now connect to the API");
    }

    // Helper method to enable CORS
    private static void enableCORS(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    // Helper method to send JSON response
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        enableCORS(exchange);
        String jsonResponse = gson.toJson(data);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Helper method to get session cart
    private static ShoppingCart getSessionCart(HttpExchange exchange) {
        String sessionId = exchange.getRemoteAddress().toString();
        System.out.println("Session ID: " + sessionId);
        return sessionCarts.computeIfAbsent(sessionId, k -> new ShoppingCart(inventory));
    }

    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                enableCORS(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            System.out.println("Product request: " + exchange.getRequestMethod() + " " + path + (query != null ? "?" + query : ""));

            if ("GET".equals(exchange.getRequestMethod())) {
                if (path.equals("/api/products")) {
                    // Get all products or search by category
                    if (query != null && query.startsWith("category=")) {
                        String category = query.substring(9);
                        System.out.println("Filtering by category: " + category);
                        sendJsonResponse(exchange, 200, catalog.searchByCategory(category));
                    } else {
                        System.out.println("Returning all products");
                        // Create enriched product list with discount info
                        List<Map<String, Object>> enrichedProducts = new ArrayList<>();
                        for (Product p : catalog.getAllProducts()) {
                            Map<String, Object> productData = new HashMap<>();
                            productData.put("id", p.getId());
                            productData.put("name", p.getName());
                            productData.put("category", p.getCategory());
                            productData.put("description", p.getDescription());
                            productData.put("price", p.getPrice());
                            productData.put("quantity", p.getQuantity());
                            productData.put("available", p.isAvailable());

                            // Add discount info if applicable
                            if (p.getDiscountPercentage() > 0) {
                                productData.put("discountPercentage", p.getDiscountPercentage());
                                productData.put("discountedPrice", p.getDiscountedPrice());
                            }

                            enrichedProducts.add(productData);
                        }
                        sendJsonResponse(exchange, 200, enrichedProducts);
                    }
                } else {
                    // Get single product by ID
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String productId = parts[3];
                        System.out.println("Getting product: " + productId);
                        Product product = catalog.getProductById(productId);
                        if (product != null) {
                            // Create enriched product data
                            Map<String, Object> productData = new HashMap<>();
                            productData.put("id", product.getId());
                            productData.put("name", product.getName());
                            productData.put("category", product.getCategory());
                            productData.put("description", product.getDescription());
                            productData.put("price", product.getPrice());
                            productData.put("quantity", product.getQuantity());
                            productData.put("available", product.isAvailable());

                            if (product.getDiscountPercentage() > 0) {
                                productData.put("discountPercentage", product.getDiscountPercentage());
                                productData.put("discountedPrice", product.getDiscountedPrice());
                            }

                            sendJsonResponse(exchange, 200, productData);
                        } else {
                            sendJsonResponse(exchange, 404, Map.of("error", "Product not found"));
                        }
                    }
                }
            }
        }
    }


    static class CartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                enableCORS(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            ShoppingCart cart = getSessionCart(exchange);
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            System.out.println("Cart request: " + method + " " + path);

            if ("GET".equals(method)) {
                System.out.println("Returning cart with " + cart.getItems().size() + " items");
                sendJsonResponse(exchange, 200, cart.getItems());

            } else if ("POST".equals(method)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Cart POST body: " + body);

                try {
                    Map<String, Object> request = gson.fromJson(body, Map.class);
                    String productId = (String) request.get("productId");
                    int quantity = ((Double) request.get("quantity")).intValue();

                    System.out.println("Adding to cart: " + productId + " x " + quantity);

                    Product product = catalog.getProductById(productId);
                    if (product != null) {
                        cart.addItem(product, quantity);
                        System.out.println("Cart now has " + cart.getItems().size() + " items");
                        sendJsonResponse(exchange, 200, cart.getItems());
                    } else {
                        System.out.println("Product not found: " + productId);
                        sendJsonResponse(exchange, 404, Map.of("error", "Product not found"));
                    }
                } catch (Exception e) {
                    System.err.println("Error adding to cart: " + e.getMessage());
                    e.printStackTrace();
                    sendJsonResponse(exchange, 400, Map.of("error", e.getMessage()));
                }

            } else if ("DELETE".equals(method)) {
                String[] parts = path.split("/");
                if (parts.length > 3) {
                    String productId = parts[3];
                    System.out.println("Removing from cart: " + productId);
                    cart.removeItem(productId);
                    sendJsonResponse(exchange, 200, cart.getItems());
                } else {
                    sendJsonResponse(exchange, 400, Map.of("error", "Product ID required"));
                }
            }
        }
    }


    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                enableCORS(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            System.out.println("Order request: " + method + " " + path);

            // NEW: Handle order history requests
            if ("GET".equals(method) && path.startsWith("/api/orders/history/")) {
                String[] parts = path.split("/");
                if (parts.length > 4) {
                    String email = java.net.URLDecoder.decode(parts[4], StandardCharsets.UTF_8);
                    System.out.println("Fetching order history for: " + email);

                    try {
                        List<Map<String, Object>> orderHistory = getOrderHistoryForCustomer(email);
                        sendJsonResponse(exchange, 200, orderHistory);
                    } catch (Exception e) {
                        System.err.println("Error fetching order history: " + e.getMessage());
                        e.printStackTrace();
                        sendJsonResponse(exchange, 500, Map.of("error", "Failed to fetch order history"));
                    }
                }
                return;
            }

            if ("POST".equals(method) && path.startsWith("/api/orders")) {
                ShoppingCart cart = getSessionCart(exchange);

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Checkout request body: " + body);

                try {
                    Map<String, Object> request = gson.fromJson(body, Map.class);

                    Map<String, String> customerData = (Map<String, String>) request.get("customerData");
                    String paymentMethodType = (String) request.get("paymentMethod");

                    System.out.println("Processing order for: " + customerData.get("name"));

                    CustomerAccount customer = new CustomerAccount(
                            customerData.get("email"),
                            "temp_password",
                            customerData.get("name"),
                            customerData.get("email"),
                            customerData.get("phone"),
                            customerData.get("address")
                    );

                    PaymentMethod paymentMethod = new Creditcard();

                    OrderProcessor processor = new OrderProcessor(inventory);
                    Invoice invoice;

                    try {
                        invoice = processor.process(cart, customer, paymentMethod);

                        // Save order to database
                        saveOrderToDatabase(invoice);

                        Map<String, Object> response = new HashMap<>();
                        response.put("orderId", invoice.getOrder().getOrderId());
                        response.put("trackingNumber", invoice.getShipment().getTrackingNumber());
                        response.put("total", invoice.getOrder().getTotal());
                        response.put("success", true);

                        System.out.println("Order completed: " + invoice.getOrder().getOrderId());

                        sendJsonResponse(exchange, 200, response);

                        String sessionId = exchange.getRemoteAddress().toString();
                        sessionCarts.remove(sessionId);

                    } catch (InsufficientStockException e) {
                        System.out.println("Order failed - insufficient stock: " + e.getMessage());
                        sendJsonResponse(exchange, 400, Map.of(
                                "error", "Insufficient stock: " + e.getMessage(),
                                "success", false
                        ));
                    } catch (PaymentProcessException e) {
                        System.out.println("Order failed - payment error: " + e.getMessage());
                        sendJsonResponse(exchange, 400, Map.of(
                                "error", "Payment failed: " + e.getMessage(),
                                "success", false
                        ));
                    }

                } catch (Exception e) {
                    System.err.println("Error processing order: " + e.getMessage());
                    e.printStackTrace();
                    sendJsonResponse(exchange, 500, Map.of(
                            "error", "Server error: " + e.getMessage(),
                            "success", false
                    ));
                }
            } else {
                sendJsonResponse(exchange, 404, Map.of("error", "Endpoint not found", "success", false));
            }
        }

        // NEW: Save order to database
        private void saveOrderToDatabase(Invoice invoice) {
            try {
                Order order = invoice.getOrder();
                CustomerAccount customer = invoice.getCustomer();
                Shipment shipment = invoice.getShipment();

                // Save order
                String orderSQL = "INSERT INTO orders (order_id, customer_username, subtotal, tax, total, status, order_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(orderSQL)) {
                    ps.setString(1, order.getOrderId());
                    ps.setString(2, customer.getEmail());
                    ps.setDouble(3, order.getSubtotal());
                    ps.setDouble(4, order.getTax());
                    ps.setDouble(5, order.getTotal());
                    ps.setString(6, order.getStatus().toString());
                    ps.setString(7, order.getOrderDate().toString());
                    ps.executeUpdate();
                }

                // Save order items
                String itemSQL = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
                for (OrderItem item : order.getItems()) {
                    try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(itemSQL)) {
                        ps.setString(1, order.getOrderId());
                        ps.setString(2, item.getProduct().getId());
                        ps.setString(3, item.getProduct().getName());
                        ps.setInt(4, item.getQuantity());
                        ps.setDouble(5, item.getProduct().getPrice());
                        ps.setDouble(6, item.getSubtotal());
                        ps.executeUpdate();
                    }
                }

                // Save shipment
                String shipmentSQL = "INSERT INTO shipments (shipment_id, order_id, tracking_number, carrier, status, delivery_address) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(shipmentSQL)) {
                    ps.setString(1, shipment.getShipmentId());
                    ps.setString(2, order.getOrderId());
                    ps.setString(3, shipment.getTrackingNumber());
                    ps.setString(4, shipment.getCarrier());
                    ps.setString(5, shipment.getStatus().toString());
                    ps.setString(6, shipment.getDeliveryAddress());
                    ps.executeUpdate();
                }

                System.out.println("Order saved to database: " + order.getOrderId());

            } catch (Exception e) {
                System.err.println("Error saving order to database: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // NEW: Get order history for customer
        private List<Map<String, Object>> getOrderHistoryForCustomer(String email) {
            List<Map<String, Object>> orders = new ArrayList<>();

            try {
                String query = "SELECT o.*, s.tracking_number FROM orders o " +
                        "LEFT JOIN shipments s ON o.order_id = s.order_id " +
                        "WHERE o.customer_username = ? ORDER BY o.order_date DESC";

                try (PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(query)) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        Map<String, Object> order = new HashMap<>();
                        order.put("orderId", rs.getString("order_id"));
                        order.put("orderDate", rs.getString("order_date"));
                        order.put("total", rs.getDouble("total"));
                        order.put("status", rs.getString("status"));
                        order.put("trackingNumber", rs.getString("tracking_number"));
                        orders.add(order);
                    }
                }

            } catch (Exception e) {
                System.err.println("Error fetching order history: " + e.getMessage());
                e.printStackTrace();
            }

            return orders;
        }
    }
}
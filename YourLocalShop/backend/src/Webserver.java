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
import java.time.LocalDateTime;

import exceptions.InsufficientStockException;
import exceptions.PaymentProcessException;
import builders.InvoiceBuilder;
import builders.ShipmentBuilder;


public class Webserver {
    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();;
    private static ProductCatalog catalog;
    private static InventoryManager inventory;

    // Store carts per session (simplified - in production use proper session management)
    private static Map<String, ShoppingCart> sessionCarts = new HashMap<>();

    public static void start(ProductCatalog cat, InventoryManager inv) throws IOException {
        catalog = cat;
        inventory = inv;

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Enable CORS for all endpoints
        server.createContext("/api/products", new ProductHandler());
        server.createContext("/api/cart", new CartHandler());
        server.createContext("/api/orders", new OrderHandler());

        server.setExecutor(null); // creates a default executor
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
        String sessionId = exchange.getRemoteAddress().toString(); // Simple session tracking
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
                        sendJsonResponse(exchange, 200, catalog.getAllProducts());
                    }
                } else {
                    // Get single product by ID
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String productId = parts[3];
                        System.out.println("Getting product: " + productId);
                        Product product = catalog.getProductById(productId);
                        if (product != null) {
                            sendJsonResponse(exchange, 200, product);
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
                // Return cart items
                System.out.println("Returning cart with " + cart.getItems().size() + " items");
                sendJsonResponse(exchange, 200, cart.getItems());

            } else if ("POST".equals(method)) {
                // Add item to cart
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
                // Remove item from cart
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
            System.out.println("Order request: " + exchange.getRequestMethod() + " " + path);

            if ("POST".equals(exchange.getRequestMethod()) && path.startsWith("/api/orders")) {
                ShoppingCart cart = getSessionCart(exchange);

                // Parse checkout request
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("Checkout request body: " + body);

                try {
                    Map<String, Object> request = gson.fromJson(body, Map.class);

                    Map<String, String> customerData = (Map<String, String>) request.get("customerData");
                    String paymentMethodType = (String) request.get("paymentMethod");

                    System.out.println("Processing order for: " + customerData.get("name"));

                    // Create customer account
                    CustomerAccount customer = new CustomerAccount(
                            customerData.get("email"), // using email as username
                            "temp_password", // In production, handle this properly
                            customerData.get("name"),
                            customerData.get("email"),
                            customerData.get("phone"),
                            customerData.get("address")
                    );

                    // Create payment method
                    PaymentMethod paymentMethod = new Creditcard(); // Only credit card for now

                    // Process order - MODIFIED to handle exceptions
                    OrderProcessor processor = new OrderProcessor(inventory);
                    Invoice invoice;

                    try {
                        invoice = processor.process(cart, customer, paymentMethod);

                        // Create response with invoice details
                        Map<String, Object> response = new HashMap<>();
                        response.put("orderId", invoice.getOrder().getOrderId());
                        response.put("trackingNumber", invoice.getShipment().getTrackingNumber());
                        response.put("total", invoice.getOrder().getTotal());
                        response.put("success", true);

                        System.out.println("Order completed: " + invoice.getOrder().getOrderId());

                        sendJsonResponse(exchange, 200, response);

                        // Clear the session cart
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
                // Invalid method or path
                sendJsonResponse(exchange, 404, Map.of("error", "Endpoint not found", "success", false));
            }
        }
    }
}
import java.util.HashMap;
import java.util.Map;

public class StatisticsGenerator implements Observer {
    private Map<String, Integer> productSales = new HashMap<>();
    private double totalRevenue = 0;
    private int totalOrders = 0;

    @Override
    public void notifyUpdate(Object event) {
        if (event instanceof Order order) {
            updateSalesData(order);
        }
    }

    private void updateSalesData(Order order) {
        totalOrders++;
        totalRevenue += order.getTotal();

        for (OrderItem item : order.getItems()) {
            String productName = item.getProduct().getName();
            productSales.put(productName,
                    productSales.getOrDefault(productName, 0) + item.getQuantity());
        }

        System.out.println("[StatisticsGenerator] Updated stats after Order " + order.getOrderId());
    }

    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalOrders() { return totalOrders; }
    public Map<String, Integer> getProductSales() { return productSales; }
}

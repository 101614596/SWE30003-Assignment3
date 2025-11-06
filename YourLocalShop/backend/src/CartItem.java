import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CartItem {
    private Product product;
    private int quantity;
    private LocalDateTime reservationTime;
    private static final int RESERVATION_MINUTES = 15;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.reservationTime = LocalDateTime.now(); 
    }

    public boolean isReservationExpired(){
        return LocalDateTime.now().isAfter(reservationTime.plusMinutes(RESERVATION_MINUTES));

    }

    public long getMinutesUntilExpiration() {
        LocalDateTime expiryTime = reservationTime.plusMinutes(RESERVATION_MINUTES);
        long minutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), expiryTime);
        return Math.max(0, minutes);
    }

    public void refreshReservation() {
        this.reservationTime = LocalDateTime.now();
    }


    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least 1.");
        this.quantity = quantity;
        this.reservationTime = LocalDateTime.now();
    }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s x%d = $%.2f",
                product.getId(), product.getName(), quantity, getSubtotal());
    }
}
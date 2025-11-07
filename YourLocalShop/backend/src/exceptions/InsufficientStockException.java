package exceptions;

public class InsufficientStockException extends Exception {
    private String productId;
    private int requested;
    private int available;

    public InsufficientStockException(String productId, int requested, int available) {
        super(String.format("Insufficient stock for product %s: requested %d, available %d",
                productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    //getters



}
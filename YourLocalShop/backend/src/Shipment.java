import java.time.LocalDateTime;

public class Shipment {
    private String shipmentId;
    private Order order;
    private String deliveryAddress;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime dispatchDate;
    private LocalDateTime deliveryDate;
    private ShipmentStatus status;

    public Shipment(String shipmentId, Order order, String deliveryAddress, String carrier) {
        this.shipmentId = shipmentId;
        this.order = order;
        this.deliveryAddress = deliveryAddress;
        this.carrier = carrier;
        this.trackingNumber = generateTrackingNumber();
        this.status = ShipmentStatus.PENDING;
    }

    //  Shipment creation
    private String generateTrackingNumber() {
        return "TRK-" + (int)(Math.random() * 1000000);
    }

    public void dispatch() {
        this.status = ShipmentStatus.DISPATCHED;
        this.dispatchDate = LocalDateTime.now();
    }

    public void deliver() {
        this.status = ShipmentStatus.DELIVERED;
        this.deliveryDate = LocalDateTime.now();
    }

    public void updateStatus(ShipmentStatus newStatus) {
        this.status = newStatus;
    }

    //  Getters
    public String getShipmentId() { return shipmentId; }
    public Order getOrder() { return order; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCarrier() { return carrier; }
    public ShipmentStatus getStatus() { return status; }
    public LocalDateTime getDispatchDate() { return dispatchDate; }
    public LocalDateTime getDeliveryDate() { return deliveryDate; }

    @Override
    public String toString() {
        return String.format(
            "Shipment [%s] - Order ID: %s%nCarrier: %s%nTracking: %s%nStatus: %s%nDestination: %s%n",
            shipmentId,
            order.getOrderId(),
            carrier,
            trackingNumber,
            status,
            deliveryAddress
        );
    }
}


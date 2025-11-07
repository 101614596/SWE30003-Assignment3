package builders;

public class ShipmentBuilder {
    private String shipmentId;
    private Order order;
    private String deliveryAddress;
    private String carrier = "AUSPOST"; // Default carrier

    public ShipmentBuilder setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
        return this;
    }

    public ShipmentBuilder setOrder(Order order) {
        this.order = order;
        return this;
    }

    public ShipmentBuilder setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
        return this;
    }

    public ShipmentBuilder setCarrier(String carrier) {
        this.carrier = carrier;
        return this;
    }

    public Shipment build() {
        if (shipmentId == null || shipmentId.isEmpty()) {
            throw new IllegalStateException("Shipment ID is required");
        }
        if (order == null) {
            throw new IllegalStateException("Order is required");
        }
        if (deliveryAddress == null || deliveryAddress.isEmpty()) {
            throw new IllegalStateException("Delivery address is required");
        }

        return new Shipment(shipmentId, order, deliveryAddress, carrier);
    }
}
public class InvoiceBuilder {
    private Order order;
    private CustomerAccount customer;
    private Shipment shipment;

    public InvoiceBuilder setOrder(Order order) {
        this.order = order;
        return this;
    }

    public InvoiceBuilder setCustomer(CustomerAccount customer) {
        this.customer = customer;
        return this;
    }

    public InvoiceBuilder setShipment(Shipment shipment) {
        this.shipment = shipment;
        return this;
    }

    public Invoice build() {
        if (order == null || customer == null) {
            throw new IllegalStateException("Order and Customer are required to build Invoice");
        }
        return new Invoice(order, customer, shipment);
    }
}
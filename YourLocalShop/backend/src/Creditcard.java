public class Creditcard implements PaymentMethod {
    @Override
    public boolean processPayment(double amount){
        System.out.println("Processing payment of $" + String.format("%.2f", amount ));
        return true;
    }

    @Override
    public String getPaymentMethod(){
        return "Credit Card";
    }
}
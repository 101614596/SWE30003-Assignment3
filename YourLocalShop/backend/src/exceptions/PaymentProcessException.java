package exceptions;

public class PaymentProcessException extends Exception {
    public PaymentProcessException(String message) {
        super(message);
    }
}
package exceptions;

public class MFAVerificationException extends Exception {
    public MFAVerificationException(String message) {
        super(message);
    }
}
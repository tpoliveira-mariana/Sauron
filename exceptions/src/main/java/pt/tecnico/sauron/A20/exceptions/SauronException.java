package pt.tecnico.sauron.A20.exceptions;

public class SauronException extends Exception {
    private final ErrorMessage _message;

    public SauronException(ErrorMessage errorMessage) {
        super(errorMessage.label);
        _message = errorMessage;
    }

    public ErrorMessage getErrorMessage() {
        return _message;
    }

    public String getErrorMessageLabel() {
        return _message.label;
    }
}
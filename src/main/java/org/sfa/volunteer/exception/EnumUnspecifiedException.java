package org.sfa.volunteer.exception;

public class EnumUnspecifiedException extends RuntimeException {
    public EnumUnspecifiedException(String message) {
        super(message);
    }

    public EnumUnspecifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}

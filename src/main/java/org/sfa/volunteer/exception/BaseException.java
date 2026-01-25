package org.sfa.volunteer.exception;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BaseException extends RuntimeException {
    private final Object[] params;

    public BaseException(String message, Object... params) {
        super(message);
        this.params = params;
    }

    public BaseException(String message, Throwable cause, Object... params) {
        super(message, cause);
        this.params = params;
    }
}
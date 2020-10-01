package com.atask.exception;

public class ATaskException extends RuntimeException {

    public ATaskException(String message) {
        super(message);
    }

    public ATaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public ATaskException(Throwable cause) {
        super(cause);
    }
}

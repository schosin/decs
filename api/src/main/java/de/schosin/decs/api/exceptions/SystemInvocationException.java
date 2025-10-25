package de.schosin.decs.api.exceptions;

public final class SystemInvocationException extends EcsException {
    private static final long serialVersionUID = 1L;

    public SystemInvocationException(String message, Exception cause) {
        super(message, cause);
    }

}

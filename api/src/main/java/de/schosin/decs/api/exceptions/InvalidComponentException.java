package de.schosin.decs.api.exceptions;

public class InvalidComponentException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final Class<?> type;

    public InvalidComponentException(String message, Class<?> type) {
        super(message);

        this.type = type;
    }

    public Class<?> getType() {
        return this.type;
    }

}

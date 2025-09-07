package de.schosin.decs.api.exceptions;

public class InvalidUtilityException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final Class<?> utilityClass;

    public InvalidUtilityException(String message, Exception cause, Class<?> utilityClass) {
        super(message, cause);

        this.utilityClass = utilityClass;
    }

    public InvalidUtilityException(String message, Class<?> utilityClass) {
        super(message);

        this.utilityClass = utilityClass;
    }

    public Class<?> getUtilityClass() {
        return this.utilityClass;
    }

}

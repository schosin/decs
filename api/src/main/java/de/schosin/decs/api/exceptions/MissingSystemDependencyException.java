package de.schosin.decs.api.exceptions;

public class MissingSystemDependencyException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final Class<?> systemClass;

    public MissingSystemDependencyException(String message, Class<?> systemClass) {
        super(message);

        this.systemClass = systemClass;
    }

    public MissingSystemDependencyException(String message, Exception cause, Class<?> systemClass) {
        super(message, cause);

        this.systemClass = systemClass;
    }

    public Class<?> getSystemClass() {
        return this.systemClass;
    }

}

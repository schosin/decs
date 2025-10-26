package de.schosin.decs.api.exceptions;

public class InvalidSystemInvocationException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final Class<?> systemInvocationClass;

    public InvalidSystemInvocationException(Class<?> systemInvocationClass) {
        super("SystemInvocation implementation not available: " + systemInvocationClass.getName());

        this.systemInvocationClass = systemInvocationClass;
    }

    public Class<?> getSystemInvocationClass() {
        return systemInvocationClass;
    }

}

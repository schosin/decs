package de.schosin.decs.api.exceptions;

public class DuplicateSingletonException extends EcsException {
    private static final long serialVersionUID = 1L;

    public DuplicateSingletonException(Class<?> type) {
        super(String.format("Singletons must be unique, but detected multiple singletons of type '%s'", type.getName()));
    }

}

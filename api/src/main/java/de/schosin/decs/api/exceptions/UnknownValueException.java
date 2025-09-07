package de.schosin.decs.api.exceptions;

public class UnknownValueException extends EcsException {
    private static final long serialVersionUID = 1L;

    public UnknownValueException(Object reference, String value) {
        super(String.format("Unkown value '%s' used: %s", value, reference));
    }

}

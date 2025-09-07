package de.schosin.decs.api.exceptions;

public class ParallelExecutionException extends EcsException {
    private static final long serialVersionUID = 1L;

    public ParallelExecutionException(Throwable cause) {
        super(cause);
    }

}

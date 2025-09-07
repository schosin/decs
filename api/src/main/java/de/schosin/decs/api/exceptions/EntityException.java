package de.schosin.decs.api.exceptions;

public abstract class EntityException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final int entityId;

    public EntityException(int entityId, String message) {
        super(message);

        this.entityId = entityId;
    }

    public int getEntityId() {
        return this.entityId;
    }

}

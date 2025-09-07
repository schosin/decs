package de.schosin.decs.api.exceptions;

public final class EntityModifiedException extends EcsException {
    private static final long serialVersionUID = 1L;

    private final int entityId;

    public EntityModifiedException(int entityId, String message) {
        super(message);

        this.entityId = entityId;
    }

    public int getEntityId() {
        return this.entityId;
    }

}

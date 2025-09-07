package de.schosin.decs.api.utils.pool;

/**
 * Interface that can be implemented by pooled types to reset its state.
 * Whenever a type implementing this interface is returned to a {@link Pool pool}, {@link #reset()} will be invoked.
 */
public interface Pooled {

    /**
     * Called when this instance is returned to a {@link Pool pool}.
     */
    void reset();

}

package de.schosin.decs.core.systems;

import de.schosin.decs.api.internal.InternalWorld;
import de.schosin.decs.api.systems.SystemType;

public interface SystemData {

    SystemType getInstance(InternalWorld world);

}

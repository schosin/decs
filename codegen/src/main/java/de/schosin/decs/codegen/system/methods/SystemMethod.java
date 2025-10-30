package de.schosin.decs.codegen.system.methods;

import java.util.List;

import de.schosin.decs.codegen.utils.ParameterData;

/**
 * Base interface extending {@link EcsMethod} for methods annotated with processor annotations.
 */
public sealed interface SystemMethod extends EcsMethod permits ProcessorMethod, CallbackMethod {

    List<? extends ParameterData> parameters();

}

package de.schosin.decs.codegen.system.helper;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.methods.CallbackMethod.RemovedMethod;
import de.schosin.decs.codegen.utils.Annotations;
import de.schosin.decs.codegen.utils.ParameterData;

public class RemovedGenerator extends AbstractCallbackGenerator<RemovedMethod> {

    public RemovedGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator, Annotations::removed, RemovedMethod.class, "RemovedHandler");
    }

    @Override
    protected List<ParameterData> resolveCallbackParameters(ExecutableElement method, String name) {
        var parameters = super.resolveCallbackParameters(method, name);

        var entityParam = parameters.stream().filter(ParameterData.EntityParameter.class::isInstance).findFirst().orElse(null);
        if (entityParam != null) {
            printError("Cannot use Entity parameter in @Removed methods. Use 'int entityId' instead.", entityParam.parameter());
            return null;
        }

        return parameters;
    }

    @Override
    protected RemovedMethod createMethod(ExecutableElement method, CompositionData composition, List<ParameterData> parameters) {
        return new RemovedMethod(method, method.getSimpleName().toString(), composition, parameters);
    }

}

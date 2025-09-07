package de.schosin.decs.codegen.system.helper;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;

import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.SystemGenerator;
import de.schosin.decs.codegen.system.methods.CallbackMethod.InsertedMethod;
import de.schosin.decs.codegen.utils.Annotations;
import de.schosin.decs.codegen.utils.ParameterData;

public class InsertedGenerator extends AbstractCallbackGenerator<InsertedMethod> {

    public InsertedGenerator(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, SystemGenerator generator) {
        super(processingEnv, roundEnv, generator, Annotations::inserted, InsertedMethod.class, "InsertedHandler");
    }

    @Override
    protected InsertedMethod createMethod(ExecutableElement method, CompositionData composition, List<ParameterData> parameters) {
        return new InsertedMethod(method, method.getSimpleName().toString(), composition, parameters);
    }

}

package de.schosin.decs.codegen.system.methods;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import de.schosin.decs.codegen.AbstractManifestTest;
import de.schosin.decs.codegen.components.ComponentData.ClassComponent;
import de.schosin.decs.codegen.components.ComponentData.EnumComponent;
import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.system.methods.CallbackMethod.InsertedMethod;
import de.schosin.decs.codegen.system.methods.CallbackMethod.RemovedMethod;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.EntityProcessorMethod;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.EntityProcessorMethod.ParallelConfig;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.EntityProcessorMethod.ParallelStrategy;
import de.schosin.decs.codegen.system.methods.ProcessorMethod.SystemProcessorMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.ArchetypeMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.CountMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.TransmuteMethod;
import de.schosin.decs.codegen.utils.Parameter;
import de.schosin.decs.codegen.utils.ParameterData;
import de.schosin.decs.codegen.utils.ParameterData.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EcsMethodTest {

    @Nested
    class MetadataTest extends AbstractManifestTest {

        @Test
        void testUnknownType() {
            var reader = createReader("""
                    UNKNOWN_TYPE
                    This line does not matter
                    """);

            assertThatThrownBy(() -> EcsMethod.readMetadata(reader))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown method type 'UNKNOWN_TYPE'. Perform a clean build.");
        }

        @ParameterizedTest
        @MethodSource("systemParameters")
        void testSystemProcessorMethod(List<SystemParameterData> parameters) throws IOException {
            var writer = createWriter();

            var method = new SystemProcessorMethod(null, "foo", true, parameters);
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(SystemProcessorMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.parameters()).isEqualTo(method.parameters());

            var systemMetadata = assertThat(ProcessorMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(SystemProcessorMethod.class)).actual();
            assertThat(systemMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(systemMetadata.parameters()).isEqualTo(method.parameters());
        }

        @ParameterizedTest
        @MethodSource("parameters")
        void testEntityProcessorMethod(List<ParameterData> parameters) throws IOException {
            var writer = createWriter();

            var parallel = new ParallelConfig(ParallelStrategy.NONE);
            var method = new EntityProcessorMethod(null, "foo", new CompositionData(List.of(), null, null), parallel, parameters, null, false);
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(EntityProcessorMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.composition()).isEqualTo(method.composition());
            assertThat(metadata.parallel()).isEqualTo(method.parallel());
            assertThat(metadata.parameters()).isEqualTo(method.parameters());

            var systemMetadata = assertThat(ProcessorMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(EntityProcessorMethod.class)).actual();
            assertThat(systemMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(systemMetadata.composition()).isEqualTo(method.composition());
            assertThat(systemMetadata.parameters()).isEqualTo(method.parameters());
        }

        @ParameterizedTest
        @MethodSource("parameters")
        void testInsertedMethod(List<ParameterData> parameters) throws IOException {
            var writer = createWriter();

            var method = new InsertedMethod(null, "foo", new CompositionData(List.of(), null, null), parameters);
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(InsertedMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.composition()).isEqualTo(method.composition());
            assertThat(metadata.parameters()).isEqualTo(method.parameters());

            var callbackMetadata = assertThat(CallbackMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(InsertedMethod.class)).actual();
            assertThat(callbackMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(callbackMetadata.composition()).isEqualTo(method.composition());
            assertThat(callbackMetadata.parameters()).isEqualTo(method.parameters());
        }

        @ParameterizedTest
        @MethodSource("parameters")
        void testRemovedMethod(List<ParameterData> parameters) throws IOException {
            var writer = createWriter();

            var method = new RemovedMethod(null, "foo", new CompositionData(List.of(), null, null), parameters);
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(RemovedMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.composition()).isEqualTo(method.composition());
            assertThat(metadata.parameters()).isEqualTo(method.parameters());

            var callbackMetadata = assertThat(CallbackMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(RemovedMethod.class)).actual();
            assertThat(callbackMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(callbackMetadata.composition()).isEqualTo(method.composition());
            assertThat(callbackMetadata.parameters()).isEqualTo(method.parameters());
        }

        @Test
        void testArchetypeMethod() throws IOException {
            var writer = createWriter();

            var parameter = new Parameter(TypeName.INT, "num");
            var enumParameter = new ComponentParameter(null, "foo", new EnumComponent(null, null, ClassName.get("foo", "Bar")), "foo");
            var componentParameter = new ComponentParameter(null, "bar", new ClassComponent(null, null, ClassName.get("foo", "Bar")), "bar");

            var method = new ArchetypeMethod(null, null, "foo", "bar", true, List.of(parameter), List.of(enumParameter), List.of(componentParameter));
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(ArchetypeMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.initializerName()).isEqualTo(method.initializerName());
            assertThat(metadata.returnsEntityRefs()).isEqualTo(method.returnsEntityRefs());
            assertThat(metadata.parameters()).isEqualTo(method.parameters());
            assertThat(metadata.enumComponents()).isEqualTo(method.enumComponents());
            assertThat(metadata.components()).isEqualTo(method.components());

            var utilityMetadata = assertThat(UtilityMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(ArchetypeMethod.class)).actual();
            assertThat(utilityMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(utilityMetadata.initializerName()).isEqualTo(method.initializerName());
            assertThat(utilityMetadata.returnsEntityRefs()).isEqualTo(method.returnsEntityRefs());
            assertThat(utilityMetadata.parameters()).isEqualTo(method.parameters());
            assertThat(utilityMetadata.enumComponents()).isEqualTo(method.enumComponents());
            assertThat(utilityMetadata.components()).isEqualTo(method.components());
        }

        @Test
        void testTransmuteMethod() throws IOException {
            var writer = createWriter();

            var parameter = new Parameter(TypeName.INT, "num");
            var enumParameter = new ComponentParameter(null, "foo", new EnumComponent(null, null, ClassName.get("foo", "Bar")), "foo");
            var componentParameter = new ComponentParameter(null, "bar", new ClassComponent(null, null, ClassName.get("foo", "Bar")), "bar");
            var remove = ClassName.get("foo", "Baz");

            var method = new TransmuteMethod(null, null, "foo", "bar", ClassName.get("bz", "Quux"), List.of(parameter), List.of(enumParameter), List.of(componentParameter), List.of(remove));
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(TransmuteMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.initializerName()).isEqualTo(method.initializerName());
            assertThat(metadata.parameters()).isEqualTo(method.parameters());
            assertThat(metadata.enumComponents()).isEqualTo(method.enumComponents());
            assertThat(metadata.components()).isEqualTo(method.components());
            assertThat(metadata.remove()).isEqualTo(method.remove());

            var utilityMetadata = assertThat(UtilityMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(TransmuteMethod.class)).actual();
            assertThat(utilityMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(utilityMetadata.initializerName()).isEqualTo(method.initializerName());
            assertThat(utilityMetadata.parameters()).isEqualTo(method.parameters());
            assertThat(utilityMetadata.enumComponents()).isEqualTo(method.enumComponents());
            assertThat(utilityMetadata.components()).isEqualTo(method.components());
            assertThat(utilityMetadata.remove()).isEqualTo(method.remove());
        }

        @Test
        void testCountMethod() throws IOException {
            var writer = createWriter();

            var method = new CountMethod(null, "foo", new CompositionData(List.of(), null, null));
            method.writeMetadata(writer);

            var metadata = assertThat(EcsMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(CountMethod.class)).actual();
            assertThat(metadata.methodName()).isEqualTo(method.methodName());
            assertThat(metadata.composition()).isEqualTo(method.composition());

            var utilityMetadata = assertThat(UtilityMethod.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(CountMethod.class)).actual();
            assertThat(utilityMetadata.methodName()).isEqualTo(method.methodName());
            assertThat(utilityMetadata.composition()).isEqualTo(method.composition());
        }

        static Stream<List<SystemParameterData>> systemParameters() {
            return Stream.of(
                    List.of(),
                    List.of(new WorldParameter(null, "foo")),
                    List.of(new ValueParameter(null, "foo", TypeName.INT, "bar")),
                    List.of(new UtilityParameter(null, "foo", ClassName.get("foo", "Bar"), "bar")),
                    List.of(new SingletonParameter(null, "foo", ClassName.get("foo", "Bar"), "bar")));
        }

        static Stream<List<? extends ParameterData>> parameters() {
            return Stream.concat(systemParameters(), Stream.of(
                    List.of(new EntityIdParameter(null, "foo")),
                    List.of(new EntityParameter(null, "foo")),
                    List.of(new ComponentParameter(null, "foo", new EnumComponent(null, null, ClassName.get("foo", "Bar")), "bar"))));
        }

    }

}
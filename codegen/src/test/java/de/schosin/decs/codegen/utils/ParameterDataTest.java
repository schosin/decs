package de.schosin.decs.codegen.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

import de.schosin.decs.codegen.AbstractManifestTest;
import de.schosin.decs.codegen.components.ComponentData;
import de.schosin.decs.codegen.components.ComponentData.ClassComponent;
import de.schosin.decs.codegen.components.ComponentData.EnumComponent;
import de.schosin.decs.codegen.components.ComponentData.SingletonEnumComponent;
import de.schosin.decs.codegen.utils.ParameterData.ComponentParameter;
import de.schosin.decs.codegen.utils.ParameterData.EntityIdParameter;
import de.schosin.decs.codegen.utils.ParameterData.EntityParameter;
import de.schosin.decs.codegen.utils.ParameterData.InvalidParameter;
import de.schosin.decs.codegen.utils.ParameterData.SingletonParameter;
import de.schosin.decs.codegen.utils.ParameterData.SystemParameterData;
import de.schosin.decs.codegen.utils.ParameterData.UtilityParameter;
import de.schosin.decs.codegen.utils.ParameterData.ValueParameter;
import de.schosin.decs.codegen.utils.ParameterData.WorldParameter;

class ParameterDataTest {

    @Test
    void testInvalidParameterThrows() {
        var parameter = new InvalidParameter(null, "invalid");

        assertThatThrownBy(parameter::type).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> parameter.entityAccessor(CodeBlock.builder(), "foo", "bar", null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    class MetadataTest extends AbstractManifestTest {

        @Test
        void testUnknownType() {
            var reader = createReader("""
                    UNKNOWN_TYPE
                    This line does not matter
                    """);

            assertThatThrownBy(() -> ParameterData.readMetadata(reader))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown parameter type 'UNKNOWN_TYPE'. Perform a clean build.");
        }

        @Test
        void testInvalidParameter() {
            var writer = createWriter();

            var parameter = new InvalidParameter(null, "invalid");

            assertThatThrownBy(() -> parameter.writeMetadata(writer)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(writer.toString()).isEmpty();
        }

        @Test
        void testWorldParameter() throws IOException {
            var writer = createWriter();

            var parameter = new WorldParameter(null, "foo");
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(WorldParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());

            var systemMetadata = assertThat(SystemParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(WorldParameter.class)).actual();
            assertThat(systemMetadata.name()).isEqualTo(parameter.name());
        }

        @ParameterizedTest
        @MethodSource("typeNames")
        void testValueParameter(TypeName type) throws IOException {
            var writer = createWriter();

            var parameter = new ValueParameter(null, "foo", type, "Bar");
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(ValueParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());
            assertThat(metadata.type()).isEqualTo(parameter.type());
            assertThat(metadata.valueName()).isEqualTo(parameter.valueName());

            var systemMetadata = assertThat(SystemParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(ValueParameter.class)).actual();
            assertThat(systemMetadata.name()).isEqualTo(parameter.name());
            assertThat(systemMetadata.type()).isEqualTo(parameter.type());
            assertThat(systemMetadata.valueName()).isEqualTo(parameter.valueName());
        }

        @ParameterizedTest
        @MethodSource("classNames")
        void testUtilityParameter(ClassName type) throws IOException {
            var writer = createWriter();

            var parameter = new UtilityParameter(null, "foo", type);
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(UtilityParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());
            assertThat(metadata.type()).isEqualTo(parameter.type());
            assertThat(metadata.fieldName()).isEqualTo(parameter.fieldName());

            var systemMetadata = assertThat(SystemParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(UtilityParameter.class)).actual();
            assertThat(systemMetadata.name()).isEqualTo(parameter.name());
            assertThat(systemMetadata.type()).isEqualTo(parameter.type());
            assertThat(systemMetadata.fieldName()).isEqualTo(parameter.fieldName());
        }

        @ParameterizedTest
        @MethodSource("classNames")
        void testSingletonParameter(ClassName type) throws IOException {
            var writer = createWriter();

            var parameter = new SingletonParameter(null, "foo", type);
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(SingletonParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());
            assertThat(metadata.type()).isEqualTo(parameter.type());
            assertThat(metadata.fieldName()).isEqualTo(parameter.fieldName());

            var systemMetadata = assertThat(SystemParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(SingletonParameter.class)).actual();
            assertThat(systemMetadata.name()).isEqualTo(parameter.name());
            assertThat(systemMetadata.type()).isEqualTo(parameter.type());
            assertThat(systemMetadata.fieldName()).isEqualTo(parameter.fieldName());
        }

        @Test
        void testEntityIdParameter() throws IOException {
            var writer = createWriter();

            var parameter = new EntityIdParameter(null, "foo");
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(EntityIdParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());
        }

        @Test
        void testEntityParameter() throws IOException {
            var writer = createWriter();

            var parameter = new EntityParameter(null, "foo");
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(EntityParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());
        }

        @ParameterizedTest
        @MethodSource("components")
        void testComponentParameter(ComponentData data) throws IOException {
            var writer = createWriter();

            var parameter = new ComponentParameter(null, "foo", data, "bar");
            parameter.writeMetadata(writer);

            var metadata = assertThat(ParameterData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(ComponentParameter.class)).actual();
            assertThat(metadata.name()).isEqualTo(parameter.name());
            assertThat(metadata.data()).isEqualTo(parameter.data());
            assertThat(metadata.fieldName()).isEqualTo(parameter.fieldName());
        }

        static Stream<TypeName> typeNames() {
            return Stream.of(
                    TypeName.INT,
                    ClassName.get(String.class),
                    ClassName.get(List.class),
                    ClassName.get("foo", "Bar"),
                    ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get("foo", "Bar")),
                    ParameterizedTypeName.get(ClassName.get(List.class), ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get("foo", "Bar"))),
                    ArrayTypeName.of(TypeName.INT),
                    ArrayTypeName.of(ClassName.get("foo", "Bar")),
                    ArrayTypeName.of(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get("foo", "Bar"))));
        }

        static Stream<ClassName> classNames() {
            return Stream.of(
                    ClassName.get(String.class),
                    ClassName.get(List.class),
                    ClassName.get("foo", "Bar"));
        }

        static Stream<ComponentData> components() {
            return Stream.of(
                    new ClassComponent(null, null, ClassName.get("foo", "Bar")),
                    new EnumComponent(null, null, ClassName.get("foo", "Bar")),
                    new SingletonEnumComponent(null, null, ClassName.get("foo", "Bar"), "INSTANCE"));
        }

    }

}

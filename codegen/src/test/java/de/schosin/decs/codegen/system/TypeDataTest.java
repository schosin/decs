package de.schosin.decs.codegen.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.palantir.javapoet.ClassName;

import de.schosin.decs.codegen.AbstractManifestTest;
import de.schosin.decs.codegen.system.TypeData.SystemData;
import de.schosin.decs.codegen.system.TypeData.UtilityData;
import de.schosin.decs.codegen.system.methods.CallbackMethod.InsertedMethod;
import de.schosin.decs.codegen.system.methods.SystemMethod.SystemProcessorMethod;
import de.schosin.decs.codegen.system.methods.UtilityMethod.CountMethod;

class TypeDataTest {

    @Nested
    class MetadataTest extends AbstractManifestTest {

        @Test
        void testUnknownType() {
            var reader = createReader("""
                    UNKNOWN_TYPE
                    This line does not matter
                    """);

            assertThatThrownBy(() -> TypeData.readMetadata(reader, generator))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown type 'UNKNOWN_TYPE'. Perform a clean build.");

            verifyNoInteractions(generator);
        }

        @ParameterizedTest
        @MethodSource("systemData")
        void testSystemData(SystemData data) throws IOException {
            var writer = createWriter();
            data.writeMetadata(writer);

            var metadata = assertThat(TypeData.readMetadata(createReader(writer), generator)).asInstanceOf(InstanceOfAssertFactories.type(SystemData.class)).actual();
            assertThat(metadata.className()).isEqualTo(data.className());
            assertThat(metadata.composition()).isEqualTo(data.composition());
            assertThat(metadata.methods()).isEqualTo(data.methods());
            assertThat(metadata.callbacks()).isEqualTo(data.callbacks());
            assertThat(metadata.utils()).isEqualTo(data.utils());

            verifyNoInteractions(generator);
        }

        static Stream<SystemData> systemData() {
            var composition = new CompositionData(List.of(), null, null);

            return Stream.of(
                    new SystemData(null, ClassName.get("foo", "Bar"), composition, List.of(new SystemProcessorMethod(null, "foo", List.of())), List.of(), List.of()),
                    new SystemData(null, ClassName.get("foo", "Bar"), composition, List.of(), List.of(new InsertedMethod(null, "foo", null, List.of())), List.of()),
                    new SystemData(null, ClassName.get("foo", "Bar"), composition, List.of(), List.of(), List.of(new CountMethod(null, "foo", null))));
        }

        @ParameterizedTest
        @MethodSource("utilityData")
        void testUtilityData(UtilityData data) throws IOException {
            var writer = createWriter();
            data.writeMetadata(writer);

            var metadata = assertThat(TypeData.readMetadata(createReader(writer), generator)).asInstanceOf(InstanceOfAssertFactories.type(UtilityData.class)).actual();
            assertThat(metadata.className()).isEqualTo(data.className());
            assertThat(metadata.utils()).isEqualTo(data.utils());

            verifyNoInteractions(generator);
        }

        static Stream<UtilityData> utilityData() {
            return Stream.of(
                    new UtilityData(null, ClassName.get("foo", "Bar"), List.of(new CountMethod(null, "foo", null))));
        }

    }

}
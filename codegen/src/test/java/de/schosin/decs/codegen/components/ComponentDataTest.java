package de.schosin.decs.codegen.components;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.palantir.javapoet.ClassName;

import de.schosin.decs.codegen.AbstractManifestTest;
import de.schosin.decs.codegen.components.ComponentData.ClassComponent;
import de.schosin.decs.codegen.components.ComponentData.EnumComponent;
import de.schosin.decs.codegen.components.ComponentData.SingletonEnumComponent;

class ComponentDataTest {

    @Nested
    class MetadataTest extends AbstractManifestTest {

        @Test
        void testClassComponent() throws IOException {
            var writer = createWriter();

            var component = new ClassComponent(null, null, ClassName.get("foo", "Bar"));
            component.writeMetadata(writer);

            var metadata = assertThat(ComponentData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(ClassComponent.class)).actual();
            assertThat(metadata.className()).isEqualTo(component.className());
        }

        @Test
        void testEnumComponent() throws IOException {
            var writer = createWriter();

            var component = new EnumComponent(null, null, ClassName.get("foo", "Bar"));
            component.writeMetadata(writer);

            var metadata = assertThat(ComponentData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(EnumComponent.class)).actual();
            assertThat(metadata.className()).isEqualTo(component.className());
        }

        @Test
        void testSingletonEnumComponent() throws IOException {
            var writer = createWriter();

            var component = new SingletonEnumComponent(null, null, ClassName.get("foo", "Bar"), "INSTANCE");
            component.writeMetadata(writer);

            var metadata = assertThat(ComponentData.readMetadata(createReader(writer))).asInstanceOf(InstanceOfAssertFactories.type(SingletonEnumComponent.class)).actual();
            assertThat(metadata.className()).isEqualTo(component.className());
            assertThat(metadata.instance()).isEqualTo(component.instance());
        }

    }

}

package de.schosin.decs.codegen.utils.archetype;

import com.palantir.javapoet.ClassName;
import de.schosin.decs.codegen.utils.ParsedType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Targets exactly one archetype defined by the components.
 *
 * @param components components of the archetype
 */
public record ArchetypeTarget(List<ClassName> components) implements SystemMethodTarget {

    static final String TYPE = "ARCHETYPE";

    @Override
    public void writeMetadata(Writer writer) throws IOException {
        writer.append(TYPE).append(System.lineSeparator());
        writer.append(Integer.toString(components.size())).append(System.lineSeparator());

        for (var component : components) {
            writer.append(component.toString()).append(System.lineSeparator());
        }
    }

    static ArchetypeTarget readMetadata(BufferedReader reader) throws IOException {
        var size = Integer.parseInt(reader.readLine());
        var components = new ArrayList<ClassName>(size);
        for (int i = 0; i < size; i++) {
            components.add(ParsedType.parse(reader.readLine()).getClassName());
        }

        return new ArchetypeTarget(components);
    }

}

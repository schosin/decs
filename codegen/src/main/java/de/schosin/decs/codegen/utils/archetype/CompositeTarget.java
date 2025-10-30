package de.schosin.decs.codegen.utils.archetype;

import com.palantir.javapoet.ClassName;
import de.schosin.decs.codegen.utils.ParsedType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Composite wrapper for more than one target.
 *
 * @param targets targets
 */
public record CompositeTarget(List<SystemMethodTarget> targets) implements SystemMethodTarget {

    static final String TYPE = "COMPOSITE";

    @Override
    public void writeMetadata(Writer writer) throws IOException {
        writer.append(TYPE).append(System.lineSeparator());

        writer.append(Integer.toString(targets.size())).append(System.lineSeparator());
        for (var target : targets) {
            target.writeMetadata(writer);
        }
    }

    static CompositeTarget readMetadata(BufferedReader reader) throws IOException {
        var size = Integer.parseInt(reader.readLine());
        var targets = new ArrayList<SystemMethodTarget>(size);
        for (int i = 0; i < size; i++) {
            targets.add(SystemMethodTarget.readMetadata(reader));
        }

        return new CompositeTarget(targets);
    }

}

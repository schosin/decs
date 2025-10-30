package de.schosin.decs.codegen.utils.archetype;

import com.palantir.javapoet.ClassName;
import de.schosin.decs.codegen.system.CompositionData;
import de.schosin.decs.codegen.utils.ManifestUtils;
import de.schosin.decs.codegen.utils.ParsedType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines a @Transmute target that targets all archetypes matching the composition and then adding and removing the components of the transmutation.
 *
 * @param composition base archetypes
 * @param add added components
 * @param remove removed components
 */
public record TransmuteTarget(CompositionData composition, List<ClassName> add, List<ClassName> remove) implements SystemMethodTarget {

    static final String TYPE = "TRANSMUTE";

    @Override
    public void writeMetadata(Writer writer) throws IOException {
        writer.append(TYPE).append(System.lineSeparator());
        ManifestUtils.writeCompositionData(composition, writer);

        writer.append(Integer.toString(add.size())).append(System.lineSeparator());
        for (var component : add) {
            writer.append(component.toString()).append(System.lineSeparator());
        }

        writer.append(Integer.toString(remove.size())).append(System.lineSeparator());
        for (var component : remove) {
            writer.append(component.toString()).append(System.lineSeparator());
        }
    }

    static TransmuteTarget readMetadata(BufferedReader reader) throws IOException {
        var composition = ManifestUtils.readCompositionData(reader, "TransmuteTarget");

        var addSize = Integer.parseInt(reader.readLine());
        var add = new ArrayList<ClassName>(addSize);
        for (int i = 0; i < addSize; i++) {
            add.add(ParsedType.parse(reader.readLine()).getClassName());
        }

        var removeSize = Integer.parseInt(reader.readLine());
        var remove = new ArrayList<ClassName>(removeSize);
        for (int i = 0; i < removeSize; i++) {
            remove.add(ParsedType.parse(reader.readLine()).getClassName());
        }

        return new TransmuteTarget(composition, add, remove);
    }

}

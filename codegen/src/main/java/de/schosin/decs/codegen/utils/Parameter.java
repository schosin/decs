package de.schosin.decs.codegen.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

import com.palantir.javapoet.TypeName;

public record Parameter(TypeName type, String name) {

    public void writeMetadata(Writer writer) throws IOException {
        writer.append(type.toString()).append(System.lineSeparator());
        writer.append(name).append(System.lineSeparator());
    }

    public static Parameter readMetadata(BufferedReader reader) throws IOException {
        var type = ParsedType.parse(reader.readLine()).getTypeName();
        var name = reader.readLine();

        return new Parameter(type, name);
    }

}

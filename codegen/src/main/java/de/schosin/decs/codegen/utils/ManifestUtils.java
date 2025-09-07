package de.schosin.decs.codegen.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.lang.model.element.TypeElement;

import com.palantir.javapoet.TypeName;

import de.schosin.decs.codegen.system.CompositionData;

public class ManifestUtils {

    private static final String NULL = "<null>";

    public static void writeTypeElement(TypeElement element, Writer writer) throws IOException {
        writer.append(element.getQualifiedName().toString()).append(System.lineSeparator());
    }

    public static TypeElement readTypeElement(BufferedReader reader, AbstractGenerator generator) throws IOException {
        return generator.elements.getTypeElement(reader.readLine());
    }

    public static void writeCompositionData(CompositionData composition, Writer writer) throws IOException {
        CompositionDataUtils.writeCompositionData(composition, writer);
    }

    public static CompositionData readCompositionData(BufferedReader reader, String source) throws IOException {
        return CompositionDataUtils.readCompositionData(reader, source);
    }

    public static void writeTypeNameInline(TypeName type, Writer writer) {
    }

    private static class CompositionDataUtils {

        private static final Pattern LINE = Pattern.compile("all\\(([^)]*)\\) ones\\(([^)]+)\\) none\\(([^)]+)\\)");

        public static void writeCompositionData(CompositionData composition, Writer writer) throws IOException {
            if (composition == null) {
                writer.append(NULL).append(System.lineSeparator());
                return;
            }

            writer.append("all(");
            if (composition.all() != null) {
                for (var all : composition.all()) {
                    writer.append(all.toString()).append(":");
                }
            } else {
                writer.append(NULL);
            }
            writer.append(") ");

            writer.append("ones(");
            if (composition.ones() != null) {
                for (var ones : composition.ones()) {
                    for (var one : ones) {
                        writer.append(one.toString()).append(":");
                    }
                    writer.append(";");
                }
            } else {
                writer.append(NULL);
            }
            writer.append(") ");

            writer.append("none(");
            if (composition.none() != null) {
                for (var none : composition.none()) {
                    writer.append(none.toString()).append(":");
                }
            } else {
                writer.append(NULL);
            }
            writer.append(")");

            writer.append(System.lineSeparator());
        }

        public static CompositionData readCompositionData(BufferedReader reader, String source) throws IOException {
            var line = reader.readLine();
            if (NULL.equals(line)) {
                return null;
            }

            var lineMatcher = LINE.matcher(line);
            if (!lineMatcher.matches()) {
                throw new IllegalStateException("Failed to read composition data for %s: %s".formatted(source, line));
            }

            var allValue = lineMatcher.group(1).split(":");
            List<TypeName> all = null;

            var allNull = allValue.length == 1 && NULL.equals(allValue[0]);
            if (!allNull) {
                all = new ArrayList<TypeName>(allValue.length);
                if (allValue.length > 1 || !"".equals(allValue[0])) {
                    for (var value : allValue) {
                        all.add(ParsedType.parse(value).getTypeName());
                    }
                }
            }

            var onesValue = lineMatcher.group(2).split(";");
            List<List<TypeName>> ones = null;

            var onesNull = onesValue.length == 1 && NULL.equals(onesValue[0]);
            if (!onesNull) {
                ones = new ArrayList<List<TypeName>>(onesValue.length);
                for (var value : onesValue) {
                    var oneValue = value.split(":");
                    var one = new ArrayList<TypeName>(oneValue.length);
                    for (var v : oneValue) {
                        one.add(ParsedType.parse(v).getTypeName());
                    }

                    ones.add(one);
                }
            }

            var noneValue = lineMatcher.group(3).split(":");
            List<TypeName> none = null;

            var noneNull = noneValue.length == 1 && NULL.equals(noneValue[0]);
            if (!noneNull) {
                none = new ArrayList<TypeName>(noneValue.length);
                for (var value : noneValue) {
                    none.add(ParsedType.parse(value).getTypeName());
                }
            }

            return new CompositionData(all, ones, none);
        }

    }

}

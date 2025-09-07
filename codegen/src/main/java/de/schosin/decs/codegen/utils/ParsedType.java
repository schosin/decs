package de.schosin.decs.codegen.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.palantir.javapoet.ArrayTypeName;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

public final class ParsedType {

    private final String packageName;
    private final String className;
    private final String[] parents;
    private final List<ParsedType> typeArguments = new ArrayList<>();
    private int arrayDepth;

    private ParsedType(String packageName, String className, String[] parents) {
        this.packageName = packageName;
        this.className = className;
        this.parents = parents;
    }

    public static ParsedType parse(String typeName) {
        return new Parser(typeName).parse();
    }

    public ClassName getClassName() {
        var type = ClassName.get(packageName, className);
        if (parents.length > 0) {
            type = ClassName.get(packageName, parents[0]);
            for (int i = 1, s = parents.length; i < s; i++) {
                type = type.nestedClass(parents[i]);
            }

            type = type.nestedClass(className);
        }

        if (!typeArguments.isEmpty()) {
            throw new IllegalStateException("Cannot get ClassName for '%s', type arguments are present: %s".formatted(className, typeArguments));
        }

        if (arrayDepth > 0) {
            throw new IllegalStateException("Cannot get ClassName for '%s', array type detected: array depth %d".formatted(className, arrayDepth));
        }

        return type;
    }

    public TypeName getTypeName() {
        if (packageName.isEmpty() && Character.isLowerCase(className.charAt(0))) {
            return getPrimitiveTypeName();
        }

        var type = ClassName.get(packageName, className);
        if (typeArguments.isEmpty()) {
            TypeName result = type;

            while (arrayDepth-- > 0) {
                result = ArrayTypeName.of(result);
            }

            return result;
        }

        var parameters = typeArguments.stream().map(ParsedType::getTypeName).toArray(TypeName[]::new);
        TypeName result = ParameterizedTypeName.get(type, parameters);

        while (arrayDepth-- > 0) {
            result = ArrayTypeName.of(result);
        }

        return result;
    }

    private TypeName getPrimitiveTypeName() {
        var type = switch (className) {
            case "boolean" -> TypeName.BOOLEAN;
            case "byte" -> TypeName.BYTE;
            case "short" -> TypeName.SHORT;
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "char" -> TypeName.CHAR;
            case "float" -> TypeName.FLOAT;
            case "double" -> TypeName.DOUBLE;
            default -> throw new IllegalStateException("Unknown primitive '%s'".formatted(className));
        };

        if (!typeArguments.isEmpty()) {
            throw new IllegalStateException("Unexpected type arguments for primitive '%s'".formatted(className));
        }

        while (arrayDepth-- > 0) {
            type = ArrayTypeName.of(type);
        }

        return type;
    }

    private static class Parser {

        private final String input;
        private int pos;

        public Parser(String typeName) {
            this.input = typeName.trim();
        }

        private ParsedType parse() {
            var rawType = parseRawType();
            var type = createParsedType(rawType);

            skipWhitespace();
            if (peek() == '<') {
                consume('<');
                do {
                    skipWhitespace();
                    type.typeArguments.add(parse());
                    skipWhitespace();
                } while (consumeIf(','));
                consume('>');
            }

            while (consumeIf('[')) {
                consume(']');
                type.arrayDepth++;
            }

            return type;
        }

        private ParsedType createParsedType(String rawType) {
            var parts = rawType.split("\\.");
            var classStartIndex = 0;

            // Find index of first class (starts with upper case char)
            for (int i = 0, s = parts.length; i < s; i++) {
                if (Character.isUpperCase(parts[i].charAt(0))) {
                    classStartIndex = i;
                    break;
                }
            }

            var packageName = Arrays.stream(parts).limit(classStartIndex).collect(Collectors.joining("."));
            var className = parts[parts.length - 1];
            var parents = Arrays.stream(parts).skip(classStartIndex).limit(parts.length - classStartIndex - 1).toArray(String[]::new);

            return new ParsedType(packageName, className, parents);
        }

        private String parseRawType() {
            var start = pos;
            while (pos < input.length() && isTypeChar(input.charAt(pos))) {
                pos++;
            }

            return input.substring(start, pos).trim();
        }

        private boolean isTypeChar(char c) {
            return Character.isJavaIdentifierPart(c) || c == '.' || c == '$';
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            return pos < input.length() ? input.charAt(pos) : '\0';
        }

        private void consume(char expected) {
            var next = peek();
            if (next != expected) {
                throw new IllegalArgumentException("Expected '%s' at position %d, but got '%s'".formatted(expected, pos, next));
            }

            pos++;
        }

        private boolean consumeIf(char expected) {
            if (peek() == expected) {
                pos++;
                return true;
            }

            return false;
        }

    }

}

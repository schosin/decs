package de.schosin.decs.codegen;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractManifestTest {

    protected StringWriter createWriter() {
        return new StringWriter();
    }

    protected BufferedReader createReader(StringWriter writer) {
        return createReader(writer.toString());
    }

    protected BufferedReader createReader(String value) {
        return new BufferedReader(new StringReader(value));
    }

}

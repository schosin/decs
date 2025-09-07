package de.schosin.decs.codegen;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schosin.decs.codegen.utils.AbstractGenerator;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractManifestTest {

    @Mock
    protected AbstractGenerator generator;

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

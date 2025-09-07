package foo;

import de.schosin.decs.api.annotations.Value;
import de.schosin.decs.api.annotations.composition.All;
import de.schosin.decs.api.annotations.system.EntityProcessor;
import de.schosin.decs.api.annotations.system.SystemProcessor;

import java.util.List;

public class TestSystem {

    @SystemProcessor
    final void intArray(@Value("intArray") int[] value) {
    }

    @SystemProcessor
    final void integerArray(@Value("integerArray") Integer[] value) {
    }

    @SystemProcessor
    final void integerListArray(@Value("integerListArray") List<Integer>[] value) {
    }

}

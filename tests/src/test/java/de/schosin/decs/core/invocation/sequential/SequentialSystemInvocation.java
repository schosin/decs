package de.schosin.decs.core.invocation.sequential;

import de.schosin.decs.api.annotations.invocation.Systems;
import de.schosin.decs.api.annotations.invocation.SystemGroup;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.core.invocation.sequential.SequentialSystemInvocationTest.SequentialA;
import de.schosin.decs.core.invocation.sequential.SequentialSystemInvocationTest.SequentialB;
import de.schosin.decs.core.invocation.sequential.SequentialSystemInvocationTest.SequentialC;
import de.schosin.decs.core.invocation.sequential.SequentialSystemInvocationTest.SequentialD;

@Systems({
        @SystemGroup(name = "group1", value = {SequentialA.class, SequentialB.class}),
        @SystemGroup(name = "group2", value = {SequentialC.class}),
        @SystemGroup(SequentialD.class)
})
public interface SequentialSystemInvocation extends SystemInvocation {
}

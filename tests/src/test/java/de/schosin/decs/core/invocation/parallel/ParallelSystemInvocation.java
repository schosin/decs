package de.schosin.decs.core.invocation.parallel;

import de.schosin.decs.api.annotations.invocation.Systems;
import de.schosin.decs.api.annotations.invocation.SystemGroup;
import de.schosin.decs.api.annotations.invocation.SystemGroup.InvocationStrategy;
import de.schosin.decs.api.systems.SystemInvocation;
import de.schosin.decs.core.invocation.parallel.ParallelSystemInvocationTest.ParallelA;
import de.schosin.decs.core.invocation.parallel.ParallelSystemInvocationTest.ParallelB;
import de.schosin.decs.core.invocation.parallel.ParallelSystemInvocationTest.ParallelC;
import de.schosin.decs.core.invocation.parallel.ParallelSystemInvocationTest.ParallelD;
import de.schosin.decs.core.invocation.parallel.ParallelSystemInvocationTest.ParallelE;

@Systems({
        @SystemGroup(name = "group1", value = {ParallelA.class, ParallelB.class}),
        @SystemGroup(name = "group2", value = {ParallelC.class, ParallelD.class}, strategy = InvocationStrategy.PARALLEL),
        @SystemGroup(ParallelE.class)
})
public interface ParallelSystemInvocation extends SystemInvocation {
}

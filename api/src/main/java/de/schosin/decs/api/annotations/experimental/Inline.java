package de.schosin.decs.api.annotations.experimental;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.schosin.decs.api.annotations.system.EntityProcessor;

/**
 * Experimental inlining of {@link EntityProcessor @EntityProcessor} methods that contain atleast one interface component.
 * 
 * <h4>Troubleshooting:</h4>
 * 
 * <p>
 * If this causes compilation errors in the generated code, try a full clean build.
 * 
 * <p>
 * If the error persists, report the issue with the entire system method, including annotations, the component in question, and the generated code calling the system method. <br />
 * The generated code can be found either by inspecting the type hierarchy of your system, or the call hierarchy of the method in question.
 * 
 * <h4>Technical details:</h4>
 * 
 * <p>
 * When inlining the method body of a processor into the generated code, calls to the getters and setters of interface components will be replaced with direct access to the underlying storage. <br />
 * This enables the following optimizations:
 * 
 * <ul>
 *  <li><b>Avoids write to an index field per entity</b>: No additional cost per entity being iterated</li>
 *  <li><b>Avoids read of an index field per method call</b>: No additional cost per getter or setter call</li>
 *  <li><b>Avoids dereference of the component instance</b>: No additional cost by pointer chasing per getter or setter call</li>
 * </ul>
 * 
 * To be able to inline the code, the annotation processor needs access to the methods source code. <br />
 * This is achieved either by using {@link com.sun.source.util.Trees Trees} when {@code javac} is used, or by attempting to resolve the source root based on common Maven or Gradle setups. <br />
 * When the annotation processor emits warnings that is has no access to the source files, you can provide the annotation processor argument {@code -AdecsSourceRoots}, which accepts a comma seperated list of source roots.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.CLASS)
public @interface Inline {
    
    /**
     * Enables some experimental optimizations.
     * 
     * <h4>Compound operators</h4>
     * 
     * <p>
     * Optimizes assignments that can be replaced with compound assignments such as "{@code +=}".
     * Optimization only possible if the expressions to the left and to the right of the assignment "{@code =}" are identical.
     * Implemented for most operators, see class {@code InlineOptimizations} in the codegen module.
     * 
     * <pre>
     * {@code
     * // User code 
     * pos.x(pos.x() + vel.vx());
     * 
     * // Unoptimized
     * positionData_x[_i] = positionData_x[_i] + velocityData_vx[_i];
     * 
     * // Optimized
     * positionData_x[_i] += velocityData_vx[_i];
     * }
     * </pre>
     */
    boolean optimize() default false;
    
}

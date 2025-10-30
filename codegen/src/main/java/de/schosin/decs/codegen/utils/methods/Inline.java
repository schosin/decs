package de.schosin.decs.codegen.utils.methods;

/**
 * Data holder for de.schosin.decs.api.annotations.experimental.Inline.
 *
 * @param enabled enable inlining of processor method
 * @param optimize optimize transformed code
 */
public record Inline(boolean enabled, boolean optimize) {
}

package io.leangen.graphql.generator.mapping;

import java.lang.reflect.AnnotatedType;

/**
 * The common interface for mappers and converters that perform their work by substituting the given type for another.
 */
public interface TypeSubstituter {

    /**
     * Returns the type that should be used in place of the original
     *
     * @param original The type to be replaced
     * @return The substitute type
     */
    AnnotatedType getSubstituteType(AnnotatedType original);
}

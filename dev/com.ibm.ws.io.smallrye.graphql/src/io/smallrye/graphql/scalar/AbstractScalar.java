// https://github.com/smallrye/smallrye-graphql/blob/4a47a2da6e4f4b4a6aedf2fc6464510737fe4c52/server/implementation/src/main/java/io/smallrye/graphql/scalar/AbstractScalar.java
// Apache v2.0 licensed - https://github.com/smallrye/smallrye-graphql/blob/1.0.9/LICENSE
package io.smallrye.graphql.scalar;

import java.util.Arrays;
import java.util.List;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

/**
 * Base Scalar for all of our own scalars
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public abstract class AbstractScalar {

    private final String name;
    private final Coercing coercing;
    private final List<Class> supportedTypes;
    private final GraphQLScalarType graphQLScalarType;

    public AbstractScalar(String name,
            Coercing coercing,
            Class... supportedTypes) {

        this.name = name;
        this.coercing = coercing;
        this.supportedTypes = Arrays.asList(supportedTypes);
        this.graphQLScalarType = GraphQLScalarType.newScalar()
                .name(name)
                .description("Scalar for " + name)
                .coercing(coercing)
                .build();
    }

    public String getName() {
        return this.name;
    }

    public Coercing getCoercing() {
        return this.coercing;
    }

    public List<Class> getSupportedClasses() {
        return this.supportedTypes;
    }

    public GraphQLScalarType getScalarType() {
        return graphQLScalarType;
    }
}
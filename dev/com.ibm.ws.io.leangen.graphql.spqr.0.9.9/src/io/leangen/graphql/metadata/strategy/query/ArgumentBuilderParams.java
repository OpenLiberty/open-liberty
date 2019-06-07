package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.metadata.strategy.type.TypeTransformer;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class ArgumentBuilderParams {

    private final Method resolverMethod;
    private final AnnotatedType declaringType;
    private final InclusionStrategy inclusionStrategy;
    private final TypeTransformer typeTransformer;
    private final GlobalEnvironment environment;

    ArgumentBuilderParams(Method resolverMethod, AnnotatedType declaringType, InclusionStrategy inclusionStrategy, TypeTransformer typeTransformer, GlobalEnvironment environment) {
        this.resolverMethod = Objects.requireNonNull(resolverMethod);
        this.declaringType = Objects.requireNonNull(declaringType);
        this.inclusionStrategy = Objects.requireNonNull(inclusionStrategy);
        this.typeTransformer = Objects.requireNonNull(typeTransformer);
        this.environment = Objects.requireNonNull(environment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Method getResolverMethod() {
        return resolverMethod;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public InclusionStrategy getInclusionStrategy() {
        return inclusionStrategy;
    }

    public TypeTransformer getTypeTransformer() {
        return typeTransformer;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public static class Builder {
        private Method resolverMethod;
        private AnnotatedType declaringType;
        private InclusionStrategy inclusionStrategy;
        private TypeTransformer typeTransformer;
        private GlobalEnvironment environment;

        public Builder withResolverMethod(Method resolverMethod) {
            this.resolverMethod = resolverMethod;
            return this;
        }

        public Builder withDeclaringType(AnnotatedType declaringType) {
            this.declaringType = declaringType;
            return this;
        }

        public Builder withInclusionStrategy(InclusionStrategy inclusionStrategy) {
            this.inclusionStrategy = inclusionStrategy;
            return this;
        }

        public Builder withTypeTransformer(TypeTransformer typeTransformer) {
            this.typeTransformer = typeTransformer;
            return this;
        }

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public ArgumentBuilderParams build() {
            return new ArgumentBuilderParams(resolverMethod, declaringType, inclusionStrategy, typeTransformer, environment);
        }
    }
}

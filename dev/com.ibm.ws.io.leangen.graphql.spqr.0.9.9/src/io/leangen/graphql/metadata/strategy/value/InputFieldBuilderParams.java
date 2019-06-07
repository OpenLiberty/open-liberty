package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;

import java.lang.reflect.AnnotatedType;
import java.util.Objects;

public class InputFieldBuilderParams {

    private final AnnotatedType type;
    private final GlobalEnvironment environment;

    /**
     * @param type Java type (used as query input) to be analyzed for deserializable fields
     * @param environment The global environment
     */
    private InputFieldBuilderParams(AnnotatedType type, GlobalEnvironment environment) {
        this.type = Objects.requireNonNull(type);
        this.environment = Objects.requireNonNull(environment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public AnnotatedType getType() {
        return type;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public static class Builder {
        private AnnotatedType type;
        private GlobalEnvironment environment;

        public Builder withType(AnnotatedType type) {
            this.type = type;
            return this;
        }

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public InputFieldBuilderParams build() {
            return new InputFieldBuilderParams(type, environment);
        }
    }
}

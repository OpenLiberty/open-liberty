package io.leangen.graphql.execution;

import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.Resolver;

import java.util.function.Consumer;

public class InvocationContext {

    private final Operation operation;
    private final Resolver resolver;
    private final ResolutionEnvironment resolutionEnvironment;
    private final Object[] arguments;

    InvocationContext(Operation operation, Resolver resolver, ResolutionEnvironment resolutionEnvironment, Object[] arguments) {
        this.operation = operation;
        this.resolver = resolver;
        this.resolutionEnvironment = resolutionEnvironment;
        this.arguments = arguments;
    }

    public Operation getOperation() {
        return operation;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public ResolutionEnvironment getResolutionEnvironment() {
        return resolutionEnvironment;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public InvocationContext transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder()
                .withOperation(this.operation)
                .withResolver(this.resolver)
                .withResolutionEnvironment(this.resolutionEnvironment)
                .withArguments(this.arguments);

        builderConsumer.accept(builder);

        return builder.build();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Builder {

        private Operation operation;
        private Resolver resolver;
        private ResolutionEnvironment resolutionEnvironment;
        private Object[] arguments;

        public Builder withOperation(Operation operation) {
            this.operation = operation;
            return this;
        }

        public Builder withResolver(Resolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Builder withResolutionEnvironment(ResolutionEnvironment resolutionEnvironment) {
            this.resolutionEnvironment = resolutionEnvironment;
            return this;
        }

        public Builder withArguments(Object[] arguments) {
            this.arguments = arguments;
            return this;
        }

        public InvocationContext build() {
            return new InvocationContext(operation, resolver, resolutionEnvironment, arguments);
        }
    }
}

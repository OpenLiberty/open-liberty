package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.InputFieldBuilderRegistry;

public class DirectiveBuilderParams {

    private final GlobalEnvironment environment;
    private final InputFieldBuilderRegistry inputFieldBuilders;

    private DirectiveBuilderParams(GlobalEnvironment environment, InputFieldBuilderRegistry inputFieldBuilders) {
        this.environment = environment;
        this.inputFieldBuilders = inputFieldBuilders;
    }

    public GlobalEnvironment getEnvironment() {
        return environment;
    }

    public InputFieldBuilderRegistry getInputFieldBuilders() {
        return inputFieldBuilders;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GlobalEnvironment environment;
        private InputFieldBuilderRegistry inputFieldBuilders;

        public Builder withEnvironment(GlobalEnvironment environment) {
            this.environment = environment;
            return this;
        }

        public Builder withInputFieldBuilders(InputFieldBuilderRegistry inputFieldBuilders) {
            this.inputFieldBuilders = inputFieldBuilders;
            return this;
        }

        public DirectiveBuilderParams build() {
            return new DirectiveBuilderParams(environment, inputFieldBuilders);
        }
    }
}

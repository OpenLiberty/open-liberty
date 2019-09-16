package io.leangen.graphql;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.execution.ContextWrapper;
import io.leangen.graphql.execution.complexity.ComplexityAnalysisInstrumentation;
import io.leangen.graphql.execution.complexity.JavaScriptEvaluator;

/**
 * Wrapper around GraphQL that allows instrumentation chaining and limiting query complexity
 */
public class GraphQLRuntime extends GraphQL {

    private final GraphQL delegate;

    @SuppressWarnings("deprecation")
    private GraphQLRuntime(GraphQL delegate, GraphQLSchema schema) {
        super(schema);
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<ExecutionResult> executeAsync(ExecutionInput executionInput) {
        return delegate.executeAsync(executionInput.transform(builder -> builder.context(new ContextWrapper(executionInput.getContext()))));
    }

    public static Builder newGraphQL(GraphQLSchema graphQLSchema) {
        return new Builder(graphQLSchema);
    }

    public static class Builder extends GraphQL.Builder {

        private GraphQLSchema graphQLSchema;
        private List<Instrumentation> instrumentations;

        private Builder(GraphQLSchema graphQLSchema) {
            super(graphQLSchema);
            this.graphQLSchema = graphQLSchema;
            this.instrumentations = new ArrayList<>();
        }

        @Override
        public Builder instrumentation(Instrumentation instrumentation) {
            this.instrumentations.add(instrumentation);
            return this;
        }

        public Builder maximumQueryComplexity(int limit) {
            instrumentations.add(new ComplexityAnalysisInstrumentation(new JavaScriptEvaluator(), limit));
            return this;
        }

        @Override
        public GraphQLRuntime build() {
            if (instrumentations.size() == 1) {
                super.instrumentation(instrumentations.get(0));
            } else if (!instrumentations.isEmpty()) {
                super.instrumentation(new ChainedInstrumentation(instrumentations));
            }
            return new GraphQLRuntime(super.build(), graphQLSchema);
        }
    }
}

package io.leangen.graphql.generator;

import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import org.dataloader.DataLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class RelayDataFetchingEnvironmentDecorator implements DataFetchingEnvironment {

    private final DataFetchingEnvironment delegate;
    private final Map<String, Object> arguments;

    RelayDataFetchingEnvironmentDecorator(DataFetchingEnvironment delegate) {
        this.delegate = delegate;
        this.arguments = Collections.unmodifiableMap(delegate.<Map<String, Object>>getArgument("input"));
    }

    @Override
    public <T> T getSource() {
        return delegate.getSource();
    }

    @Override
    public Map<String, Object> getArguments() {
        return arguments;
    }

    @Override
    public boolean containsArgument(String name) {
        return arguments.containsKey(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getArgument(String name) {
        return (T) arguments.get(name);
    }

    @Override
    public <T> T getContext() {
        return delegate.getContext();
    }

    @Override
    public <T> T getRoot() {
        return delegate.getRoot();
    }

    @Override
    public GraphQLFieldDefinition getFieldDefinition() {
        return delegate.getFieldDefinition();
    }

    @Override
    public List<Field> getFields() {
        return delegate.getFields();
    }

    @Override
    public Field getField() {
        return delegate.getField();
    }

    @Override
    public GraphQLOutputType getFieldType() {
        return delegate.getFieldType();
    }

    @Override
    public ExecutionStepInfo getExecutionStepInfo() {
        return delegate.getExecutionStepInfo();
    }

    @Override
    public GraphQLType getParentType() {
        return delegate.getParentType();
    }

    @Override
    public GraphQLSchema getGraphQLSchema() {
        return delegate.getGraphQLSchema();
    }

    @Override
    public Map<String, FragmentDefinition> getFragmentsByName() {
        return delegate.getFragmentsByName();
    }

    @Override
    public ExecutionId getExecutionId() {
        return delegate.getExecutionId();
    }

    @Override
    public DataFetchingFieldSelectionSet getSelectionSet() {
        return delegate.getSelectionSet();
    }

    @Override
    public ExecutionContext getExecutionContext() {
        return delegate.getExecutionContext();
    }

    @Override
    public <K, V> DataLoader<K, V> getDataLoader(String dataLoaderName) {
        return delegate.getDataLoader(dataLoaderName);
    }
}

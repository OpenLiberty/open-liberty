package io.smallrye.graphql.execution.context;

import static io.smallrye.graphql.SmallRyeGraphQLServerMessages.msg;

import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.SelectedField;
import io.smallrye.graphql.api.Context;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.schema.model.ReferenceType;
import io.smallrye.graphql.schema.model.Schema;
import io.smallrye.graphql.schema.model.Type;

/**
 * Implements the Context from MicroProfile API.
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class SmallRyeContext implements Context {
    private static Schema schema;

    private static final ThreadLocal<SmallRyeContext> current = new ThreadLocal<>();

    public static void register(JsonObject jsonInput) {
        SmallRyeContext registry = new SmallRyeContext(jsonInput);
        current.set(registry);
    }

    public static void setSchema(Schema schema) {
        SmallRyeContext.schema = schema;
    }

    public static Context getContext() {
        SmallRyeContext context = current.get();
        if (context != null) {
            return context;
        } else {
            throw new ContextNotActiveException();
        }
    }

    public static void remove() {
        current.remove();
    }

    @Override
    public JsonObject getRequest() {
        return jsonObject;
    }

    @Override
    public <T> T unwrap(Class<T> wrappedType) {
        // We only support DataFetchingEnvironment at this point
        if (wrappedType.equals(DataFetchingEnvironment.class)) {
            return (T) this.dfe;
        }
        throw msg.unsupportedWrappedClass(wrappedType.getName());
    }

    @Override
    public boolean hasArgument(String name) {
        return dfe.containsArgument(name);
    }

    @Override
    public <T> T getArgument(String name) {
        return dfe.getArgument(name);
    }

    @Override
    public Map<String, Object> getArguments() {
        return dfe.getArguments();
    }

    @Override
    public String getPath() {
        return dfe.getExecutionStepInfo().getPath().toString();
    }

    @Override
    public String getExecutionId() {
        return dfe.getExecutionId().toString();
    }

    @Override
    public String getFieldName() {
        return dfe.getField().getName();
    }

    @Override
    public <T> T getSource() {
        return dfe.getSource();
    }

    @Override
    public JsonArray getSelectedFields(boolean includeSourceFields) {
        DataFetchingFieldSelectionSet selectionSet = dfe.getSelectionSet();
        List<SelectedField> fields = selectionSet.getFields();
        return toJsonArrayBuilder(fields, includeSourceFields).build();
    }

    private final JsonObject jsonObject;
    private DataFetchingEnvironment dfe;
    private Field field;

    private SmallRyeContext(final JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public void setDataFromFetcher(DataFetchingEnvironment dfe, Field field) {
        this.dfe = dfe;
        this.field = field;
    }

    private JsonArrayBuilder toJsonArrayBuilder(List<SelectedField> fields, boolean includeSourceFields) {
        JsonArrayBuilder builder = jsonbuilder.createArrayBuilder();

        for (SelectedField field : fields) {
            if (!isFlattenScalar(field)) {
                if (includeSourceFields || !isSourceField(field)) {
                    if (isScalar(field)) {
                        builder = builder.add(field.getName());
                    } else {
                        builder = builder.add(toJsonObjectBuilder(field, includeSourceFields));
                    }
                }
            }
        }

        return builder;
    }

    private JsonObjectBuilder toJsonObjectBuilder(SelectedField selectedField, boolean includeSourceFields) {
        JsonObjectBuilder builder = jsonbuilder.createObjectBuilder();
        builder = builder.add(selectedField.getName(),
                toJsonArrayBuilder(selectedField.getSelectionSet().getFields(), includeSourceFields));
        return builder;
    }

    private boolean isSourceField(SelectedField selectedField) {
        if (field.getReference().getType().equals(ReferenceType.TYPE)) {
            Type type = schema.getTypes().get(field.getReference().getName());
            return type.hasOperation(selectedField.getName());
        }
        return false; // Only Type has source field (for now)
    }

    private boolean isScalar(SelectedField field) {
        GraphQLType graphQLType = unwrapGraphQLType(field.getFieldDefinition().getType());
        return isScalar(graphQLType);
    }

    private boolean isScalar(GraphQLType gqlt) {
        return GraphQLScalarType.class.isAssignableFrom(gqlt.getClass());
    }

    private GraphQLType unwrapGraphQLType(GraphQLType gqlt) {
        if (isNonNull(gqlt)) {
            GraphQLNonNull graphQLNonNull = (GraphQLNonNull) gqlt;
            return unwrapGraphQLType(graphQLNonNull.getWrappedType());
        } else if (isList(gqlt)) {
            GraphQLList graphQLList = (GraphQLList) gqlt;
            return unwrapGraphQLType(graphQLList.getWrappedType());
        }
        return gqlt;
    }

    private boolean isNonNull(GraphQLType gqlt) {
        return GraphQLNonNull.class.isAssignableFrom(gqlt.getClass());
    }

    private boolean isList(GraphQLType gqlt) {
        return GraphQLList.class.isAssignableFrom(gqlt.getClass());
    }

    private boolean isFlattenScalar(SelectedField field) {
        return field.getQualifiedName().contains("/");
    }

    // Liberty change - added "static " - this is part of SmallRye-GraphQL #350/351 which will be in 1.0.7 
    private static final JsonBuilderFactory jsonbuilder = Json.createBuilderFactory(null);
}

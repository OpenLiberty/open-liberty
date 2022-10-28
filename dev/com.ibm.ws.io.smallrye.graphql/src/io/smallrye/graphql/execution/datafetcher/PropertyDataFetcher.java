// https://github.com/smallrye/smallrye-graphql/blob/1e1000a4b8aba36b180f8ceedb4c2357e0df974c/server/implementation/src/main/java/io/smallrye/graphql/execution/datafetcher/PropertyDataFetcher.java
// Apache v2.0 licensed - https://github.com/smallrye/smallrye-graphql/blob/1.0.9/LICENSE
// Liberty Change - Recompiling with graphql-java 19.2 APIs
package io.smallrye.graphql.execution.datafetcher;

import static io.smallrye.graphql.SmallRyeGraphQLServerLogging.log;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.execution.datafetcher.helper.FieldHelper;
import io.smallrye.graphql.schema.model.Field;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;

/**
 * Extending the default property data fetcher to intercept the result for some manipulation
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public class PropertyDataFetcher extends graphql.schema.PropertyDataFetcher {

    private final FieldHelper fieldHelper;
    private final Field field;

    public PropertyDataFetcher(Field field) {
        super(field.getPropertyName());
        this.field = field;
        this.fieldHelper = new FieldHelper(field);
    }

    @FFDCIgnore(value = { AbstractDataFetcherException.class }) // Liberty Change
    @Override
    public Object get(DataFetchingEnvironment dfe) {
        GraphQLContext graphQLContext = dfe.getContext();
        graphQLContext.put("context", ((SmallRyeContext) graphQLContext.get("context")).withDataFromFetcher(dfe, field));

        Object resultFromMethodCall = super.get(dfe);
        try {
            // See if we need to transform
            return fieldHelper.transformResponse(resultFromMethodCall);
        } catch (AbstractDataFetcherException ex) {
            log.transformError(ex);
            return resultFromMethodCall;
        }
    }
}
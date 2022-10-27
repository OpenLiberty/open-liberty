// https://github.com/smallrye/smallrye-graphql/blob/1e1000a4b8aba36b180f8ceedb4c2357e0df974c/server/implementation/src/main/java/io/smallrye/graphql/execution/datafetcher/AbstractDataFetcher.java
// Apache v2.0 licensed - https://github.com/smallrye/smallrye-graphql/blob/1.0.9/LICENSE
// Liberty Change - Recompiling with graphql-java 19.2 APIs
package io.smallrye.graphql.execution.datafetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import org.dataloader.BatchLoaderWithContext;
import org.eclipse.microprofile.graphql.GraphQLException;

import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.smallrye.graphql.bootstrap.Config;
import io.smallrye.graphql.execution.context.SmallRyeContext;
import io.smallrye.graphql.execution.datafetcher.helper.ArgumentHelper;
import io.smallrye.graphql.execution.datafetcher.helper.BatchLoaderHelper;
import io.smallrye.graphql.execution.datafetcher.helper.FieldHelper;
import io.smallrye.graphql.execution.datafetcher.helper.PartialResultHelper;
import io.smallrye.graphql.execution.datafetcher.helper.ReflectionHelper;
import io.smallrye.graphql.execution.event.EventEmitter;
import io.smallrye.graphql.schema.model.Operation;
import io.smallrye.graphql.transformation.AbstractDataFetcherException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * The abstract data fetcher
 *
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 * @param <K>
 * @param <T>
 */
public abstract class AbstractDataFetcher<K, T> implements DataFetcher<T>, BatchLoaderWithContext<K, T> {

    protected Operation operation;
    protected FieldHelper fieldHelper;
    protected ReflectionHelper reflectionHelper;
    protected PartialResultHelper partialResultHelper;
    protected ArgumentHelper argumentHelper;
    protected EventEmitter eventEmitter;
    protected BatchLoaderHelper batchLoaderHelper;
    protected List<String> unwrapExceptions = new ArrayList<>();

    public AbstractDataFetcher(Operation operation, Config config) {
        this.operation = operation;
        this.eventEmitter = EventEmitter.getInstance(config);
        this.fieldHelper = new FieldHelper(operation);
        this.reflectionHelper = new ReflectionHelper(operation, eventEmitter);
        this.argumentHelper = new ArgumentHelper(operation.getArguments());
        this.partialResultHelper = new PartialResultHelper();
        this.batchLoaderHelper = new BatchLoaderHelper();
        if (config != null && config.getUnwrapExceptions().isPresent()) {
            this.unwrapExceptions.addAll(config.getUnwrapExceptions().get());
        }
        this.unwrapExceptions.addAll(DEFAULT_EXCEPTION_UNWRAP);
    }

    @FFDCIgnore(value = { AbstractDataFetcherException.class, GraphQLException.class, IllegalArgumentException.class }) // Liberty Change
    @Override
    public T get(final DataFetchingEnvironment dfe) throws Exception {
        // update the context
        GraphQLContext graphQLContext = dfe.getContext();
        SmallRyeContext context = ((SmallRyeContext) graphQLContext.get("context")).withDataFromFetcher(dfe, operation);
        graphQLContext.put("context", context);

        final DataFetcherResult.Builder<Object> resultBuilder = DataFetcherResult.newResult().localContext(graphQLContext);

        eventEmitter.fireBeforeDataFetch(context);

        try {
            Object[] transformedArguments = argumentHelper.getArguments(dfe);

            return invokeAndTransform(dfe, resultBuilder, transformedArguments);
        } catch (AbstractDataFetcherException abstractDataFetcherException) {
            //Arguments or result couldn't be transformed
            abstractDataFetcherException.appendDataFetcherResult(resultBuilder, dfe);
            eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), abstractDataFetcherException);
        } catch (GraphQLException graphQLException) {
            partialResultHelper.appendPartialResult(resultBuilder, dfe, graphQLException);
            eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), graphQLException);
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException ex) {
            //m.invoke failed
            eventEmitter.fireOnDataFetchError(dfe.getExecutionId().toString(), ex);
            throw ex;
        } finally {
            eventEmitter.fireAfterDataFetch(context);
        }

        return invokeFailure(resultBuilder);
    }

    protected abstract <T> T invokeAndTransform(DataFetchingEnvironment dfe, DataFetcherResult.Builder<Object> resultBuilder,
            Object[] transformedArguments) throws AbstractDataFetcherException, Exception;

    protected abstract <T> T invokeFailure(DataFetcherResult.Builder<Object> resultBuilder);

    protected Throwable unwrapThrowable(Throwable t) {
        if (shouldUnwrapThrowable(t)) {
            t = t.getCause();
            return unwrapThrowable(t);
        }
        return t;
    }

    private boolean shouldUnwrapThrowable(Throwable t) {
        return unwrapExceptions.contains(t.getClass().getName()) && t.getCause() != null;
    }

    private static final List<String> DEFAULT_EXCEPTION_UNWRAP = new ArrayList<>();

    static {
        DEFAULT_EXCEPTION_UNWRAP.add(CompletionException.class.getName());
        DEFAULT_EXCEPTION_UNWRAP.add("javax.ejb.EJBException");
        DEFAULT_EXCEPTION_UNWRAP.add("jakarta.ejb.EJBException");
    }
}
package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.annotations.Subscription;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created by bojan.tomic on 7/3/16.
 */
public class AnnotatedOperationNameGenerator implements OperationNameGenerator {

    @Override
    public String generateQueryName(OperationNameGeneratorParams<?> params) {
        return Optional.ofNullable(params.getElement().getAnnotation(Query.class))
                .map(query -> params.getMessageBundle().interpolate(query.value()))
                .orElse(null);
    }

    @Override
    public String generateMutationName(OperationNameGeneratorParams<Method> params) {
        return Optional.ofNullable(params.getElement().getAnnotation(Mutation.class))
                .map(mutation -> params.getMessageBundle().interpolate(mutation.value()))
                .orElse(null);
    }

    @Override
    public String generateSubscriptionName(OperationNameGeneratorParams<Method> params) {
        return Optional.ofNullable(params.getElement().getAnnotation(Subscription.class))
                .map(subscription -> params.getMessageBundle().interpolate(subscription.value()))
                .orElse(null);
    }
}

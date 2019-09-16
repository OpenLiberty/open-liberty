package io.leangen.graphql.metadata.exceptions;

import graphql.GraphQLException;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class UnresolvableTypeException extends GraphQLException {

    public UnresolvableTypeException(String fieldType, Object result) {
        super(String.format(
                "Exact GraphQL type for %s is unresolvable for an object of type %s",
                fieldType, result.getClass().getName()));
    }

    public UnresolvableTypeException(Object result) {
        super(String.format(
                "Exact GraphQL type is unresolvable for an object of type %s", result.getClass().getName()));
    }

    public UnresolvableTypeException(Object result, Exception cause) {
        super(String.format("Exception occurred during GraphQL type resolution for an object of type %s",
                result.getClass().getName()), cause);
    }
}

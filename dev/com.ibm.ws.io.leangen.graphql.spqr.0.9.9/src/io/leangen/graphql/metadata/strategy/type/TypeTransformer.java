package io.leangen.graphql.metadata.strategy.type;

import io.leangen.graphql.metadata.exceptions.TypeMappingException;

import java.lang.reflect.AnnotatedType;

@FunctionalInterface
public interface TypeTransformer {
    
    AnnotatedType transform(AnnotatedType annotatedType) throws TypeMappingException;
}

package io.leangen.graphql.metadata.strategy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;

public interface InclusionStrategy {

    boolean includeOperation(AnnotatedElement element, AnnotatedType type);

    boolean includeArgument(Parameter parameter, AnnotatedType type);

    boolean includeInputField(Class<?> declaringClass, AnnotatedElement element, AnnotatedType elementType);
}

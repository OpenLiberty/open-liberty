package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.AnnotatedType;

public interface ScalarDeserializationStrategy {

    boolean isDirectlyDeserializable(AnnotatedType type);
}

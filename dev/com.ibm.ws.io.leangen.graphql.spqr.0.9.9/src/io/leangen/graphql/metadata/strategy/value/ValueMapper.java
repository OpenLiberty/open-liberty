package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

public interface ValueMapper {

    String TYPE_METADATA_FIELD_NAME = "_type_";

    default <T> T fromInput(Object graphQlInput, AnnotatedType type) {
        return fromInput(graphQlInput, graphQlInput.getClass(), type);
    }
    
    <T> T fromInput(Object graphQLInput, Type sourceType, AnnotatedType outputType) throws InputParsingException;

    <T> T fromString(String json, AnnotatedType type) throws InputParsingException;
    
    String toString(Object output);
}

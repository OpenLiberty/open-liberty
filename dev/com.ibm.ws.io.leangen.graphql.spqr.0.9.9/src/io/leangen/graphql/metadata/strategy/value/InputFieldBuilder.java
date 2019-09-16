package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.metadata.InputField;

import java.lang.reflect.AnnotatedType;
import java.util.Set;

public interface InputFieldBuilder {
    
    Set<InputField> getInputFields(InputFieldBuilderParams params);

    boolean supports(AnnotatedType type);
}

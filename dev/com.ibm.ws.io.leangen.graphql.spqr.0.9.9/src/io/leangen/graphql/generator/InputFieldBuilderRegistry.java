package io.leangen.graphql.generator;

import io.leangen.graphql.metadata.InputField;
import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilder;
import io.leangen.graphql.metadata.strategy.value.InputFieldBuilderParams;
import io.leangen.graphql.util.ClassUtils;

import java.util.List;
import java.util.Set;

public class InputFieldBuilderRegistry {

    private final List<InputFieldBuilder> builders;

    public InputFieldBuilderRegistry(List<InputFieldBuilder> builders) {
        this.builders = builders;
    }

    public Set<InputField> getInputFields(InputFieldBuilderParams params) {
        return builders.stream()
                .filter(builder -> builder.supports(params.getType()))
                .findFirst()
                .map(builder -> builder.getInputFields(params))
                .orElseThrow(() -> new MappingException(String.format("No %s found for type %s",
                        InputFieldBuilder.class.getSimpleName(), ClassUtils.toString(params.getType()))));
    }
}

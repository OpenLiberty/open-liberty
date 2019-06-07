package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class VoidToBooleanTypeAdapter extends AbstractTypeAdapter<Void, Boolean> {

    @Override
    public Boolean convertOutput(Void original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        return true;
    }

    @Override
    public Void convertInput(Boolean substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        throw new UnsupportedOperationException("Void used as input");
    }
}

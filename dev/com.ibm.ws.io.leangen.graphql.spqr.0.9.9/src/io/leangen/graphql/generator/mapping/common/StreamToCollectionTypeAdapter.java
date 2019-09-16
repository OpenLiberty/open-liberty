package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.AbstractTypeAdapter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class StreamToCollectionTypeAdapter<T> extends AbstractTypeAdapter<Stream<T>, List<T>> {

    @Override
    public List<T> convertOutput(Stream<T> original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        try (Stream<T> stream = original) {
            return stream
                    .map(item -> resolutionEnvironment.<T, T>convertOutput(item, getElementType(type)))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Stream<T> convertInput(List<T> substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return substitute.stream().map(item -> environment.convertInput(item, getElementType(type), valueMapper));
    }

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), getElementType(original));
    }
    
    private AnnotatedType getElementType(AnnotatedType type) {
        return GenericTypeReflector.getTypeParameter(type, Stream.class.getTypeParameters()[0]);
    }
}

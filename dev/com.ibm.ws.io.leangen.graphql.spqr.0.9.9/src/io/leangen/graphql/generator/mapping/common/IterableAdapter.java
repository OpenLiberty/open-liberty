package io.leangen.graphql.generator.mapping.common;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.generator.mapping.InputConverter;
import io.leangen.graphql.metadata.strategy.value.ValueMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.List;

public class IterableAdapter<T> extends AbstractTypeSubstitutingMapper<List<T>> implements InputConverter<Iterable<T>, List<T>> {

    @Override
    public AnnotatedType getSubstituteType(AnnotatedType original) {
        return TypeFactory.parameterizedAnnotatedClass(List.class, original.getAnnotations(), getElementType(original));
    }

    private AnnotatedType getElementType(AnnotatedType type) {
        return GenericTypeReflector.getTypeParameter(type, Iterable.class.getTypeParameters()[0]);
    }

    @Override
    public Iterable<T> convertInput(List<T> substitute, AnnotatedType type, GlobalEnvironment environment, ValueMapper valueMapper) {
        return substitute; //List is already Iterable
    }

    @Override
    public boolean supports(AnnotatedType type) {
        /*
        Support only Iterable itself, not subtypes, because many unexpected types (e.g. Jackson's ObjectNode) are Iterable
        and catching those could cause mayhem.
        TODO Come up with a way to guard certain types from accidental input conversion.
        If a specific type (e.g. List) does *not* need conversion but a more broad type (e.g. Iterable) does,
        the specific type needs to be guarded.
        A sure-fire way would be to register dummy InputConverters in such cases, but seems heavy-handed.
        Another idea is to check ScalarDeserializationStrategy.isDirectlyDeserializable before invoking converters.
        This will not work in all cases (e.g. Collection will still be caught), but might be enough for realistic ones.
        For the remainder, special checks or guard converters can be added e.g.
        GenericTypeReflector.isSuperType(Iterable.class, type.getType()) && !GenericTypeReflector.isSuperType(Collection.class, type.getType());
        TODO Consider registering this adapter as an input converter for Gson only
        */
        return ClassUtils.getRawType(type.getType()).equals(Iterable.class);
    }
}

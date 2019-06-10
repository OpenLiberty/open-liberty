package io.leangen.graphql.generator.mapping;

import io.leangen.graphql.annotations.Ignore;
import io.leangen.graphql.metadata.exceptions.MappingException;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class TypeMapperRegistry {

    private final List<TypeMapper> typeMappers;

    public TypeMapperRegistry(List<TypeMapper> typeMappers) {
        this.typeMappers = Collections.unmodifiableList(typeMappers);
    }

    public TypeMapper getTypeMapper(AnnotatedType javaType, Set<Class<? extends TypeMapper>> mappersToSkip) {
        return getTypeMapper(javaType, typeMapper -> !mappersToSkip.contains(typeMapper.getClass()))
                .orElseThrow(() -> new MappingException(String.format("No %s found for type %s",
                        TypeMapper.class.getSimpleName(), ClassUtils.toString(javaType))));
    }

    private Optional<TypeMapper> getTypeMapper(AnnotatedType javaType, Predicate<TypeMapper> filter) {
        return typeMappers.stream()
                .filter(filter)
                .filter(typeMapper -> typeMapper.supports(javaType))
                .findFirst();
    }

    public AnnotatedType getMappableType(AnnotatedType type) {
        Optional<TypeMapper> mapper = this.getTypeMapper(type, typeMapper -> !typeMapper.getClass().isAnnotationPresent(Ignore.class));
        if (mapper.isPresent() && mapper.get() instanceof TypeSubstituter) {
            return getMappableType(((TypeSubstituter) mapper.get()).getSubstituteType(type));
        }
        return ClassUtils.transformType(type, this::getMappableType);
    }
}

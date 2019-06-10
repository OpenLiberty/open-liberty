package io.leangen.graphql.generator.mapping.common;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.OutputConverter;

/**
 * Only used to trigger the conversion on the components of collections, or keys/values of maps
 * @author Bojan Tomic (kaqqao)
 */
public class CollectionOutputConverter implements OutputConverter {

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (GenericTypeReflector.isSuperType(Collection.class, type.getType())) {
            return processCollection((Collection<?>) original, (AnnotatedParameterizedType) type, resolutionEnvironment);
        }
        return processMap((Map<?, ?>) original, (AnnotatedParameterizedType) type, resolutionEnvironment);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return GenericTypeReflector.isSuperType(Collection.class, type.getType())
                || GenericTypeReflector.isSuperType(Map.class, type.getType());
    }

    private List<?> processCollection(Collection<?> collection, AnnotatedParameterizedType type, ResolutionEnvironment resolutionEnvironment) {
        return collection.stream()
                .map(e -> resolutionEnvironment.convertOutput(e, type.getAnnotatedActualTypeArguments()[0]))
                .collect(Collectors.toList());
    }

    private Map<?, ?> processMap(Map<?, ?> map, AnnotatedParameterizedType type, ResolutionEnvironment resolutionEnvironment) {
        Map<?, ?> processed = new LinkedHashMap<>();
        map.forEach((k, v) -> processed.put(
                resolutionEnvironment.convertOutput(k, type.getAnnotatedActualTypeArguments()[0]),
                resolutionEnvironment.convertOutput(v, type.getAnnotatedActualTypeArguments()[1])));
        return processed;
    }
}

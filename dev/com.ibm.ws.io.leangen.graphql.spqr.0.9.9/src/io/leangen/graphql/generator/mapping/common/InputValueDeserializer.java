package io.leangen.graphql.generator.mapping.common;

import io.leangen.graphql.generator.mapping.ArgumentInjector;
import io.leangen.graphql.generator.mapping.ArgumentInjectorParams;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InputValueDeserializer implements ArgumentInjector {

    private static Map<Class<?>, Object> EMPTY_VALUES = emptyValues();

    @Override
    public Object getArgumentValue(ArgumentInjectorParams params) {
        if (params.getInput() == null) {
            if (params.isPresent()) {
                return EMPTY_VALUES.getOrDefault(ClassUtils.getRawType(params.getType().getType()), null);
            }
            return null;
        }
        return params.getResolutionEnvironment().valueMapper.fromInput(params.getInput(), params.getType());
    }

    private static Map<Class<?>, Object> emptyValues() {
        Map<Class<?>, Object> empty = new HashMap<>();
        empty.put(Optional.class, Optional.empty());
        empty.put(OptionalInt.class, OptionalInt.empty());
        empty.put(OptionalLong.class, OptionalLong.empty());
        empty.put(OptionalDouble.class, OptionalDouble.empty());
        return Collections.unmodifiableMap(empty);
    }

    @Override
    public boolean supports(AnnotatedType type, Parameter parameter) {
        return true;
    }
}

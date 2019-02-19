package io.leangen.graphql.metadata.strategy.value.jackson;

import com.fasterxml.jackson.databind.JavaType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

public class TypeUtils {

    static AnnotatedType toJavaType(JavaType jacksonType) {
        if (jacksonType.getRawClass().getTypeParameters().length > 0) {
            AnnotatedType[] paramTypes = jacksonType.getBindings().getTypeParameters().stream()
                    .map(TypeUtils::toJavaType)
                    .toArray(AnnotatedType[]::new);
            return TypeFactory.parameterizedAnnotatedClass(jacksonType.getRawClass(), jacksonType.getRawClass().getAnnotations(), paramTypes);
        }
        if (jacksonType.isArrayType()) {
            return TypeFactory.arrayOf(toJavaType(jacksonType.getContentType()), new Annotation[0]);
        }
        return GenericTypeReflector.annotate(jacksonType.getRawClass());
    }
}

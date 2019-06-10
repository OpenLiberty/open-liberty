package io.leangen.graphql.metadata.strategy.type;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.metadata.exceptions.TypeMappingException;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

public class DefaultTypeTransformer implements TypeTransformer {
    
    private final AnnotatedType rawReplacement;
    private final AnnotatedType unboundedReplacement;

    public DefaultTypeTransformer(boolean replaceRaw, boolean replaceUnbounded) {
        AnnotatedType replacement = GenericTypeReflector.annotate(Object.class);
        this.rawReplacement = replaceRaw ? replacement : null;
        this.unboundedReplacement = replaceUnbounded ? replacement : null;
    }

    public DefaultTypeTransformer(AnnotatedType rawReplacement, AnnotatedType unboundedReplacement) {
        this.rawReplacement = rawReplacement;
        this.unboundedReplacement = unboundedReplacement;
    }

    @Override
    public AnnotatedType transform(AnnotatedType annotatedType) throws TypeMappingException {
        annotatedType = ClassUtils.eraseBounds(annotatedType, unboundedReplacement);
        return ClassUtils.completeGenerics(annotatedType, rawReplacement);
    }
}

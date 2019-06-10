package io.leangen.graphql.generator.mapping;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.generator.mapping.common.AbstractTypeSubstitutingMapper;
import io.leangen.graphql.util.ClassUtils;

import java.lang.reflect.AnnotatedType;

/**
 * Created by bojan.tomic on 9/21/16.
 */
public abstract class AbstractTypeAdapter<T, S>
        extends AbstractTypeSubstitutingMapper<S>
        implements InputConverter<T, S>, OutputConverter<T, S> {

    protected final AnnotatedType sourceType;

    protected AbstractTypeAdapter() {
        this.sourceType = getSourceType();
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return ClassUtils.isAssignable(sourceType.getType(), type.getType());
    }

    private AnnotatedType getSourceType() {
        return GenericTypeReflector.getTypeParameter(getClass().getAnnotatedSuperclass(), AbstractTypeAdapter.class.getTypeParameters()[0]);
    }
}

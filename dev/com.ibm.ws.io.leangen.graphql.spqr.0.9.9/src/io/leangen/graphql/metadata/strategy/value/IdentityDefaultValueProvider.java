package io.leangen.graphql.metadata.strategy.value;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class IdentityDefaultValueProvider implements DefaultValueProvider {

    @Override
    public Object getDefaultValue(AnnotatedElement targetElement, AnnotatedType type, Object initialValue) {
        return initialValue;
    }
}

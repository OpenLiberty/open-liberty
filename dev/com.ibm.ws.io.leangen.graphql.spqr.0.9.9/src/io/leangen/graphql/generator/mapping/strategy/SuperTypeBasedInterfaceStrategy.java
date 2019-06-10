package io.leangen.graphql.generator.mapping.strategy;

import java.lang.reflect.AnnotatedType;
import java.util.Collection;

import io.leangen.graphql.util.ClassUtils;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class SuperTypeBasedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    private Collection<Class<?>> mappedTypes;

    public SuperTypeBasedInterfaceStrategy(Collection<Class<?>> mappedTypes, boolean mapClasses) {
        super(mapClasses);
        this.mappedTypes = mappedTypes;
    }

    @Override
    public boolean supportsInterface(AnnotatedType interfase) {
        Class<?> raw = ClassUtils.getRawType(interfase.getType());
        return mappedTypes.stream().anyMatch(type -> type.isAssignableFrom(raw));
    }
}

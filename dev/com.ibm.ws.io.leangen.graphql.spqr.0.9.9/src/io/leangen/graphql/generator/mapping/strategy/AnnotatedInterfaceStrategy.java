package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.annotations.types.Interface;

import java.lang.reflect.AnnotatedType;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class AnnotatedInterfaceStrategy extends AbstractInterfaceMappingStrategy {

    public AnnotatedInterfaceStrategy(boolean mapClasses) {
        super(mapClasses);
    }

    @Override
    public boolean supportsInterface(AnnotatedType interfase) {
        return interfase.isAnnotationPresent(Interface.class);
    }
}

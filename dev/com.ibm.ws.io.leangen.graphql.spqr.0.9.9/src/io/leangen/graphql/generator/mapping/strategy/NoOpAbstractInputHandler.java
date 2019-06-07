package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.generator.BuildContext;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NoOpAbstractInputHandler implements AbstractInputHandler {

    @Override
    public Set<Type> findConstituentAbstractTypes(AnnotatedType javaType, BuildContext buildContext) {
        return Collections.emptySet();
    }

    @Override
    public List<Class<?>> findConcreteSubTypes(Class abstractType, BuildContext buildContext) {
        return Collections.emptyList();
    }
}

package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.util.Scalars;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

public interface AbstractInputHandler {

    Set<Type> findConstituentAbstractTypes(AnnotatedType javaType, BuildContext buildContext);

    List<Class<?>> findConcreteSubTypes(Class abstractType, BuildContext buildContext);
    
    default void setScalars(Scalars scalars) {
        // default is no-op
    }
}

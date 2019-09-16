package io.leangen.graphql.generator.mapping.strategy;

import io.leangen.graphql.generator.BuildContext;

import java.lang.reflect.AnnotatedType;
import java.util.List;

public interface ImplementationDiscoveryStrategy {

    List<AnnotatedType> findImplementations(AnnotatedType type, String[] scanPackages, BuildContext buildContext);
}

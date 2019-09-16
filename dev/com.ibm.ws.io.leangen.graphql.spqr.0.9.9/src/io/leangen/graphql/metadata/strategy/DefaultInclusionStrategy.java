package io.leangen.graphql.metadata.strategy;

import io.leangen.graphql.annotations.Context;
import io.leangen.graphql.annotations.Ignore;
import io.leangen.graphql.annotations.Info;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Source;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Arrays;

public class DefaultInclusionStrategy implements InclusionStrategy {

    private final String[] basePackages;

    public DefaultInclusionStrategy(String... basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public boolean includeOperation(AnnotatedElement element, AnnotatedType type) {
        return !ClassUtils.hasAnnotation(element, Ignore.class);
    }

    @Override
    public boolean includeArgument(Parameter parameter, AnnotatedType type) {
        return !ClassUtils.hasAnnotation(parameter, Ignore.class)
                && !parameter.isAnnotationPresent(Source.class)
                && !parameter.isAnnotationPresent(Context.class)
                && !parameter.isAnnotationPresent(Info.class);
    }

    @Override
    public boolean includeInputField(Class<?> declaringClass, AnnotatedElement element, AnnotatedType elementType) {
        return !ClassUtils.hasAnnotation(element, Ignore.class)
                && (Utils.isArrayEmpty(basePackages)
                || Arrays.stream(basePackages).anyMatch(pkg -> ClassUtils.isSubPackage(declaringClass.getPackage(), pkg)));
    }
}

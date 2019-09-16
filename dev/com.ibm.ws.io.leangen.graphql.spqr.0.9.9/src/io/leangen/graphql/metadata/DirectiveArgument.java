package io.leangen.graphql.metadata;

import io.leangen.graphql.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.stream.Collectors;

public class DirectiveArgument {

    private final String name;
    private final String description;
    private final TypedElement typedElement;
    private final Object value;
    private final Object defaultValue;
    private final Annotation annotation;

    public DirectiveArgument(String name, String description, AnnotatedType javaType, Object value, Object defaultValue, AnnotatedElement element, Annotation annotation) {
        this.name = name;
        this.typedElement = new TypedElement(javaType, element);
        this.description = description;
        this.value = value;
        this.defaultValue = defaultValue;
        this.annotation = annotation;
    }

    public String getName() {
        return name;
    }

    public AnnotatedType getJavaType() {
        return typedElement.getJavaType();
    }

    public String getDescription() {
        return description;
    }

    public Object getValue() {
        return value;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public TypedElement getTypedElement() {
        return typedElement;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    @Override
    public String toString() {
        return String.format("Directive argument '%s' of type %s bound to [%s]", name, ClassUtils.toString(getJavaType()),
                typedElement.getElements().stream().map(ClassUtils::toString).collect(Collectors.joining()));
    }
}

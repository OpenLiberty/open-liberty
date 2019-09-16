package io.leangen.graphql.metadata;

import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.Objects;

public class InputField {

    private final String name;
    private final String description;
    private final TypedElement typedElement;
    private final AnnotatedType deserializableType;
    private final Object defaultValue;

    public InputField(String name, String description, AnnotatedType javaType, AnnotatedType deserializableType, Object defaultValue, AnnotatedElement element) {
        this.name = Utils.requireNonEmpty(name);
        this.description = description;
        this.typedElement = new TypedElement(Objects.requireNonNull(javaType), element);
        this.deserializableType = deserializableType != null ? deserializableType : javaType;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AnnotatedType getJavaType() {
        return typedElement.getJavaType();
    }

    public AnnotatedType getDeserializableType() {
        return deserializableType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public TypedElement getTypedElement() {
        return typedElement;
    }

    @Override
    public String toString() {
        return String.format("Input field '%s' of type %s bound to [%s]", name, ClassUtils.toString(getJavaType()), ClassUtils.toString(typedElement.getElement()));
    }
}

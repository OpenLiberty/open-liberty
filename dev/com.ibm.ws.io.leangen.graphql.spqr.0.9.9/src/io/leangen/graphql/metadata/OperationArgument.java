package io.leangen.graphql.metadata;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OperationArgument {

    private final TypedElement typedElement;
    private final AnnotatedType baseType;
    private final String name;
    private final String description;
    private final Object defaultValue;
    private final boolean context;
    private final boolean mappable;

    public OperationArgument(AnnotatedType javaType, String name, String description, Object defaultValue,
                             Parameter parameter, boolean context, boolean mappable) {
        this(javaType, name, description, defaultValue, Utils.singletonList(parameter), context, mappable);
    }

    public OperationArgument(AnnotatedType javaType, String name, String description, Object defaultValue,
                             List<Parameter> parameters, boolean context, boolean mappable) {

        this.typedElement = new TypedElement(Objects.requireNonNull(javaType), parameters);
        this.baseType = resolveBaseType(typedElement.getJavaType());
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.defaultValue = defaultValue;
        this.context = context;
        this.mappable = mappable;
    }

    public AnnotatedType getJavaType() {
        return typedElement.getJavaType();
    }

    public AnnotatedType getBaseType() {
        return baseType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public Parameter getParameter() {
        return (Parameter) typedElement.getElement();
    }

    public boolean isContext() {
        return context;
    }

    public TypedElement getTypedElement() {
        return typedElement;
    }

    public boolean isMappable() {
        return mappable;
    }

    private static AnnotatedType resolveBaseType(AnnotatedType type) {
        //Unwrap collection types
        AnnotatedType unwrappedCollectionType = GenericTypeReflector.isSuperType(Collection.class, type.getType())
                ? GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0])
                : type;
        // Lose all the TYPE_USE annotations
        return GenericTypeReflector.annotate(unwrappedCollectionType.getType());
    }

    @Override
    public String toString() {
        return String.format("Operation argument '%s' of type %s bound to [%s]", name, ClassUtils.toString(getJavaType()),
                typedElement.getElements().stream().map(ClassUtils::toString).collect(Collectors.joining()));
    }
}

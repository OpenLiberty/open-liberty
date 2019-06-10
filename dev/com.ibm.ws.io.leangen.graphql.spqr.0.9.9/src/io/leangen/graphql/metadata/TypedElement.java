package io.leangen.graphql.metadata;

import io.leangen.graphql.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;

public class TypedElement {

    private final AnnotatedType javaType;
    private final List<? extends AnnotatedElement> elements;

    TypedElement(AnnotatedType javaType, AnnotatedElement element) {
        this.javaType = javaType;
        this.elements = Utils.singletonList(element);
    }

    TypedElement(AnnotatedType javaType, List<? extends AnnotatedElement> elements) {
        this.javaType = javaType;
        this.elements = elements;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return elements.stream().anyMatch(element -> element.isAnnotationPresent(annotation));
    }

    public boolean isAnnotationPresentAnywhere(Class<? extends Annotation> annotation) {
        return javaType.isAnnotationPresent(annotation) || isAnnotationPresent(annotation);
    }

    public AnnotatedType getJavaType() {
        return javaType;
    }

    public List<? extends AnnotatedElement> getElements() {
        return elements;
    }

    public AnnotatedElement getElement() {
        if (elements.size() == 1) {
            return elements.get(0);
        }
        throw new IllegalStateException("Multiple mappable elements found when a single was expected");
    }
}

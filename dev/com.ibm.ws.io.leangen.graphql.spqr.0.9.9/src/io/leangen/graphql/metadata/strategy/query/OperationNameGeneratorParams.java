package io.leangen.graphql.metadata.strategy.query;

import io.leangen.graphql.metadata.messages.EmptyMessageBundle;
import io.leangen.graphql.metadata.messages.MessageBundle;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;

public class OperationNameGeneratorParams<T extends Member & AnnotatedElement> {

    private final T element;
    private final AnnotatedType declaringType;
    private final Object instance;
    private final MessageBundle messageBundle;

    OperationNameGeneratorParams(T element, AnnotatedType declaringType, Object instance, MessageBundle messageBundle) {
        this.element = Objects.requireNonNull(element);
        this.declaringType = Objects.requireNonNull(declaringType);
        this.instance = instance;
        this.messageBundle = messageBundle != null ? messageBundle : EmptyMessageBundle.INSTANCE;
    }

    public static Builder<Field> builderForField() {
        return new Builder<>();
    }

    public static Builder<Method> builderForMethod() {
        return new Builder<>();
    }

    public T getElement() {
        return element;
    }

    public AnnotatedType getDeclaringType() {
        return declaringType;
    }

    public Object getInstance() {
        return instance;
    }

    public MessageBundle getMessageBundle() {
        return messageBundle;
    }

    public boolean isMethod() {
        return element instanceof Method;
    }

    public static class Builder<T extends Member & AnnotatedElement> {
        private T element;
        private AnnotatedType declaringType;
        private Object instance;
        private MessageBundle messageBundle;

        public Builder<T> withElement(T element) {
            this.element = element;
            return this;
        }

        public Builder<T> withDeclaringType(AnnotatedType declaringType) {
            this.declaringType = declaringType;
            return this;
        }

        public Builder<T> withInstance(Object instance) {
            this.instance = instance;
            return this;
        }

        public Builder<T> withMessageBundle(MessageBundle messageBundle) {
            this.messageBundle = messageBundle;
            return this;
        }

        public OperationNameGeneratorParams<T> build() {
            return new OperationNameGeneratorParams<>(element, declaringType, instance, messageBundle);
        }
    }
}

package io.leangen.graphql.metadata.strategy.type;

import io.leangen.graphql.annotations.types.GraphQLDirective;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.InputType;
import org.eclipse.microprofile.graphql.Type;

import java.beans.Introspector;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class DefaultTypeInfoGenerator implements TypeInfoGenerator {

    @Override
    public String generateTypeName(AnnotatedType type, MessageBundle messageBundle) {
        if (type instanceof AnnotatedParameterizedType) {
            String baseName = generateSimpleName(type, messageBundle);
            StringBuilder genericName = new StringBuilder(baseName);
            Arrays.stream(((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments())
                    .map(t -> generateSimpleName(t, messageBundle))
                    .forEach(argName -> genericName.append("_").append(argName));
            return genericName.toString();
        }
        return generateSimpleName(type, messageBundle);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
        return Optional.ofNullable(type.getAnnotation(Type.class))
                .map(ann -> messageBundle.interpolate(ann.description()))
                .orElse("");
    }

    @Override
    public String generateInputTypeName(AnnotatedType type, MessageBundle messageBundle) {
        return Optional.ofNullable(type.getAnnotation(InputType.class))
                .map(ann -> messageBundle.interpolate(ann.value()))
                .filter(Utils::isNotEmpty)
                .orElse(TypeInfoGenerator.super.generateInputTypeName(type, messageBundle));
    }

    @Override
    public String generateInputTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
        return Optional.ofNullable(type.getAnnotation(InputType.class))
                .map(ann -> messageBundle.interpolate(ann.description()))
                .orElse(generateTypeDescription(type, messageBundle));
    }

    @Override
    public String[] getFieldOrder(AnnotatedType type, MessageBundle messageBundle) {
        return Utils.emptyArray();
    }

    @Override
    public String generateDirectiveTypeName(AnnotatedType type, MessageBundle messageBundle) {
        return messageBundle.interpolate(
                Optional.ofNullable(type.getAnnotation(GraphQLDirective.class))
                        .map(GraphQLDirective::name)
                        .filter(Utils::isNotEmpty)
                        .orElse(Introspector.decapitalize(ClassUtils.getRawType(type.getType()).getSimpleName())));
    }

    @Override
    public String generateDirectiveTypeDescription(AnnotatedType type, MessageBundle messageBundle) {
        return messageBundle.interpolate(
                Optional.ofNullable(type.getAnnotation(GraphQLDirective.class))
                        .map(GraphQLDirective::description)
                        .orElse(""));
    }

    @SuppressWarnings("unchecked")
    private String generateSimpleName(AnnotatedType type, MessageBundle messageBundle) {
        Optional<String> name = Optional.ofNullable(type.getAnnotation(Type.class))
                .map(Type::value)
                .filter(Utils::isNotEmpty);
        return messageBundle.interpolate(name.orElseGet(() -> getSimpleName(ClassUtils.getRawType(type.getType()))));
    }

    private String getFirstNonEmptyOrDefault(Optional<String>[] optionals, Supplier<String> defaultValue) {
        return Arrays.stream(optionals)
                .map(opt -> opt.filter(Utils::isNotEmpty))
                .reduce(Utils::or)
                .map(opt -> opt.orElse(defaultValue.get()))
                .get();
    }

    private String getSimpleName(Class<?> clazz) {
        if (clazz.isArray()) {
            return getSimpleName(clazz.getComponentType()) + "Array";
        }
        return clazz.getSimpleName();
    }
}

package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.execution.GlobalEnvironment;
import io.leangen.graphql.metadata.messages.MessageBundle;
import io.leangen.graphql.util.ReservedStrings;
import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.InputField;
import org.eclipse.microprofile.graphql.Query;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Optional;

public class InputFieldInfoGenerator {

    public Optional<String> getName(List<AnnotatedElement> candidates, MessageBundle messageBundle) {
        Optional<String> explicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(InputField.class))
                .findFirst()
                .map(element -> element.getAnnotation(InputField.class).value());
        Optional<String> implicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(Query.class))
                .findFirst()
                .map(element -> element.getAnnotation(Query.class).value());
        return Utils.or(explicit, implicit).filter(Utils::isNotEmpty).map(messageBundle::interpolate);
    }

    public Optional<String> getDescription(List<AnnotatedElement> candidates, MessageBundle messageBundle) {
        Optional<String> explicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(InputField.class))
                .findFirst()
                .map(element -> element.getAnnotation(InputField.class).description());
        Optional<String> implicit = candidates.stream()
                .filter(element -> element.isAnnotationPresent(Query.class))
                .findFirst()
                .map(element -> element.getAnnotation(Query.class).description());
        return Utils.or(explicit, implicit).filter(Utils::isNotEmpty).map(messageBundle::interpolate);
    }

    public Optional<Object> defaultValue(List<AnnotatedElement> candidates, AnnotatedType type, DefaultValueProvider defaultValueProvider, GlobalEnvironment environment) {
        return candidates.stream()
                .filter(element -> element.isAnnotationPresent(DefaultValue.class))
                .findFirst()
                .map(element -> defaultValueProvider.getDefaultValue(element, type, environment.messageBundle.interpolate(ReservedStrings.decode(element.getAnnotation(DefaultValue.class).value()))));
    }
}

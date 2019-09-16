package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.InputField;

import java.lang.reflect.Method;

public class AnnotationMappingUtils {

    public static String inputFieldName(Method method) {
        if (method.isAnnotationPresent(InputField.class)) {
            return Utils.coalesce(method.getAnnotation(InputField.class).value(), method.getName());
        }
        return method.getName();
    }

    public static String inputFieldDescription(Method method) {
        return method.isAnnotationPresent(InputField.class) ? method.getAnnotation(InputField.class).description() : "";
    }
}

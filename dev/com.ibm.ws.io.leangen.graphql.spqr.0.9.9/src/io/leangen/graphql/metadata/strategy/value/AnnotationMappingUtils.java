package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;

import java.lang.reflect.Method;

public class AnnotationMappingUtils {

    public static String inputFieldName(Method method) {
        if (method.isAnnotationPresent(Name.class)) {
            return Utils.coalesce(method.getAnnotation(Name.class).value(), method.getName());
        }
        return method.getName();
    }

    public static String inputFieldDescription(Method method) {
        return method.isAnnotationPresent(Description.class) ? method.getAnnotation(Description.class).value() : "";
    }
}

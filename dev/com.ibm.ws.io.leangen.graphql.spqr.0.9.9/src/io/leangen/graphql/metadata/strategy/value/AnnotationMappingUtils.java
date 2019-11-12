package io.leangen.graphql.metadata.strategy.value;

import io.leangen.graphql.util.Utils;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.SchemaName;

import java.lang.reflect.Method;

public class AnnotationMappingUtils {

    public static String inputFieldName(Method method) {
        if (method.isAnnotationPresent(SchemaName.class)) {
            return Utils.coalesce(method.getAnnotation(SchemaName.class).value(), method.getName());
        }
        return method.getName();
    }

    public static String inputFieldDescription(Method method) {
        return method.isAnnotationPresent(Description.class) ? method.getAnnotation(Description.class).value() : "";
    }
}

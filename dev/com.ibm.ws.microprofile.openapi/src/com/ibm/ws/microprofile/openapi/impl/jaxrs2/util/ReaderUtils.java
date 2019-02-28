package com.ibm.ws.microprofile.openapi.impl.jaxrs2.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.HttpMethod;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.ibm.ws.microprofile.openapi.impl.core.util.ParameterProcessor;
import com.ibm.ws.microprofile.openapi.impl.core.util.ReflectionUtils;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.OpenAPIExtension;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.ext.OpenAPIExtensions;

public class ReaderUtils {
    private static final String GET_METHOD = "get";
    private static final String POST_METHOD = "post";
    private static final String PUT_METHOD = "put";
    private static final String DELETE_METHOD = "delete";
    private static final String HEAD_METHOD = "head";
    private static final String OPTIONS_METHOD = "options";
    private static final String PATH_DELIMITER = "/";

    /**
     * Collects constructor-level parameters from class.
     *
     * @param cls is a class for collecting
     * @param components
     * @return the collection of supported parameters
     */
    public static List<Parameter> collectConstructorParameters(Class<?> cls, Components components, javax.ws.rs.Consumes classConsumes) {
        if (cls.isLocalClass() || (cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers()))) {
            return Collections.emptyList();
        }

        List<Parameter> selected = Collections.emptyList();
        int maxParamsCount = 0;

        for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
            if (!ReflectionUtils.isConstructorCompatible(constructor)
                && !ReflectionUtils.isInject(Arrays.asList(constructor.getDeclaredAnnotations()))) {
                continue;
            }

            final Type[] genericParameterTypes = constructor.getGenericParameterTypes();
            final Annotation[][] annotations = constructor.getParameterAnnotations();

            int paramsCount = 0;
            final List<Parameter> parameters = new ArrayList<Parameter>();
            for (int i = 0; i < genericParameterTypes.length; i++) {
                final List<Annotation> tmpAnnotations = Arrays.asList(annotations[i]);
                if (isContext(tmpAnnotations)) {
                    paramsCount++;
                } else {
                    final Type genericParameterType = genericParameterTypes[i];
                    final List<Parameter> tmpParameters = collectParameters(genericParameterType, tmpAnnotations, components, classConsumes);
                    if (tmpParameters.size() >= 1) {
                        for (Parameter tmpParameter : tmpParameters) {
                            if (ParameterProcessor.applyAnnotations(
                                                                    tmpParameter,
                                                                    genericParameterType,
                                                                    tmpAnnotations,
                                                                    components,
                                                                    classConsumes == null ? new String[0] : classConsumes.value(),
                                                                    null) != null) {
                                parameters.add(tmpParameter);
                            }
                        }
                        paramsCount++;
                    }
                }
            }

            if (paramsCount >= maxParamsCount) {
                maxParamsCount = paramsCount;
                selected = parameters;
            }
        }

        return selected;
    }

    /**
     * Collects field-level parameters from class.
     *
     * @param cls is a class for collecting
     * @param components
     * @return the collection of supported parameters
     */
    public static List<Parameter> collectFieldParameters(Class<?> cls, Components components, javax.ws.rs.Consumes classConsumes) {
        final List<Parameter> parameters = new ArrayList<Parameter>();
        for (Field field : ReflectionUtils.getDeclaredFields(cls)) {
            final List<Annotation> annotations = Arrays.asList(field.getAnnotations());
            final Type genericType = field.getGenericType();
            parameters.addAll(collectParameters(genericType, annotations, components, classConsumes));
        }
        return parameters;
    }

    private static List<Parameter> collectParameters(Type type, List<Annotation> annotations, Components components, javax.ws.rs.Consumes classConsumes) {
        final Iterator<OpenAPIExtension> chain = OpenAPIExtensions.chain();
        return chain.hasNext() ? chain.next().extractParameters(annotations, type, new HashSet<>(), components, classConsumes, null, false,
                                                                chain).parameters : Collections.emptyList();
    }

    private static boolean isContext(List<Annotation> annotations) {
//        for (Annotation annotation : annotations) {
//            if (annotation instanceof Context) {
//                return true;
//            }
//        }
        return false;
    }

    /**
     * Splits the provided array of strings into an array, using comma as the separator.
     * Also removes leading and trailing whitespace and omits empty strings from the results.
     *
     * @param strings is the provided array of strings
     * @return the resulted array of strings
     */
    public static String[] splitContentValues(String[] strings) {
        final Set<String> result = new LinkedHashSet<String>();

        for (String string : strings) {
            if (string.isEmpty())
                continue;
            String[] splitted = string.trim().split(",");
            for (String string2 : splitted) {
                result.add(string2);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static Optional<List<String>> getStringListFromStringArray(String[] array) {
        if (array == null) {
            return Optional.empty();
        }
        List<String> list = new ArrayList<>();
        boolean isEmpty = true;
        for (String value : array) {
            if (StringUtils.isNotBlank(value)) {
                isEmpty = false;
            }
            list.add(value);
        }
        if (isEmpty) {
            return Optional.empty();
        }
        return Optional.of(list);
    }

    public static String getPath(javax.ws.rs.Path classLevelPath, javax.ws.rs.Path methodLevelPath, String parentPath) {
        if (classLevelPath == null && methodLevelPath == null && StringUtils.isEmpty(parentPath)) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        if (parentPath != null && !"".equals(parentPath) && !"/".equals(parentPath)) {
            if (!parentPath.startsWith("/")) {
                parentPath = "/" + parentPath;
            }
            if (parentPath.endsWith("/")) {
                parentPath = parentPath.substring(0, parentPath.length() - 1);
            }

            b.append(parentPath);
        }
        if (classLevelPath != null && !"/".equals(classLevelPath.value())) {
            String classPath = classLevelPath.value();
            if (!classPath.startsWith("/") && !b.toString().endsWith("/")) {
                b.append("/");
            }
            if (classPath.endsWith("/")) {
                classPath = classPath.substring(0, classPath.length() - 1);
            }
            b.append(classPath);
        }
        if (methodLevelPath != null && !"/".equals(methodLevelPath.value())) {
            String methodPath = methodLevelPath.value();
            if (!methodPath.startsWith("/") && !b.toString().endsWith("/")) {
                b.append("/");
            }
            if (methodPath.endsWith("/")) {
                methodPath = methodPath.substring(0, methodPath.length() - 1);
            }
            b.append(methodPath);
        }
        String output = b.toString();
        if (!output.startsWith("/")) {
            output = "/" + output;
        }
        if (output.endsWith("/") && output.length() > 1) {
            return output.substring(0, output.length() - 1);
        } else {
            return output;
        }
    }

    public static String extractOperationMethod(Operation operation, Method method, Iterator<OpenAPIExtension> chain) {
        if (method.getAnnotation(javax.ws.rs.GET.class) != null) {
            return GET_METHOD;
        } else if (method.getAnnotation(javax.ws.rs.PUT.class) != null) {
            return PUT_METHOD;
        } else if (method.getAnnotation(javax.ws.rs.POST.class) != null) {
            return POST_METHOD;
        } else if (method.getAnnotation(javax.ws.rs.DELETE.class) != null) {
            return DELETE_METHOD;
        } else if (method.getAnnotation(javax.ws.rs.OPTIONS.class) != null) {
            return OPTIONS_METHOD;
        } else if (method.getAnnotation(javax.ws.rs.HEAD.class) != null) {
            return HEAD_METHOD;
        } else if (method.getAnnotation(DELETE.class) != null) {
            return DELETE_METHOD;
        } else if (method.getAnnotation(HttpMethod.class) != null) {
            HttpMethod httpMethod = method.getAnnotation(HttpMethod.class);
            return httpMethod.value().toLowerCase();
        } else if (!StringUtils.isEmpty(getHttpMethodFromCustomAnnotations(method))) {
            return getHttpMethodFromCustomAnnotations(method);
        } else if ((ReflectionUtils.getOverriddenMethod(method)) != null) {
            return extractOperationMethod(operation, ReflectionUtils.getOverriddenMethod(method), chain);
        } else if (chain != null && chain.hasNext()) {
            return chain.next().extractOperationMethod(operation, method, chain);
        } else {
            return null;
        }
    }

    public static String getHttpMethodFromCustomAnnotations(Method method) {
        for (Annotation methodAnnotation : method.getAnnotations()) {
            HttpMethod httpMethod = methodAnnotation.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                return httpMethod.value().toLowerCase();
            }
        }
        return null;
    }

    public static void copyParamValues(Parameter to, Parameter from) {
        if (from.getIn() != null) {
            to.setIn(from.getIn());
        }
        if (from.getAllowEmptyValue() != null) {
            to.setAllowEmptyValue(from.getAllowEmptyValue());
        }
        if (from.getAllowReserved() != null) {
            to.setAllowReserved(from.getAllowReserved());
        }
        if (from.getDeprecated() != null) {
            to.setDeprecated(from.getDeprecated());
        }
        if (from.getDescription() != null) {
            to.setDescription(from.getDescription());
        }
        if (from.getStyle() != null) {
            to.setStyle(from.getStyle());
        }
        if (to.getSchema() == null) {
            to.setSchema(from.getSchema());
        }
        if (to.getContent() == null) {
            to.setContent(from.getContent());
        }
        if (from.getExample() != null) {
            to.setExample(from.getExample());
        }
        if (from.getRequired() != null) {
            to.setRequired(from.getRequired());
        }
        if (from.getExplode() != null) {
            to.setExplode(from.getExplode());
        }
        if (from.getExamples() != null) {
            to.setExamples(from.getExamples());
        }

    }
}

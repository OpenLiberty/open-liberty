/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.ProxyClassLoader;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.ProtocolHeaders;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalContextResolver;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpHeaders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalInvocationHandler;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalMessageContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProtocolHeaders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProviders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalSecurityContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.injection.InjectionRuntimeContextHelper;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.MethodInfo;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;

public final class InjectionUtils {
    private static final TraceComponent tc = Tr.register(InjectionUtils.class);

    public static final Set<String> STANDARD_CONTEXT_CLASSES = new HashSet<>();
    public static final Set<String> VALUE_CONTEXTS = new HashSet<>();
    static {
        // JAX-RS 1.0-1.1
        STANDARD_CONTEXT_CLASSES.add(Application.class.getName());
        STANDARD_CONTEXT_CLASSES.add(UriInfo.class.getName());
        STANDARD_CONTEXT_CLASSES.add(HttpHeaders.class.getName());
        STANDARD_CONTEXT_CLASSES.add(Request.class.getName());
        STANDARD_CONTEXT_CLASSES.add(SecurityContext.class.getName());
        STANDARD_CONTEXT_CLASSES.add(Providers.class.getName());
        STANDARD_CONTEXT_CLASSES.add(ContextResolver.class.getName());
        STANDARD_CONTEXT_CLASSES.add("javax.servlet.http.HttpServletRequest");
        STANDARD_CONTEXT_CLASSES.add("javax.servlet.http.HttpServletResponse");
        STANDARD_CONTEXT_CLASSES.add("javax.servlet.ServletContext");
        // JAX-RS 2.0
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.container.ResourceContext");
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.container.ResourceInfo");
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.core.Configuration");
        // JAX-RS 2.1
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.sse.Sse");
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.sse.SseEventSink");

        VALUE_CONTEXTS.add(Application.class.getName());
        VALUE_CONTEXTS.add("javax.ws.rs.sse.Sse");
    }
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(InjectionUtils.class);

    private static final String SERVLET_CONFIG_CLASS_NAME = "javax.servlet.ServletConfig";
    private static final String SERVLET_CONTEXT_CLASS_NAME = "javax.servlet.ServletContext";
    private static final String HTTP_SERVLET_REQUEST_CLASS_NAME = "javax.servlet.http.HttpServletRequest";
    private static final String HTTP_SERVLET_RESPONSE_CLASS_NAME = "javax.servlet.http.HttpServletResponse";
    private static final String ENUM_CONVERSION_CASE_SENSITIVE = "enum.conversion.case.sensitive";

    private static final String IGNORE_MATRIX_PARAMETERS = "ignore.matrix.parameters";

//    private static List<String> JAXRS_JEE_COMPONENTS = Collections.<String> emptyList();

    private static List<String> explicitLifcycle = new ArrayList<String>();
    static {
        explicitLifcycle.add("javax.enterprise.context.RequestScoped");
        explicitLifcycle.add("javax.enterprise.context.ApplicationScoped");
        explicitLifcycle.add("javax.enterprise.context.SessionScoped");
        explicitLifcycle.add("javax.enterprise.context.Dependent");
    }

    private InjectionUtils() {

    }

    public static Field getDeclaredField(Class<?> cls, String fieldName) {
        if (cls == null || cls == Object.class) {
            return null;
        }
        Field f = ReflectionUtil.getDeclaredField(cls, fieldName);
        if (f != null) {
            return f;
        }
        return getDeclaredField(cls.getSuperclass(), fieldName);
    }

    public static boolean isConcreteClass(Class<?> cls) {
        return !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers());
    }

    private static ParameterizedType findGenericDeclaration(GenericDeclaration declaration, Type scope) {
        if (scope instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) scope;
            if (type.getRawType() == declaration) {
                return type;
            }
            scope = type.getRawType();
        }
        if (scope instanceof Class) {
            Class<?> classScope = (Class<?>) scope;
            ParameterizedType result = findGenericDeclaration(declaration, classScope.getGenericSuperclass());
            if (result == null) {
                for (Type type : classScope.getGenericInterfaces()) {
                    result = findGenericDeclaration(declaration, type);
                    if (result != null) {
                        break;
                    }
                }
            }
            return result;
        }
        return null;
    }

    public static Type getSuperType(Class<?> serviceClass, TypeVariable<?> var) {

        int pos = 0;
        GenericDeclaration genericDeclaration = var.getGenericDeclaration();
        TypeVariable<?>[] vars = genericDeclaration.getTypeParameters();
        for (; pos < vars.length; pos++) {
            if (vars[pos].getName().equals(var.getName())) {
                break;
            }
        }

        ParameterizedType genericSubtype = findGenericDeclaration(genericDeclaration, serviceClass);
        Type result = null;
        if (genericSubtype != null) {
            result = genericSubtype.getActualTypeArguments()[pos];
        }
        if (result instanceof TypeVariable) {
            result = getSuperType(serviceClass, (TypeVariable<?>) result);
        }

        if (result == null || result == Object.class) {
            for (Type bound : var.getBounds()) {
                if (bound != Object.class) {
                    result = bound;
                    break;
                }
            }
        }
        return result;
    }

    public static Method checkProxy(Method methodToInvoke, Object resourceObject) {
        if (Proxy.class.isInstance(resourceObject)) {
            String methodToInvokeName = methodToInvoke.getName();
            Class<?>[] methodToInvokeTypes = methodToInvoke.getParameterTypes();

            for (Class<?> c : resourceObject.getClass().getInterfaces()) {
                try {
                    return c.getMethod(methodToInvokeName, methodToInvokeTypes);
                } catch (NoSuchMethodException ex) {
                    //ignore
                }
                if (methodToInvokeTypes.length > 0) {
                    for (Method m : c.getMethods()) {
                        if (m.getName().equals(methodToInvokeName)
                            && m.getParameterTypes().length == methodToInvokeTypes.length) {
                            Class<?>[] methodTypes = m.getParameterTypes();
                            for (int i = 0; i < methodTypes.length; i++) {
                                if (!methodTypes[i].isAssignableFrom(methodToInvokeTypes[i])) {
                                    break;
                                }
                            }
                            return m;
                        }

                    }
                }

            }
        }
        return methodToInvoke;

    }

    public static void injectFieldValue(final Field f,
                                        final Object o,
                                        final Object v) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    f.setAccessible(true);
                    f.set(o, v);
                } catch (IllegalAccessException ex) {
                    reportServerError("FIELD_ACCESS_FAILURE",
                                      f.getType().getName());
                }
                return null;
            }
        });
    }

    public static Object extractFieldValue(final Field f,
                                           final Object o) {
        return AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    f.setAccessible(true);
                    return f.get(o);
                } catch (IllegalAccessException ex) {
                    reportServerError("FIELD_ACCESS_FAILURE",
                                      f.getType().getName());
                }
                return null;
            }
        });
    }

    public static Class<?> getActualType(Type genericType) {

        return getActualType(genericType, 0);
    }

    public static Class<?> getActualType(Type genericType, int pos) {

        if (genericType == null) {
            return null;
        }
        if (genericType == Object.class) {
            return (Class<?>) genericType;
        }
        if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            if (genericType instanceof TypeVariable) {
                genericType = getType(((TypeVariable<?>) genericType).getBounds(), pos);
            } else if (genericType instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) genericType;
                Type[] bounds = wildcardType.getLowerBounds();
                if (bounds.length == 0) {
                    bounds = wildcardType.getUpperBounds();
                }
                genericType = getType(bounds, pos);
            } else if (genericType instanceof GenericArrayType) {
                genericType = ((GenericArrayType) genericType).getGenericComponentType();
            }
            Class<?> cls = null;
            if (!(genericType instanceof ParameterizedType)) {
                cls = (Class<?>) genericType;
            } else {
                cls = (Class<?>) ((ParameterizedType) genericType).getRawType();
            }
            return cls.isArray() ? cls.getComponentType() : cls;

        }
        ParameterizedType paramType = (ParameterizedType) genericType;
        Type t = getType(paramType.getActualTypeArguments(), pos);
        //return t instanceof Class ? (Class<?>) t : getActualType(t, 0);//Liberty change
        return getClassType(t); //Liberty change
    }

    /**
     * Get the class type of the provided type. If the type is a Class, then
     * type is returned. If the type is ParameterizedType, then the Raw type is
     * returned.
     * <p>
     * E.g. if type is <code>String.class</code>, then <code>String.class</code>
     * is returned. If type is <code>List&lt;String&gt;</code>, then
     * <code>List.class</code> is returned.
     *
     * @param type the type to return the class type for
     * @return the class type of type
     */
    //Liberty change
    public static Class<?> getClassType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return (Class<?>) parameterizedType.getRawType();
        }

        if (type instanceof GenericArrayType) {
            GenericArrayType genericArray = (GenericArrayType) type;
            Class<?> classType = getClassType(genericArray.getGenericComponentType());
            return Array.newInstance(classType, 0).getClass();
        }

        if (type instanceof TypeVariable<?>) {
            return getClassType(((TypeVariable<?>) type).getBounds()[0]);
        }

        if (type instanceof WildcardType) {
            return getClassType(((WildcardType) type).getUpperBounds()[0]);
        }

        return null;
    }

    public static Type getType(Type[] types, int pos) {
        if (pos >= types.length) {
            throw new RuntimeException("No type can be found at position " + pos);
        }
        return types[pos];
    }

    public static Class<?> getRawType(Type genericType) {

        if (genericType instanceof Class) {
            return (Class<?>) genericType;
        } else if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type t = paramType.getRawType();
            if (t instanceof Class) {
                return (Class<?>) t;
            }
        } else if (genericType instanceof GenericArrayType) {
            return getRawType(((GenericArrayType) genericType).getGenericComponentType());
        }
        return null;
    }

    public static Type[] getActualTypes(Type genericType) {
        if (genericType == null
            || !ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            return null;
        }
        ParameterizedType paramType = (ParameterizedType) genericType;
        return paramType.getActualTypeArguments();
    }

    public static void injectThroughMethod(Object requestObject,
                                           Method method,
                                           Object parameterValue) {
        injectThroughMethod(requestObject, method, parameterValue, null);
    }

    public static void injectThroughMethod(Object requestObject,
                                           Method method,
                                           Object parameterValue,
                                           Message inMessage) {
        try {
            Method methodToInvoke = checkProxy(method, requestObject);
            methodToInvoke.invoke(requestObject, new Object[] { parameterValue });
        } catch (IllegalAccessException ex) {
            reportServerError("METHOD_ACCESS_FAILURE", method.getName());
        } catch (InvocationTargetException ex) {
            Response r = JAXRSUtils.convertFaultToResponse(ex.getCause(), inMessage);
            if (r != null) {
                inMessage.getExchange().put(Response.class, r);
                throw new WebApplicationException();
            }
            reportServerError("METHOD_ACCESS_FAILURE", method.getName());
        } catch (Exception ex) {
            reportServerError("METHOD_INJECTION_FAILURE", method.getName());
        }
    }

    public static Object extractFromMethod(Object requestObject, Method method) {
        return extractFromMethod(requestObject, method, true);
    }

    public static Object extractFromMethod(Object requestObject,
                                           Method method,
                                           boolean logError) {
        try {
            Method methodToInvoke = checkProxy(method, requestObject);
            return methodToInvoke.invoke(requestObject);
        } catch (IllegalAccessException ex) {
            reportServerError("METHOD_ACCESS_FAILURE", method.getName(), logError);
        } catch (Exception ex) {
            reportServerError("METHOD_INJECTION_FAILURE", method.getName(), logError);
        }
        return null;
    }

    @FFDCIgnore(value = { NoSuchMethodException.class, NumberFormatException.class, Exception.class }) // Liberty Change
    @SuppressWarnings("unchecked")
    public static <T> T handleParameter(String value,
                                        boolean decoded,
                                        Class<T> pClass,
                                        Type genericType,
                                        Annotation[] paramAnns,
                                        ParameterType pType,
                                        Message message) {
        if (value == null) {
            return null;
        }

        //fix new Date("") throw exception defect
        if (value.isEmpty() && genericType == Date.class) {
            return null;
        }

        if (pType == ParameterType.PATH) {
            if (PathSegment.class.isAssignableFrom(pClass)) {
                return pClass.cast(new PathSegmentImpl(value, decoded));
            } else if (!MessageUtils.getContextualBoolean(message, IGNORE_MATRIX_PARAMETERS)) {
                value = new PathSegmentImpl(value, false).getPath();
            }
        }

        value = decodeValue(value, decoded, pType);

        Object result = null;
        try {
            result = createFromParameterHandler(value, pClass, genericType, paramAnns, message);
        } catch (IllegalArgumentException nfe) {
            throw createParamConversionException(pType, nfe);
        }
        if (result != null) {
            T theResult = null;
            if (pClass.isPrimitive()) {
                theResult = (T) result;
            } else {
                theResult = pClass.cast(result);
            }
            return theResult;
        }
        if (Number.class.isAssignableFrom(pClass) && "".equals(value)) {
            //pass empty string to boxed number type will result in 404
            return null;
        }
        if (Boolean.class == pClass) {
            // allow == checks for Boolean object
            pClass = (Class<T>) Boolean.TYPE;
        }
        if (pClass.isPrimitive()) {
            try {
                @SuppressWarnings("unchecked")
                T ret = (T) PrimitiveUtils.read(value, pClass);
                // cannot us pClass.cast as the pClass is something like
                // Boolean.TYPE (representing the boolean primitive) and
                // the object is a Boolean object
                return ret;
            } catch (NumberFormatException nfe) {
                throw createParamConversionException(pType, nfe);
            }
        }

        boolean adapterHasToBeUsed = false;
        Class<?> cls = pClass;
        Class<?> valueType = JAXBUtils.getValueTypeFromAdapter(pClass, pClass, paramAnns);
        if (valueType != cls) {
            cls = valueType;
            adapterHasToBeUsed = true;
        }
        if (pClass == String.class && !adapterHasToBeUsed) {
            return pClass.cast(value);
        }
        // check constructors accepting a single String value
        try {
            Constructor<?> c = cls.getConstructor(new Class<?>[] { String.class });
            result = c.newInstance(new Object[] { value });
        } catch (NoSuchMethodException ex) {
            // try valueOf
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable t = getOrThrowActualException(ex);
            Tr.error(tc, new org.apache.cxf.common.i18n.Message("CLASS_CONSTRUCTOR_FAILURE",
                            BUNDLE,
                            pClass.getName()).toString());
            Response r = JAXRSUtils.toResponse(HttpUtils.getParameterFailureStatus(pType));
            throw ExceptionUtils.toHttpException(t, r);
        }
        if (result == null) {
            // check for valueOf(String) static methods
            String[] methodNames = cls.isEnum()
                            ? new String[] { "fromString", "fromValue", "valueOf" }
                            : new String[] { "valueOf", "fromString" };
            result = evaluateFactoryMethods(value, pType, result, cls, methodNames);
        }

        if (adapterHasToBeUsed) {
            // as the last resort, try XmlJavaTypeAdapters
            Object valueToReplace = result != null ? result : value;
            try {
                result = JAXBUtils.convertWithAdapter(valueToReplace, pClass, paramAnns);
            } catch (Throwable ex) {
                result = null;
            }
        }

        if (result == null) {
            reportServerError("WRONG_PARAMETER_TYPE", pClass.getName());
        }

        try {
            return pClass.cast(result);
        } catch (ClassCastException ex) {
            reportServerError("WRONG_PARAMETER_TYPE", pClass.getName());
            return null;
        }
    }

    private static RuntimeException createParamConversionException(ParameterType pType, Exception ex) {
        //
        //  For path, query & matrix parameters this is 404,
        //  for others 400...
        //
        if (pType == ParameterType.PATH || pType == ParameterType.QUERY
            || pType == ParameterType.MATRIX) {
            return ExceptionUtils.toNotFoundException(ex, null);
        }
        return ExceptionUtils.toBadRequestException(ex, null);
    }

    public static <T> T createFromParameterHandler(String value,
                                                   Class<T> pClass,
                                                   Type genericType,
                                                   Annotation[] anns,
                                                   Message message) {
        T result = null;
        if (message != null) {
            ServerProviderFactory pf = ServerProviderFactory.getInstance(message);
            ParamConverter<T> pm = pf.createParameterHandler(pClass, genericType, anns, message);
            if (pm != null) {
                result = pm.fromString(value);
            }
        }
        return result;
    }

    public static void reportServerError(String messageName, String parameter) {
        reportServerError(messageName, parameter, true);
    }

    public static void reportServerError(String messageName, String parameter, boolean logError) {
        org.apache.cxf.common.i18n.Message errorMessage =
                        new org.apache.cxf.common.i18n.Message(messageName,
                                        BUNDLE,
                                        parameter);
        if (logError) {
            Tr.error(tc, errorMessage.toString());
        }
        Response r = JAXRSUtils.toResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR)
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .entity(errorMessage.toString()).build();
        throw ExceptionUtils.toInternalServerErrorException(null, r);
    }

    @FFDCIgnore(value = { Exception.class }) // Liberty Change
    private static Object evaluateFactoryMethods(String value, ParameterType pType, Object result,
                                                 Class<?> cls, String[] methodNames) {
        Exception factoryMethodEx = null;
        for (String mName : methodNames) {
            try {
                result = evaluateFactoryMethod(value, cls, pType, mName);
                if (result != null) {
                    factoryMethodEx = null;
                    break;
                }
            } catch (Exception ex) {
                // If it is enum and the method name is "fromValue" then don't throw
                // the exception immediately but try the next factory method
                factoryMethodEx = ex;
                if (!cls.isEnum() || !"fromValue".equals(mName)) {
                    break;
                }
            }
        }
        if (factoryMethodEx != null) {
            Throwable t = getOrThrowActualException(factoryMethodEx);
            Tr.error(tc, new org.apache.cxf.common.i18n.Message("CLASS_VALUE_OF_FAILURE",
                            BUNDLE,
                            cls.getName()).toString());
            throw new WebApplicationException(t, HttpUtils.getParameterFailureStatus(pType));
        }
        return result;
    }

    @FFDCIgnore({ NoSuchMethodException.class, IllegalAccessException.class })
    private static <T> T evaluateFactoryMethod(String value,
                                               Class<T> pClass,
                                               ParameterType pType,
                                               String methodName)
                    throws InvocationTargetException {
        try {
            Method m = pClass.getMethod(methodName, new Class<?>[] { String.class });
            if (Modifier.isStatic(m.getModifiers())) {
                return pClass.cast(m.invoke(null, new Object[] { value }));
            }
        } catch (NoSuchMethodException ex) {
            // no luck: try another factory methods
        } catch (IllegalAccessException ex) {
            // factory method is not accessible: try another
        }

        return null;
    }

    private static Throwable getOrThrowActualException(Throwable ex) {
        Throwable t = ex instanceof InvocationTargetException ? ((InvocationTargetException) ex).getCause() : ex;
        if (t instanceof WebApplicationException) {
            throw (WebApplicationException) t;
        }
        return t;
    }

    public static Object handleBean(Class<?> paramType, Annotation[] paramAnns,
                                    MultivaluedMap<String, String> values,
                                    ParameterType pType, Message message, boolean decoded) {
        Object bean = null;
        try {
            if (paramType.isInterface()) {
                paramType = org.apache.cxf.jaxrs.utils.JAXBUtils.getValueTypeFromAdapter(paramType,
                                                                                         paramType,
                                                                                         paramAnns);
            }
            bean = paramType.newInstance();
        } catch (IllegalAccessException ex) {
            reportServerError("CLASS_ACCESS_FAILURE", paramType.getName());
        } catch (Exception ex) {
            reportServerError("CLASS_INSTANTIATION_FAILURE", paramType.getName());
        }

        Map<String, MultivaluedMap<String, String>> parsedValues =
                        new HashMap<String, MultivaluedMap<String, String>>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            String memberKey = entry.getKey();
            String beanKey = null;

            int idx = memberKey.indexOf('.');
            if (idx == -1) {
                beanKey = "." + memberKey;
            } else {
                beanKey = memberKey.substring(0, idx);
                memberKey = memberKey.substring(idx + 1);
            }

            MultivaluedMap<String, String> value = parsedValues.get(beanKey);
            if (value == null) {
                value = new MetadataMap<String, String>();
                parsedValues.put(beanKey, value);
            }
            value.put(memberKey, entry.getValue());
        }

        if (!parsedValues.isEmpty()) {
            for (Map.Entry<String, MultivaluedMap<String, String>> entry : parsedValues.entrySet()) {
                String memberKey = entry.getKey();

                boolean isbean = !memberKey.startsWith(".");
                if (!isbean) {
                    memberKey = memberKey.substring(1);
                }

                Object setter = null;
                Object getter = null;
                for (Method m : paramType.getMethods()) {
                    if (m.getName().equalsIgnoreCase("set" + memberKey)
                        && m.getParameterTypes().length == 1) {
                        setter = m;
                    } else if (m.getName().equalsIgnoreCase("get" + memberKey)
                               || isBooleanType(m.getReturnType())
                               && m.getName().equalsIgnoreCase("is" + memberKey)) {
                        getter = m;
                    }
                    if (setter != null && getter != null) {
                        break;
                    }
                }
                if (setter == null) {
                    for (Field f : paramType.getFields()) {
                        if (f.getName().equalsIgnoreCase(memberKey)) {
                            setter = f;
                            getter = f;
                            break;
                        }
                    }
                }

                if (setter != null && getter != null) {
                    Class<?> type = null;
                    Type genericType = null;
                    Object paramValue = null;
                    if (setter instanceof Method) {
                        type = Method.class.cast(setter).getParameterTypes()[0];
                        genericType = Method.class.cast(setter).getGenericParameterTypes()[0];
                        paramValue = InjectionUtils.extractFromMethod(bean, (Method) getter);
                    } else {
                        type = Field.class.cast(setter).getType();
                        genericType = Field.class.cast(setter).getGenericType();
                        paramValue = InjectionUtils.extractFieldValue((Field) getter, bean);
                    }

                    List<MultivaluedMap<String, String>> processedValuesList =
                                    processValues(type, genericType, entry.getValue(), isbean);

                    for (MultivaluedMap<String, String> processedValues : processedValuesList) {
                        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
                            Object appendValue = InjectionUtils.injectIntoCollectionOrArray(type,
                                                                                            genericType, paramAnns, processedValues,
                                                                                            isbean, true,
                                                                                            pType, message);
                            paramValue = InjectionUtils.mergeCollectionsOrArrays(paramValue, appendValue,
                                                                                 genericType);
                        } else if (isSupportedMap(genericType)) {
                            Object appendValue = InjectionUtils.injectIntoMap(
                                                                              type, genericType, paramAnns, processedValues, true, pType, message);
                            paramValue = InjectionUtils.mergeMap(paramValue, appendValue, genericType);

                        } else if (isbean) {
                            paramValue = InjectionUtils.handleBean(type, paramAnns, processedValues,
                                                                   pType, message, decoded);
                        } else {
                            paramValue = InjectionUtils.handleParameter(
                                                                        processedValues.values().iterator().next().get(0),
                                                                        decoded, type, type, paramAnns, pType, message);
                        }

                        if (paramValue != null) {
                            if (setter instanceof Method) {
                                InjectionUtils.injectThroughMethod(bean, (Method) setter, paramValue);
                            } else {
                                InjectionUtils.injectFieldValue((Field) setter, bean, paramValue);
                            }
                        }
                    }
                }
            }
        }

        return bean;
    }

    @SuppressWarnings("unchecked")
    private static Object mergeMap(Object first, Object second, Type genericType) {
        if (first == null) {
            return second;
        } else if (first instanceof Map) {
            Map.class.cast(first).putAll((Map<?, ?>) second);
            return first;
        }
        return null;
    }

    // CHECKSTYLE:OFF
    private static Object injectIntoMap(Class<?> rawType, Type genericType,
                                        Annotation[] paramAnns,
                                        MultivaluedMap<String, String> processedValues,
                                        boolean decoded,
                                        ParameterType pathParam, Message message) {
        // CHECKSTYLE:ON
        ParameterizedType paramType = (ParameterizedType) genericType;
        Class<?> keyType = (Class<?>) paramType.getActualTypeArguments()[0];
        Type secondType = InjectionUtils.getType(paramType.getActualTypeArguments(), 1);

        if (secondType instanceof ParameterizedType) {
            MultivaluedMap<Object, Object> theValues = new MetadataMap<Object, Object>();
            ParameterizedType valueParamType = (ParameterizedType) secondType;
            Class<?> valueType = (Class<?>) InjectionUtils.getType(valueParamType
                            .getActualTypeArguments(), 0);

            for (Map.Entry<String, List<String>> processedValuesEntry : processedValues.entrySet()) {
                List<String> valuesList = processedValuesEntry.getValue();
                for (String value : valuesList) {
                    Object o = InjectionUtils.handleParameter(value,
                                                              decoded, valueType, valueType, paramAnns, pathParam, message);
                    theValues.add(convertStringToPrimitive(processedValuesEntry.getKey(), keyType), o);
                }
            }
            return theValues;
        }
        Map<Object, Object> theValues = new HashMap<>();
        Class<?> valueType =
                        (Class<?>) InjectionUtils.getType(paramType.getActualTypeArguments(), 1);
        for (Map.Entry<String, List<String>> processedValuesEntry : processedValues.entrySet()) {
            List<String> valuesList = processedValuesEntry.getValue();
            for (String value : valuesList) {
                Object o = InjectionUtils.handleParameter(value,
                                                          decoded, valueType, valueType, paramAnns, pathParam, message);
                theValues.put(
                              convertStringToPrimitive(processedValuesEntry.getKey(), keyType),
                              o);
            }
        }
        return theValues;

    }

    private static boolean isSupportedMap(Type genericType) {
        Class<?> rawType = getRawType(genericType);
        if (Map.class.isAssignableFrom(rawType) && genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            if (paramType.getActualTypeArguments().length == 2) {
                Class<?> firstType = getRawType(getType(paramType.getActualTypeArguments(), 0));
                Type secondType = getType(paramType.getActualTypeArguments(), 1);
                Class<?> secondRawType = getRawType(secondType);

                return InjectionUtils.isPrimitive(firstType)
                       && (InjectionUtils.isPrimitive(secondRawType)
                       || allowedMapListValue(secondRawType, secondType));
            }
        }
        return false;
    }

    private static boolean allowedMapListValue(Class<?> cls, Type type) {
        if (List.class.isAssignableFrom(cls)) {
            Class<?> listtype = getRawType(
                            getType(((ParameterizedType) type).getActualTypeArguments(), 0));
            return InjectionUtils.isPrimitive(listtype);
        }
        return false;
    }

    private static List<MultivaluedMap<String, String>> processValues(Class<?> type, Type genericType,
                                                                      MultivaluedMap<String, String> values,
                                                                      boolean isbean) {
        List<MultivaluedMap<String, String>> valuesList =
                        new ArrayList<MultivaluedMap<String, String>>();

        if (isbean && InjectionUtils.isSupportedCollectionOrArray(type)) {
            Class<?> realType = InjectionUtils.getActualType(genericType);
            for (Map.Entry<String, List<String>> entry : values.entrySet()) {
                String memberKey = entry.getKey();
                Class<?> memberType = null;

                for (Method m : realType.getMethods()) {
                    if (m.getName().equalsIgnoreCase("set" + memberKey)
                        && m.getParameterTypes().length == 1) {
                        memberType = m.getParameterTypes()[0];
                        break;
                    }
                }
                if (memberType == null) {
                    for (Field f : realType.getFields()) {
                        if (f.getName().equalsIgnoreCase(memberKey)) {
                            memberType = f.getType();
                            break;
                        }
                    }
                }

                // Strip values tied to collection/array fields from beans that are within
                // collection/array themselves, the only way to support this would be to have
                // an indexing syntax for nested beans, perhaps like this:
                //    a(0).b=1&a(0).b=2&a(1).b=3&a(1).b=4
                // For now though we simply don't support this capability. To illustrate, the 'c'
                // param is dropped from this multivaluedmap example since it is a list:
                //    {c=[71, 81, 91, 72, 82, 92], a=[C1, C2], b=[790, 791]}
                if (memberType != null && InjectionUtils.isSupportedCollectionOrArray(memberType)) {
                    continue;
                }

                // Split multivaluedmap value list contents into separate multivaluedmap instances
                // whose list contents are only 1 level deep, for example:
                //    {a=[C1, C2], b=[790, 791]}
                // becomes these 2 separate multivaluedmap instances:
                //    {a=[C1], b=[790]} and {a=[C2], b=[791]}
                int idx = 0;
                for (String value : entry.getValue()) {
                    MultivaluedMap<String, String> splitValues =
                                    (idx < valuesList.size()) ? valuesList.get(idx) : null;
                    if (splitValues == null) {
                        splitValues = new MetadataMap<String, String>();
                        valuesList.add(splitValues);
                    }
                    splitValues.add(memberKey, value);
                    idx++;
                }
            }
        } else {
            valuesList.add(values);
        }

        return valuesList;
    }

    public static boolean isSupportedCollectionOrArray(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || type.isArray();
    }

    @SuppressWarnings("unchecked")
    private static Object mergeCollectionsOrArrays(Object first, Object second, Type genericType) {
        if (first == null) {
            return second;
        } else if (first instanceof Collection) {
            Collection.class.cast(first).addAll((Collection<?>) second);
            return first;
        } else {
            int firstLen = Array.getLength(first);
            int secondLen = Array.getLength(second);
            Object mergedArray = Array.newInstance(InjectionUtils.getActualType(genericType),
                                                   firstLen + secondLen);
            System.arraycopy(first, 0, mergedArray, 0, firstLen);
            System.arraycopy(second, 0, mergedArray, firstLen, secondLen);
            return mergedArray;
        }
    }

    static Class<?> getCollectionType(Class<?> rawType) {
        Class<?> type = null;
        if (SortedSet.class.isAssignableFrom(rawType)) {
            type = TreeSet.class;
        } else if (Set.class.isAssignableFrom(rawType)) {
            type = HashSet.class;
        } else if (Collection.class.isAssignableFrom(rawType)) {
            type = ArrayList.class;
        }
        return type;

    }

    //CHECKSTYLE:OFF
    private static Object injectIntoCollectionOrArray(Class<?> rawType,
                                                      Type genericType,
                                                      Annotation[] paramAnns,
                                                      MultivaluedMap<String, String> values,
                                                      boolean isbean, boolean decoded,
                                                      ParameterType pathParam, Message message) {
        //CHECKSTYLE:ON
        Class<?> type = getCollectionType(rawType);

        Class<?> realType = null;
        Type realGenericType = null;
        if (rawType.isArray()) {
            realType = rawType.getComponentType();
            realGenericType = realType;
        } else {
            Type[] types = getActualTypes(genericType);
            if (types == null || types.length == 0 || !(types[0] instanceof ParameterizedType)) {
                realType = getActualType(genericType);
                realGenericType = realType;
            } else {
                realType = getRawType(types[0]);
                realGenericType = types[0];
            }
        }
        Object theValues = null;
        if (type != null) {
            try {
                theValues = type.newInstance();
            } catch (IllegalAccessException ex) {
                reportServerError("CLASS_ACCESS_FAILURE", type.getName());
            } catch (Exception ex) {
                reportServerError("CLASS_INSTANTIATION_FAILURE", type.getName());
            }
        } else {
            theValues = Array.newInstance(realType, isbean ? 1 : values.values().iterator().next().size());
        }
        if (isbean) {
            Object o = InjectionUtils.handleBean(realType, paramAnns, values, pathParam, message, decoded);
            addToCollectionValues(theValues, o, 0);
        } else {
            List<String> valuesList = values.values().iterator().next();
            valuesList = checkPathSegment(valuesList, realType, pathParam);
            for (int ind = 0; ind < valuesList.size(); ind++) {
                Object o = InjectionUtils.handleParameter(valuesList.get(ind), decoded,
                               realType, realGenericType, paramAnns, pathParam, message);
                addToCollectionValues(theValues, o, ind);
            }
        }
        return theValues;
    }

    @SuppressWarnings("unchecked")
    private static void addToCollectionValues(Object theValues, Object o, int index) {
        if (o != null) {
            if (theValues instanceof Collection) {
                Collection.class.cast(theValues).add(o);
            } else if (theValues.getClass().getComponentType().isPrimitive()) {
                Array.set(theValues, index, o);
            } else {
                ((Object[]) theValues)[index] = o;
            }
        }
    }

    private static List<String> checkPathSegment(List<String> values, Class<?> type,
                                                 ParameterType pathParam) {
        if (pathParam != ParameterType.PATH || !PathSegment.class.isAssignableFrom(type)) {
            return values;
        }
        List<String> newValues = new ArrayList<>();
        for (String v : values) {
            String[] segments = StringUtils.split(v, "/");
            for (String s : segments) {
                if (s.length() != 0) {
                    newValues.add(s);
                }
            }
            if (v.endsWith("/")) {
                newValues.add("");
            }
        }
        return newValues;
    }

    //
    //CHECKSTYLE:OFF
    public static Object createParameterObject(List<String> paramValues,
                                               Class<?> paramType,
                                               Type genericType,
                                               Annotation[] paramAnns,
                                               String defaultValue,
                                               boolean decoded,
                                               ParameterType pathParam,
                                               Message message) {
        //CHECKSTYLE:ON

        if (paramValues == null) {
            if (defaultValue != null) {
                paramValues = Collections.singletonList(defaultValue);
            } else {
                if (paramType.isPrimitive()) {
                    paramValues = Collections.singletonList(
                                    boolean.class == paramType ? "false"
                                                    : char.class == paramType ? Character.toString('\u0000') : "0");
                } else if (InjectionUtils.isSupportedCollectionOrArray(paramType)) {
                    paramValues = Collections.emptyList();
                } else {
                    return null;
                }
            }
        }

        Object value = null;
        if (InjectionUtils.isSupportedCollectionOrArray(paramType)) {
            MultivaluedMap<String, String> paramValuesMap = new MetadataMap<String, String>();
            paramValuesMap.put("", paramValues);
            value = InjectionUtils.injectIntoCollectionOrArray(paramType, genericType, paramAnns,
                                                               paramValuesMap, false, decoded, pathParam, message);
        } else {
            String result = null;
            if (!paramValues.isEmpty()) {
                boolean isLast = pathParam == ParameterType.PATH ? true : false;
                result = isLast ? paramValues.get(paramValues.size() - 1)
                                : paramValues.get(0);
            }
            if (result != null) {
                value = InjectionUtils.handleParameter(result, decoded, paramType, genericType,
                                                       paramAnns, pathParam, message);
            }
        }
        return value;
    }

    // TODO : investigate the possibility of using generic proxies only
    @SuppressWarnings("unchecked")
    public static <T> ThreadLocalProxy<T> createThreadLocalProxy(Class<T> type) {
        ThreadLocalProxy<?> proxy = null;
        if (UriInfo.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalUriInfo();
        } else if (HttpHeaders.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalHttpHeaders();
        } else if (ProtocolHeaders.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalProtocolHeaders();
        } else if (SecurityContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalSecurityContext();
        } else if (ContextResolver.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalContextResolver<Object>();
        } else if (Request.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalRequest();
        } else if (Providers.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalProviders();
        } else if (MessageContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalMessageContext();
// Liberty Change for CXF Begin
        } else if (type.getName().equals(MessageContext.class.getName())) {
            MessageContextProxyClassLoader loader = new MessageContextProxyClassLoader(getClassLoader(Proxy.class), getClassLoader(type), getClassLoader(ThreadLocalProxy.class));
            proxy = (ThreadLocalProxy<T>) Proxy.newProxyInstance(loader,
                                                                 new Class[] { type, ThreadLocalProxy.class },
                                                                 new ProxyInvocationHandler(new ThreadLocalMessageContext()));
// Liberty Change for CXF Begin
        }

        if (proxy == null && isServletApiContext(type.getName())) {
            proxy = createThreadLocalServletApiContext(type.getName());
        }
        if (proxy == null) {
            ProxyClassLoader loader = new ProxyClassLoader(getClassLoader(Proxy.class));
            loader.addLoader(getClassLoader(type));
            loader.addLoader(getClassLoader(ThreadLocalProxy.class));
            return (ThreadLocalProxy<T>) Proxy.newProxyInstance(loader,
                                                                new Class[] { type, ThreadLocalProxy.class },
                                                                new ThreadLocalInvocationHandler<T>());
        }

        return (ThreadLocalProxy<T>) proxy;
    }

    private static boolean isServletApiContext(String name) {
        return name.startsWith("javax.servlet.");
    }

    private static ThreadLocalProxy<?> createThreadLocalServletApiContext(String name) {
        String proxyClassName = null;
        if (HTTP_SERVLET_REQUEST_CLASS_NAME.equals(name)) {
            proxyClassName = "org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletRequest";
        } else if (HTTP_SERVLET_RESPONSE_CLASS_NAME.equals(name)) {
            proxyClassName = "org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletResponse";
        } else if (SERVLET_CONTEXT_CLASS_NAME.equals(name)) {
            proxyClassName = "org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletContext";
        } else if (SERVLET_CONFIG_CLASS_NAME.equals(name)) {
            proxyClassName = "org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletConfig";
        }
        if (proxyClassName != null) {
            try {
                // Liberty Change for CXF Begin
                return (ThreadLocalProxy<?>) getClassLoader(InjectionUtils.class).loadClass(proxyClassName).newInstance();
                // Liberty Change for CXF Begin
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        return null;
    }

    public static Method getGetterFromSetter(Method setter) throws Exception {
        return setter.getDeclaringClass().getMethod("get" + setter.getName().substring(3));
    }

    // Liberty Change for CXF Begin
    public static void injectContextProxiesAndApplication(AbstractResourceInfo cri,
                                                          Object instance,
                                                          Application app,
                                                          ProviderFactory factory) {

        /** inject proxy for singleton only */
        if (!cri.isSingleton())
            return;

        /** application inject has been done earlier. @see LibertyJaxRsServerFactoryBean.injectContextApplication() */
        if (cri instanceof ApplicationInfo)
            return;

        JaxRsFactoryBeanCustomizer beanCustomizer = InjectionRuntimeContextHelper.findBeanCustomizer(instance.getClass(), cri.getBus());
        Boolean isManagedBean = beanCustomizer == null ? false : true;

        InjectionRuntimeContext irc = InjectionRuntimeContextHelper.getRuntimeContext();
        synchronized (instance) {
            for (Map.Entry<Class<?>, Method> entry : cri.getContextMethods().entrySet()) {
                Method method = entry.getValue();
                Object value = null;
                Class<?> cls = method.getParameterTypes()[0];
                if (cls == Application.class) {
                  value = app;
                } else if (VALUE_CONTEXTS.contains(cls.getName()) && factory != null) {
                    ContextProvider<?> p = factory.createContextProvider(cls, null);
                    if (p != null) {
                        value = p.createContext(null);
                    }
                } else {
                    value = cri.getContextSetterProxy(method);
                }

                if (isManagedBean)
                    irc.setRuntimeCtxObject(entry.getKey().getName(), value);
                else
                    InjectionUtils.injectThroughMethod(instance, method, value);
            }

            for (Field f : cri.getContextFields()) {
                Object value = null;
                Class<?> cls = f.getType();
                if (cls == Application.class) {
                    value = app;
                } else if (VALUE_CONTEXTS.contains(cls.getName()) && factory != null) {
                    ContextProvider<?> p = factory.createContextProvider(cls, null);
                    if (p != null) {
                        value = p.createContext(null);
                    }
                } else {
                    value = cri.getContextFieldProxy(f);
                }

                if (isManagedBean)
                    irc.setRuntimeCtxObject(f.getType().getName(), value);
                else
                    InjectionUtils.injectFieldValue(f, instance, value);
            }
        }

        Object o = null;
        //replace singleton resource after context injection is done.
        if (isManagedBean && (cri instanceof ClassResourceInfo)) {
            o = beanCustomizer.onSingletonServiceInit(instance,
                                                      InjectionRuntimeContextHelper.getBeanCustomizerContext(beanCustomizer, cri.getBus()));

            SingletonResourceProvider sp = new SingletonResourceProvider(o);
            ((ClassResourceInfo) cri).setResourceProvider(sp);
            instance = o;
        }

        if (!isManagedBean && (cri instanceof ClassResourceInfo))
        {
            //call postConstruct method for singleton resource which are non-CDI/EJB
            Method postConstructMethod = ResourceUtils.findPostConstructMethod(instance.getClass());
            InjectionUtils.invokeLifeCycleMethod(instance, postConstructMethod);
        }

    }

    // Liberty Change for CXF End

    public static void injectContextProxies(AbstractResourceInfo cri, Object instance) {
        injectContextProxiesAndApplication(cri, instance, null, null);
    }

    @SuppressWarnings("unchecked")
    public static void injectContextField(AbstractResourceInfo cri,
                                          Field f, Object o, Object value) {
        if (!cri.isSingleton()) {
            InjectionUtils.injectFieldValue(f, o, value);
        } else {
            ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>) cri.getContextFieldProxy(f);
            if (proxy != null) {
                proxy.set(value);
            }
        }
    }

    // Liberty Change for CXF Begin
    @SuppressWarnings("unchecked")
    public static void injectManagedObjectContextField(AbstractResourceInfo cri,
                                                       Field f, Object o, Object value) {

        InjectionRuntimeContext irc = null;
        irc = InjectionRuntimeContextHelper.getRuntimeContext();
        irc.setRuntimeCtxObject(f.getType().getName(), value);
//      if (!cri.isSingleton()) {
//
//          irc.setRuntimeCtxObject(o.getClass().getName()
//                                  + "-" + f.getType().getName(), value);
//      } else {
//          ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>) cri.getContextFieldProxy(f);
//          if (proxy != null) {
//              proxy.set(value);
//          }
//
//      }
    }

    public static void injectContexts(Object requestObject,
                                      AbstractResourceInfo resource,
                                      Message message) {

        if (resource.contextsAvailable()) {

            final Class clz;

            if (((resource instanceof ProviderInfo)) && !(resource instanceof ApplicationInfo) && resource.getConstructorProxies() == null) {

                ProviderInfo<?> pi = (ProviderInfo<?>) resource;

                Object oldProvider = pi.getOldProvider();
                clz = oldProvider.getClass();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "injectContexts pre: oldProvider=" + oldProvider + " clz=" + clz + " loader="+getClassLoader(clz));
                }
            } else {
                clz = requestObject.getClass();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "injectContexts post: clz=" + clz + " loader="+getClassLoader(clz));
            }
            JaxRsFactoryBeanCustomizer beanCustomizer = InjectionRuntimeContextHelper.findBeanCustomizer(clz, resource.getBus());
            if (beanCustomizer != null)
            {
                injectManagedObjectContextMethods(requestObject, resource, message);
                injectManagedObjectContextFields(requestObject, resource, message);
            }
            else {
                injectContextMethods(requestObject, resource, message);
                injectContextFields(requestObject, resource, message);
                //call postConstruct method for per-request resource which has context injection
                //and is non-cdi/ejb
                if (!resource.isSingleton())
                {
                    Method postConstructMethod = ResourceUtils.findPostConstructMethod(requestObject.getClass());
                    InjectionUtils.invokeLifeCycleMethod(requestObject, postConstructMethod);
                }
            }
            if (resource instanceof ApplicationInfo)
            {

                injectManagedObjectContextMethods(requestObject, resource, message);
                injectManagedObjectContextFields(requestObject, resource, message);

            }
            injectConstructorProxies(requestObject, resource, message);

        }

        /**
         * Provider replacement should be here
         * when the first call come, the Provider init and @PostConstruct can get the context values
         */
        InjectionRuntimeContextHelper.initSingletonEJBCDIProvider(resource, message, requestObject);
    }

// Liberty Change for CXF End

    @SuppressWarnings("unchecked")
    public static void injectContextMethods(Object requestObject,
                                            AbstractResourceInfo cri,
                                            Message message) {

        for (Map.Entry<Class<?>, Method> entry : cri.getContextMethods().entrySet()) {
            Method method = entry.getValue();
            if (VALUE_CONTEXTS.contains(method.getParameterTypes()[0].getName()) && cri.isSingleton()) {
                continue;
            }
            Object o = JAXRSUtils.createContextValue(message,
                                                     method.getGenericParameterTypes()[0],
                                                     entry.getKey());

            if (o != null) {
                if (!cri.isSingleton()) {
                    InjectionUtils.injectThroughMethod(requestObject, method, o, message);
                } else {
                    ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>) cri.getContextSetterProxy(method);
                    if (proxy != null) {
                        proxy.set(o);
                    }
                }

            }
        }
    }

// Liberty Change for CXF Begain
    /**
     * @param requestObject
     * @param resource
     */
    public static void injectManagedObjectContextMethods(Object requestObject,
                                                         AbstractResourceInfo cri,
                                                         Message message) {
        for (Map.Entry<Class<?>, Method> entry : cri.getContextMethods().entrySet()) {
            Method method = entry.getValue();
//            if (method.getParameterTypes()[0] == Application.class && cri.isSingleton()) {
//                continue;
//            }
            Object o = JAXRSUtils.createContextValue(message,
                                                     method.getGenericParameterTypes()[0],
                                                     entry.getKey());

            InjectionRuntimeContext irc = InjectionRuntimeContextHelper.getRuntimeContext();
            if (o != null) {
                irc.setRuntimeCtxObject(entry.getKey().getName(), o);
            }

//            if (o != null) {
//                if (!cri.isSingleton()) {
////                    irc.setRuntimeCtxObject(requestObject.getClass().getName()
////                                            + "-" + method.getParameterTypes()[0].getName(), o);
//
//                } else {
//                    ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>) cri.getContextSetterProxy(method);
//                    if (proxy != null) {
//                        proxy.set(o);
//                    }
//                }
//            }
        }
    }

// Liberty Change for CXF End
    public static void injectContextFields(Object o,
                                           AbstractResourceInfo cri,
                                           Message m) {

        for (Field f : cri.getContextFields()) {
            if (VALUE_CONTEXTS.contains(f.getType().getName()) && cri.isSingleton()) {
                continue;
            }
            Object value = JAXRSUtils.createContextValue(m, f.getGenericType(), f.getType());
            if (value != null)
                InjectionUtils.injectContextField(cri, f, o, value);
        }
    }

// Liberty Change for CXF Begain
    public static void injectManagedObjectContextFields(Object o,
                                                        AbstractResourceInfo cri,
                                                        Message m) {

        for (Field f : cri.getContextFields()) {
//            if (f.getType() == Application.class && cri.isSingleton()) {
//                continue;
//            }
            Object value = JAXRSUtils.createContextValue(m, f.getGenericType(), f.getType());
            InjectionUtils.injectManagedObjectContextField(cri, f, o, value);
        }
    }

// Liberty Change for CXF End
    @SuppressWarnings("unchecked")
    public static void injectConstructorProxies(Object o,
                                                AbstractResourceInfo cri,
                                                Message m) {

        Map<Class<?>, ThreadLocalProxy<?>> proxies = cri.getConstructorProxies();
        if (proxies != null) {
            for (Map.Entry<Class<?>, ThreadLocalProxy<?>> entry : proxies.entrySet()) {
                Object value = JAXRSUtils.createContextValue(m, entry.getKey(), entry.getKey());
                ((ThreadLocalProxy<Object>) entry.getValue()).set(value);
            }
        }
    }

    public static MultivaluedMap<String, Object> extractValuesFromBean(Object bean, String baseName) {
        MultivaluedMap<String, Object> values = new MetadataMap<String, Object>();
        fillInValuesFromBean(bean, baseName, values);
        return values;
    }

    private static boolean isBooleanType(Class<?> cls) {
        return boolean.class == cls || Boolean.class == cls;
    }

    public static void fillInValuesFromBean(Object bean, String baseName,
                                            MultivaluedMap<String, Object> values) {
        for (Method m : bean.getClass().getMethods()) {
            String methodName = m.getName();
            boolean startsFromGet = methodName.startsWith("get");
            if ((startsFromGet
                || isBooleanType(m.getReturnType()) && methodName.startsWith("is"))
                && m.getParameterTypes().length == 0) {

                int minLen = startsFromGet ? 3 : 2;
                if (methodName.length() <= minLen) {
                    continue;
                }

                String propertyName = methodName.substring(minLen);
                if (propertyName.length() == 1) {
                    propertyName = propertyName.toLowerCase();
                } else {
                    propertyName = propertyName.substring(0, 1).toLowerCase()
                                   + propertyName.substring(1);
                }
                if (baseName.contains(propertyName)
                    || "class".equals(propertyName)
                    || "declaringClass".equals(propertyName)) {
                    continue;
                }
                if (!"".equals(baseName)) {
                    propertyName = baseName + "." + propertyName;
                }

                Object value = extractFromMethod(bean, m);
                if (value == null) {
                    continue;
                }
                if (isPrimitive(value.getClass()) || Date.class.isAssignableFrom(value.getClass())) {
                    values.putSingle(propertyName, value);
                } else if (value.getClass().isEnum()) {
                    values.putSingle(propertyName, value.toString());
                } else if (isSupportedCollectionOrArray(value.getClass())) {
                    List<Object> theValues = null;
                    if (value.getClass().isArray()) {
                        theValues = Arrays.asList((Object[]) value);
                    } else if (value instanceof Set) {
                        theValues = new ArrayList<>((Set<?>) value);
                    } else {
                        theValues = CastUtils.cast((List<?>) value);
                    }
                    values.put(propertyName, theValues);
                } else if (Map.class.isAssignableFrom(value.getClass())) {
                    if (isSupportedMap(m.getGenericReturnType())) {
                        Map<Object, Object> map = CastUtils.cast((Map<?, ?>) value);
                        for (Map.Entry<Object, Object> entry : map.entrySet()) {
                            values.add(propertyName + "." + entry.getKey().toString(),
                                       entry.getValue().toString());
                        }
                    }
                } else {
                    fillInValuesFromBean(value, propertyName, values);
                }
            }
        }
    }

    public static Map<Parameter, Class<?>> getParametersFromBeanClass(Class<?> beanClass,
                                                                      ParameterType type,
                                                                      boolean checkIgnorable) {
        Map<Parameter, Class<?>> params = new LinkedHashMap<Parameter, Class<?>>();
        for (Method m : beanClass.getMethods()) {
            String methodName = m.getName();
            boolean startsFromGet = methodName.startsWith("get");
            if ((startsFromGet
                || isBooleanType(m.getReturnType()) && methodName.startsWith("is"))
                && m.getParameterTypes().length == 0) {

                int minLen = startsFromGet ? 3 : 2;
                if (methodName.length() <= minLen) {
                    continue;
                }
                String propertyName = StringUtils.uncapitalize(methodName.substring(minLen));
                if (m.getReturnType() == Class.class
                    || checkIgnorable && canPropertyBeIgnored(m, propertyName)) {
                    continue;
                }
                params.put(new Parameter(type, propertyName), m.getReturnType());
            }
        }
        return params;
    }

    private static boolean canPropertyBeIgnored(Method m, String propertyName) {
        for (Annotation ann : m.getAnnotations()) {
            String annType = ann.annotationType().getName();
            if ("org.apache.cxf.aegis.type.java5.IgnoreProperty".equals(annType)
                || "javax.xml.bind.annotation.XmlTransient".equals(annType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPrimitive(Class<?> type) {
        return String.class == type
               || isPrimitiveOnly(type);
    }

    public static boolean isPrimitiveOnly(Class<?> type) {
        return type.isPrimitive()
               || Number.class.isAssignableFrom(type)
               || Boolean.class == type
               || Character.class == type;
    }

    public static String decodeValue(String value, boolean decode, ParameterType param) {
        if (!decode) {
            return value;
        }
        if (param == ParameterType.PATH || param == ParameterType.MATRIX) {
            return HttpUtils.pathDecode(value);
        } else {
            return HttpUtils.urlDecode(value);
        }
    }

    public static void invokeLifeCycleMethod(Object instance, Method method) {
        if (method != null) {
            method = InjectionUtils.checkProxy(method, instance);
            try {
                method.invoke(instance, new Object[] {});
            } catch (InvocationTargetException ex) {
                String msg = "Method " + method.getName() + " can not be invoked"
                             + " due to InvocationTargetException";
                throw new WebApplicationException(JAXRSUtils.toResponseBuilder(500).entity(msg).build());
            } catch (IllegalAccessException ex) {
                String msg = "Method " + method.getName() + " can not be invoked"
                             + " due to IllegalAccessException";
                throw ExceptionUtils.toInternalServerErrorException(ex,
                                                                    JAXRSUtils.toResponseBuilder(500).entity(msg).build());
            }
        }
    }

    public static Object convertStringToPrimitive(String value, Class<?> cls) {
        return convertStringToPrimitive(value, cls, new Annotation[] {});
    }

    public static Object convertStringToPrimitive(String value, Class<?> cls, Annotation[] anns) {
        Message m = JAXRSUtils.getCurrentMessage();
        Object obj = createFromParameterHandler(value, cls, cls, anns, m);
        if (obj != null) {
            return obj;
        }
        if (String.class == cls) {
            return value;
        } else if (cls.isPrimitive()) {
            return PrimitiveUtils.read(value, cls);
        } else if (cls.isEnum()) {
            if (m != null && !MessageUtils.getContextualBoolean(m, ENUM_CONVERSION_CASE_SENSITIVE, false)) {
                obj = invokeValueOf(value.toUpperCase(), cls);
            }
            if (obj == null) {
                try {
                    obj = invokeValueOf(value, cls);
                } catch (RuntimeException ex) {
                    if (m == null) {
                        obj = invokeValueOf(value.toUpperCase(), cls);
                    } else {
                        throw ex;
                    }
                }
            }
            return obj;
        } else {
            try {
                Constructor<?> c = cls.getConstructor(new Class<?>[] { String.class });
                return c.newInstance(new Object[] { value });
            } catch (Throwable ex) {
                // try valueOf
            }
            return invokeValueOf(value, cls);
        }
    }

    private static Object invokeValueOf(String value, Class<?> cls) {
        try {
            Method m = cls.getMethod("valueOf", new Class[] { String.class });
            return m.invoke(null, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Class<?> getRawResponseClass(Object targetObject) {
        if (targetObject != null) {
            Class<?> targetClass = targetObject.getClass();
            return ClassHelper.getRealClassFromClass(targetClass);
        }
        return null;
    }

// Liberty Change for CXF Begain
    private static boolean isAsyncMethod(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> c : parameterTypes)
        {
            if (c.isAssignableFrom(javax.ws.rs.container.AsyncResponse.class))
                return true;
        }
        return false;
    }

    /**
     * Hack to generate a type class for collection object.
     *
     * @param targetObject
     * @return
     */
    private static Type getAsynchronizedGenericType(Object targetObject) {
        if (targetObject instanceof java.util.Collection) {
            Class<? extends java.util.Collection> rawType = (Class<? extends Collection>) targetObject.getClass();
            Class<?> actualType = Object.class;
            if (((java.util.Collection<?>) targetObject).size() > 0) {
                Object element = ((java.util.Collection<?>) targetObject).iterator().next();
                actualType = element.getClass();
            }
            return new ParameterizedType() {
                private Type actualType, rawType;

                public ParameterizedType setTypes(Type actualType, Type rawType) {
                    this.actualType = actualType;
                    this.rawType = rawType;
                    return this;
                }

                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[] { actualType };
                }

                @Override
                public Type getRawType() {
                    return rawType;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            }.setTypes(actualType, rawType);

        } else
            return targetObject.getClass();
    }

    public static Type getGenericResponseType(Method invoked,
                                              Class<?> serviceCls,
                                              Object targetObject,
                                              Class<?> targetType,
                                              Exchange exchange) {
        if (targetObject == null) {
            return null;
        }
        Type type = null;

        if (GenericEntity.class.isAssignableFrom(targetObject.getClass())) {
            type = processGenericTypeIfNeeded(serviceCls, targetType, ((GenericEntity<?>) targetObject).getType());
        } else if ((invoked != null) && (invoked.getReturnType().isAssignableFrom(targetType))) {
            type = processGenericTypeIfNeeded(serviceCls, targetType, invoked.getGenericReturnType());
        } else if ((invoked != null) && (isAsyncMethod(invoked))) {
            //TODO: interesting
            type = processGenericTypeIfNeeded(serviceCls, targetType, getAsynchronizedGenericType(targetObject));
        }

        else {

            // when a method has been invoked it is still possible that either an ExceptionMapper
            // or a ResponseHandler filter overrides a response entity; if it happens then
            // the Type is the class of the response object, unless this new entity is assignable
            // to invoked.getReturnType(); same applies to the case when a method returns Response

            type = targetObject.getClass();
        }

        return type;
    }

    // Liberty Change for CXF End
    public static Class<?> updateParamClassToTypeIfNeeded(Class<?> paramCls, Type type) {
        if (paramCls != type && type instanceof Class) {
            Class<?> clsType = (Class<?>) type;
            if (paramCls.isAssignableFrom(clsType)
                || clsType != Object.class && !clsType.isInterface() && clsType.isAssignableFrom(paramCls)) {
                paramCls = clsType;
            }
        }
        return paramCls;
    }

    public static Type processGenericTypeIfNeeded(Class<?> serviceCls, Class<?> paramCls, Type type) {

        if (type instanceof TypeVariable) {
            type = InjectionUtils.getSuperType(serviceCls, (TypeVariable<?>) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            if (pt.getActualTypeArguments()[0] instanceof TypeVariable
                && isSupportedCollectionOrArray(getRawType(pt))) {
                TypeVariable<?> typeVar = (TypeVariable<?>)pt.getActualTypeArguments()[0];
                Type theType = InjectionUtils.getSuperType(serviceCls, typeVar);
                if (theType instanceof Class) {
                    type = new ParameterizedCollectionType((Class<?>)theType);
                } else {
                    type = processGenericTypeIfNeeded(serviceCls, paramCls, theType);
                    type = new ParameterizedCollectionType(type);
                }
            }
        }

        if (type == null || type == Object.class) {
            type = paramCls;
        }
        return type;

    }

    public static Object getEntity(Object o) {
        return o instanceof GenericEntity ? ((GenericEntity<?>) o).getEntity() : o;
    }

    private static final Set<String> JAXRS_COMPONENTS_INTERFACE;
    static {
        JAXRS_COMPONENTS_INTERFACE = new HashSet<String>();
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.MessageBodyWriter");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.MessageBodyReader");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.ExceptionMapper");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.ContextResolver");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.ReaderInterceptor");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.WriterInterceptor");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.ext.ParamConverterProvider");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.container.ContainerRequestFilter");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.container.ContainerResponseFilter");
        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.container.DynamicFeature");
        JAXRS_COMPONENTS_INTERFACE.add("org.apache.cxf.jaxrs.ext.ContextResolver");

        JAXRS_COMPONENTS_INTERFACE.add("javax.ws.rs.core.Application");

    }

    private static final Set<String> JAXRS_COMPONENTS_ABSTRACTCLASS;
    static {
        JAXRS_COMPONENTS_ABSTRACTCLASS = new HashSet<String>();
        JAXRS_COMPONENTS_ABSTRACTCLASS.add("javax.ws.rs.core.Application");
    }

    /**
     * @param moduleContainer
     * @return
     * @throws UnableToAdaptException
     */
    public static List<String> getJaxRsInjectionClasses(Container moduleContainer) {

//        if (!JAXRS_JEE_COMPONENTS.isEmpty()) {
//            return JAXRS_JEE_COMPONENTS;
//        }

        WebAnnotations webAnnotations = null;
        try {
            webAnnotations = moduleContainer
                            .adapt(WebAnnotations.class);
            webAnnotations.openInfoStore();
            AnnotationTargets_Targets annotationTargets = webAnnotations
                            .getAnnotationTargets();
            Set<String> allComponentsClassNames = new HashSet<String>();
            //We should skip the annotation scan on Interface

            // Scan annotation for @Provider, @Path, @ApplicationPath
            allComponentsClassNames.addAll(annotationTargets.getAllInheritedAnnotatedClasses(
                                                                                             Provider.class.getName(),
                                                                                             AnnotationTargets_Targets.POLICY_SEED));
            allComponentsClassNames
                            .addAll(annotationTargets.getAllInheritedAnnotatedClasses(
                                                                                      Path.class.getName(),
                                                                                      AnnotationTargets_Targets.POLICY_SEED));
            allComponentsClassNames.addAll(annotationTargets.getAllInheritedAnnotatedClasses(
                                                                                             ApplicationPath.class.getName(), AnnotationTargets_Targets.POLICY_SEED));

            Set<String> allComponentsClassNamesFinal = new HashSet<String>();

            //add the valid provider but no @Provider

            for (String str : JAXRS_COMPONENTS_INTERFACE) {
                allComponentsClassNames.addAll(annotationTargets.getAllImplementorsOf(str));
            }

            for (String str : JAXRS_COMPONENTS_ABSTRACTCLASS) {

                allComponentsClassNames.addAll(annotationTargets.getSubclassNames(str));
            }

            //filter the interface but add all implementors
            //filter the class without @inject, @resources
            //filter the class with @ApplicationScoped,@SessionScoped,@RequestScoped,@Dependent
            for (String str : allComponentsClassNames) {

                ClassInfo targetClass = webAnnotations.getClassInfo(str);
                Set<String> impl = annotationTargets.getAllImplementorsOf(str);
                if (!impl.isEmpty()) {
                    if (shouldConsiderInjection(targetClass)) {
                        allComponentsClassNamesFinal.addAll(impl);
                    }

                } else {
                    if (!targetClass.isInterface()) {
                        if (shouldConsiderInjection(targetClass)) {
                            allComponentsClassNamesFinal.add(str);
                        }
                    }

                }

            }

            webAnnotations.closeInfoStore();
            return new ArrayList<String>(allComponentsClassNamesFinal);
        } catch (Exception e) {
//            LOG.log(Level.FINE, "Exception when retrieve the JAX-RS components: ", e);
        }
        return Collections.<String> emptyList();
    }

    /**
     * @param targetClass
     */
    private static boolean shouldConsiderInjection(ClassInfo clazz) {
//        if (targetClass.getAnnotation("javax.inject.Inject") != null || targetClass.getAnnotation("javax.annotation.ManagedBean") != null
//            || targetClass.getAnnotation("javax.annotation.Resource") != null) {

        if (clazz.isAnnotationPresent("javax.inject.Inject") && !clazz.isAnnotationWithin(explicitLifcycle)) {
            return true;
        } else {

            List<? extends com.ibm.wsspi.anno.info.FieldInfo> fields = clazz.getDeclaredFields();
            Iterator<? extends com.ibm.wsspi.anno.info.FieldInfo> fieldToCheck = fields.iterator();
            while (fieldToCheck.hasNext()) {
                if (fieldToCheck.next().isAnnotationPresent("javax.inject.Inject")) {
                    return true;
                }
            }

            List<? extends MethodInfo> methods = clazz.getDeclaredMethods();
            Iterator<? extends MethodInfo> methodToCheck = methods.iterator();
            while (methodToCheck.hasNext()) {
                if (methodToCheck.next().isAnnotationPresent("javax.inject.Inject")) {
                    return true;
                }
            }

            List<? extends MethodInfo> c = clazz.getDeclaredConstructors();
            Iterator<? extends MethodInfo> constructorToCheck = c.iterator();
            while (constructorToCheck.hasNext()) {
                if (constructorToCheck.next().isAnnotationPresent("javax.inject.Inject")) {
                    return true;
                }
            }
        }
        ClassInfo cls = clazz.getSuperclass();
        if (cls != null) {
            return shouldConsiderInjection(cls);
        } else {
            return false;
        }
    }
    
    private static ClassLoader getClassLoader(final Class<?> clazz) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return clazz.getClassLoader();
            }
        });
    }
}

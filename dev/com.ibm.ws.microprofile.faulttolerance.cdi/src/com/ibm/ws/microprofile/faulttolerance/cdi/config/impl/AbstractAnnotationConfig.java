/*******************************************************************************
 * Copyright (c) 2017 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.FTUtils;

public class AbstractAnnotationConfig<T extends Annotation> {

    private static final TraceComponent tc = Tr.register(AbstractAnnotationConfig.class);

    private final Class<T> annotationType;
    private final String keyPrefix;
    private final T annotation;
    private final Class<?> annotatedClass;
    private final String targetName;
    private final Method annotatedMethod;

    public AbstractAnnotationConfig(Class<?> annotatedClass, T annotation, Class<T> annotationType) {
        this(null, annotatedClass, annotation, annotationType);
    }

    public AbstractAnnotationConfig(Method annotatedMethod, Class<?> annotatedClass, T annotation, Class<T> annotationType) {
        this.annotationType = annotationType;
        if (annotatedMethod == null) {
            this.keyPrefix = getPropertyKeyPrefix(annotatedClass);
            this.targetName = annotatedClass.getName();
        } else {
            this.keyPrefix = getPropertyKeyPrefix(annotatedMethod);
            this.targetName = annotatedClass.getName() + "." + annotatedMethod.getName();
        }
        this.annotation = annotation;
        this.annotatedClass = annotatedClass;
        this.annotatedMethod = annotatedMethod;
    }

    protected class AnnotationParameterConfig<S> {

        protected S parameterValue = null;
        protected final String parameterName;
        protected final Class<S> parameterType;

        private AnnotationParameterConfig(String parameterName, Class<S> parameterType) {
            this.parameterType = parameterType;
            this.parameterName = parameterName;
        }

        private void init(T annotation) {

            S configValue = getConfigValue();
            if (configValue != null) {
                parameterValue = configValue;
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        try {
                            Method m = annotationType.getDeclaredMethod(parameterName);
                            parameterValue = parameterType.cast(m.invoke(annotation));
                            return null;
                        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT5997E", e), e);
                        }
                    }
                });
            }
        }

        public S getValue() {
            return parameterValue;
        }

        public S getConfigValue() {
            return readConfigValue(parameterType);
        }

        public <P> P readConfigValue(Class<P> type) {

            Config mpConfig = ConfigProvider.getConfig(getClassLoader(annotatedClass));

            String key = getPropertyKey(keyPrefix, parameterName);
            P configValue = mpConfig.getOptionalValue(key, type).orElse(null);

            if (configValue == null) {
                key = getPropertyKey("", parameterName);
                configValue = mpConfig.getOptionalValue(key, type).orElse(null);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (configValue != null) {
                    Tr.debug(tc, "Found config value for " + getPropertyKey(keyPrefix, parameterName), configValue);
                } else {
                    Tr.debug(tc, "No config value found for " + getPropertyKey(keyPrefix, parameterName));
                }
            }

            return configValue;
        }
    }

    /**
     * Config for parameters which take a class
     * <p>
     * If the value is retrieved from config, this class validates that the class is of the correct type
     */
    protected class AnnotationParameterConfigClass<S> extends AnnotationParameterConfig<Class<? extends S>> {

        private final Class<S> parameterClass;

        @SuppressWarnings("unchecked")
        private AnnotationParameterConfigClass(String parameterName, Class<S> parameterClass) {
            super(parameterName, (Class<Class<? extends S>>) (Class<?>) Class.class); // Hate generics
            this.parameterClass = parameterClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends S> getConfigValue() {

            String configValue = readConfigValue(String.class);

            Class<?> result = null;
            if (configValue != null) {
                try {

                    result = getClassLoader(annotatedClass).loadClass(configValue);

                } catch (ClassNotFoundException ex) {
                    throw new FaultToleranceException(Tr.formatMessage(tc, "Cannot load class {0} specified in config for {1}",
                                                                       configValue, getPropertyKey(keyPrefix, parameterName)));
                }

                if (!parameterClass.isAssignableFrom(result)) {
                    throw new FaultToleranceException(Tr.formatMessage(tc, "Class {0} cannot be assigned to type {1}, as specified in config for {2}",
                                                                       configValue, parameterClass.getName(), getPropertyKey(keyPrefix, parameterName)));
                }
            }

            // Safe as checked above
            return (Class<? extends S>) result;
        }

    }

    /**
     * Config for parameters which take a class
     * <p>
     * If the value is retrieved from config, this class validates that the classes are of the correct type
     */
    protected class AnnotationParameterConfigClassArray<S> extends AnnotationParameterConfig<Class<? extends S>[]> {

        private final Class<S> parameterClass;

        @SuppressWarnings("unchecked")
        private AnnotationParameterConfigClassArray(String parameterName, Class<S> parameterClass) {
            super(parameterName, (Class<Class<? extends S>[]>) (Class<?>) Class[].class); // Hate generics
            this.parameterClass = parameterClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends S>[] getConfigValue() {
            String[] configValue = readConfigValue(String[].class);

            Class<?>[] result = null;
            if (configValue != null) {
                result = new Class<?>[configValue.length];
                for (int i = 0; i < configValue.length; i++) {
                    try {
                        result[i] = getClassLoader(annotatedClass).loadClass(configValue[i]);
                    } catch (ClassNotFoundException ex) {
                        throw new FaultToleranceException(Tr.formatMessage(tc, "Cannot load class {0} specified in config for {1}", configValue[i],
                                                                           getPropertyKey(keyPrefix, parameterName)));
                    }

                    if (!parameterClass.isAssignableFrom(result[i])) {
                        throw new FaultToleranceException(Tr.formatMessage(tc, "Class {0} cannot be assigned to type {1}, as specified in config for {2}",
                                                                           configValue[i], parameterClass.getName(), getPropertyKey(keyPrefix, parameterName)));
                    }
                }
            }

            // Safe as checked above
            return (Class<? extends S>[]) result;
        }

    }

    protected <S> AnnotationParameterConfig<S> getParameterConfig(String name, Class<S> type) {
        AnnotationParameterConfig<S> parameterConfig = new AnnotationParameterConfig<>(name, type);
        parameterConfig.init(this.annotation);
        return parameterConfig;
    }

    protected <S> AnnotationParameterConfig<Class<? extends S>> getParameterConfigClass(String name, Class<S> type) {
        AnnotationParameterConfig<Class<? extends S>> parameterConfig = new AnnotationParameterConfigClass<>(name, type);
        parameterConfig.init(this.annotation);
        return parameterConfig;
    }

    protected <S> AnnotationParameterConfig<Class<? extends S>[]> getParameterConfigClassArray(String name, Class<S> type) {
        AnnotationParameterConfig<Class<? extends S>[]> parameterConfig = new AnnotationParameterConfigClassArray<>(name, type);
        parameterConfig.init(this.annotation);
        return parameterConfig;

    }

    public Class<T> annotationType() {
        return annotationType;
    }

    /**
     * @return the fully qualified name of the annotated member
     */
    public String getTargetName() {
        return this.targetName;
    }

    private String getPropertyKey(String prefix, String parameter) {
        // <prefix>Annotation/parameter
        String key = prefix + annotationType.getSimpleName() + "/" + parameter;
        return key;
    }

    private static String getPropertyKeyPrefix(Method method) {
        // <classname>/methodname/......
        Class<?> clazz = method.getDeclaringClass();
        clazz = FTUtils.getRealClass(clazz);
        String key = clazz.getName() + "/" + method.getName() + "/";
        return key;
    }

    private static String getPropertyKeyPrefix(Class<?> clazz) {
        // <classname>/.......
        //need to make sure this is not a proxy class. If it is, get its superclass
        clazz = FTUtils.getRealClass(clazz);
        String key = clazz.getName() + "/";
        return key;
    }

    public Class<?> getAnnotatedClass() {
        return annotatedClass;
    }

    public Method getAnnotatedMethod() {
        return annotatedMethod;
    }

    public void validate() {
        //no-op by default
    }

    /**
     * Get hold of classloader
     *
     * @param clazz the class
     * @return the classloader that loads the clazz
     */
    private ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader classloader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {

                return annotatedClass.getClassLoader();

            }
        });
        return classloader;

    }
}

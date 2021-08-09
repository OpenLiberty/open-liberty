/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.opentracing.jaeger.adapter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.CodecConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.ReporterConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SamplerConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.SenderConfiguration;

/**
 *
 */
public abstract class JaegerAdapterFactory {
    
    private static final String JAEGER_CONFIGURATION_IMPL = "com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl.ConfigurationImpl";
    private static final Class<?>[] JAEGER_CONFIGURATION_ARG_TYPES = { String.class };

    private static final String JAEGER_REPORTER_CONFIGURATION_IMPL = "com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl.ReporterConfigurationImpl";
    private static final Class<?>[] JAEGER_REPORTER_CONFIGURATION_ARG_TYPES = { };

    private static final String JAEGER_SENDER_CONFIGURATION_IMPL = "com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl.SenderConfigurationImpl";
    private static final Class<?>[] JAEGER_SENDER_CONFIGURATION_ARG_TYPES = { };

    private static final String JAEGER_SAMPLER_CONFIGURATION_IMPL = "com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl.SamplerConfigurationImpl";
    private static final Class<?>[] JAEGER_SAMPLER_CONFIGURATION_ARG_TYPES = { };

    private static final String JAEGER_CODEC_CONFIGURATION_IMPL = "com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl.CodecConfigurationImpl";
    private static final Class<?>[] JAEGER_CODEC_CONFIGURATION_ARG_TYPES = { };

    protected abstract ClassLoader getClassLoader();

    protected static final <T> T getInstance(ClassLoader classloader, Class<T> interfaceClass, String implClassName, Class<?>[] parameterTypes, Object... parameters) {
        Class<? extends T> implClass = getImplClass(classloader, interfaceClass, implClassName);
        T instance = getInstance(implClass, parameterTypes, parameters);
        return instance;
    }

    protected static final <T> T getInstance(Class<T> implClass, Class<?>[] parameterTypes, Object[] parameters) {
        Constructor<T> xtor = getConstructor(implClass, parameterTypes);

        T instance = null;
        try {
            instance = xtor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new JaegerAdapterException(e);
        }
        return instance;
    }

    protected static final <T> Constructor<T> getConstructor(Class<T> implClass, Class<?>... parameterTypes) {
        Constructor<T> xtor = null;
        try {
            xtor = implClass.getConstructor(parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new JaegerAdapterException(e);
        }
        return xtor;
    }

    @SuppressWarnings("unchecked")
    protected static final <T> Class<? extends T> getImplClass(ClassLoader classloader, Class<T> interfaceClass, String implClassName) {
        Class<? extends T> implClass = null;
        try {
            implClass = (Class<? extends T>) classloader.loadClass(implClassName);
        } catch (ClassNotFoundException e) {
            throw new JaegerAdapterException(e);
        }
        return implClass;
    }

    public Configuration newConfiguration(String serviceName) {
        Configuration config = getInstance(getClassLoader(), Configuration.class, JAEGER_CONFIGURATION_IMPL, JAEGER_CONFIGURATION_ARG_TYPES, serviceName);
        return config;
    }

    public ReporterConfiguration newReporterConfiguration() {
        ReporterConfiguration config = getInstance(getClassLoader(), ReporterConfiguration.class, JAEGER_REPORTER_CONFIGURATION_IMPL, JAEGER_REPORTER_CONFIGURATION_ARG_TYPES);
        return config;
    }

    public SenderConfiguration newSenderConfiguration() {
        SenderConfiguration config = getInstance(getClassLoader(), SenderConfiguration.class, JAEGER_SENDER_CONFIGURATION_IMPL, JAEGER_SENDER_CONFIGURATION_ARG_TYPES);
        return config;
    }
    
    public SamplerConfiguration newSamplerConfiguration() {
        SamplerConfiguration config = getInstance(getClassLoader(), SamplerConfiguration.class, JAEGER_SAMPLER_CONFIGURATION_IMPL, JAEGER_SAMPLER_CONFIGURATION_ARG_TYPES);
        return config;
    }

    public CodecConfiguration newCodecConfiguration() {
        CodecConfiguration config = getInstance(getClassLoader(), CodecConfiguration.class, JAEGER_CODEC_CONFIGURATION_IMPL, JAEGER_CODEC_CONFIGURATION_ARG_TYPES);
        return config;
    }

}

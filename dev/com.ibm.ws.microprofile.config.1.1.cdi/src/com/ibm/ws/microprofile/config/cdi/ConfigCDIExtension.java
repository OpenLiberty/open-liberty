/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.cdi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.converters.DefaultConverters;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 * The ConfigCDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
public class ConfigCDIExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(ConfigCDIExtension.class);

    protected Set<Type> validInjectionTypes = new HashSet<Type>();
    protected Set<Type> badInjectionTypes = new HashSet<Type>();

    public ConfigCDIExtension() {
        validInjectionTypes.addAll(getDefaultConverterTypes());
    }

    protected void addBadInjectionType(Type type) {
        badInjectionTypes.add(type);
    }

    protected Collection<? extends Type> getDefaultConverterTypes() {
        return DefaultConverters.getDefaultConverters().getTypes();
    }

    void processInjectionTarget(@Observes ProcessInjectionTarget<?> pit) {
        Class<?> targetClass = pit.getAnnotatedType().getJavaClass();
        ClassLoader classLoader = targetClass.getClassLoader();

        for (InjectionPoint injectionPoint : pit.getInjectionTarget().getInjectionPoints()) {
            ConfigProperty configProperty = ConfigProducer.getConfigPropertyAnnotation(injectionPoint);
            if (configProperty != null) {
                Type type = injectionPoint.getType();
                ConfigProperty qualifier = ConfigProducer.getConfigPropertyAnnotation(injectionPoint);
                String defaultValue = qualifier.defaultValue();
                String propertyName = null;
                Throwable configException = null;
                try {
                    propertyName = ConfigProducer.getPropertyName(injectionPoint, qualifier);
                    configException = validateConfigProperty(type, propertyName, defaultValue, classLoader);
                } catch (IllegalArgumentException e) {
                    configException = e;
                }

                if (configException != null) {
                    Tr.error(tc, "unable.to.resolve.injection.point.CWMCG5003E", injectionPoint, configException);
                }
            }
        }
    }

    protected Throwable validateConfigProperty(Type type, String propertyName, String defaultValue, ClassLoader classLoader) {
        Throwable configException = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            configException = processParameterizedType(propertyName, defaultValue, pType, classLoader);
        } else {
            configException = processConversionType(propertyName, defaultValue, type, classLoader, false);
        }
        return configException;
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        ConfigBean configBean = new ConfigBean(beanManager);
        abd.addBean(configBean);

        validInjectionTypes.removeAll(badInjectionTypes);

        for (Type type : validInjectionTypes) {
            try {
                if (type instanceof TypeVariable) {
                    TypeVariable<?> typeVar = (TypeVariable<?>) type;
                    Type[] bounds = typeVar.getBounds();
                    for (Type bound : bounds) {
                        addConfigPropertyBean(abd, beanManager, bound);
                    }
                } else {
                    addConfigPropertyBean(abd, beanManager, type);
                }
            } catch (ConfigTypeException e) {
                abd.addDefinitionError(e);
            }
        }
    }

    protected Throwable processParameterizedType(String propertyName, String defaultValue, ParameterizedType pType, ClassLoader classLoader) {
        Throwable configException = null;
        Type rType = pType.getRawType();

        if (Provider.class.isAssignableFrom((Class<?>) rType)) {
            Type[] aTypes = pType.getActualTypeArguments();
            //instance must have exactly one type arg
            Type type = aTypes[0];
            //Check if the Provider provides an optional property.
            if (type instanceof ParameterizedType) {
                ParameterizedType maybeOptionalType = (ParameterizedType) type;
                if (Optional.class.isAssignableFrom((Class<?>) maybeOptionalType.getRawType())) {
                    configException = processConversionType(propertyName, defaultValue, type, classLoader, true);
                } else {
                    configException = processConversionType(propertyName, defaultValue, type, classLoader, false);
                }
            } else {
                configException = processConversionType(propertyName, defaultValue, type, classLoader, false);
            }
        } else if (Optional.class.isAssignableFrom((Class<?>) rType)) {
            //property is optional
            configException = processConversionType(propertyName, defaultValue, pType, classLoader, true);
        } else {
            configException = processConversionType(propertyName, defaultValue, pType, classLoader, false);
        }
        return configException;
    }

    protected Throwable processConversionType(String propertyName, String defaultValue, Type injectionType, ClassLoader classLoader,
                                              boolean optional) {
        Throwable configException = null;

        Config config = ConfigProvider.getConfig(classLoader);
        try {
            ConfigProducer.newValue(config, propertyName, defaultValue, injectionType, optional);
            validInjectionTypes.add(injectionType);
        } catch (Throwable e) {
            configException = e;
            addBadInjectionType(injectionType);
        }

        return configException;
    }

    private void addConfigPropertyBean(AfterBeanDiscovery abd, BeanManager beanManager, Type type) throws ConfigTypeException {

        //in addition to Class, a type may be...
        //GenericArrayType, ParameterizedType, TypeVariable<D>, WildcardType

        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            addConfigPropertyBean(abd, beanManager, clazz);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            addConfigPropertyBean(abd, beanManager, pType);
        } else {
            //Need more testing for GenericArrayType, TypeVariable<D>, WildcardType?
            throw new ConfigTypeException(Tr.formatMessage(tc, "unable.to.determine.injection.type.CWMCG5001E", type));
        }
    }

    private void addConfigPropertyBean(AfterBeanDiscovery abd, BeanManager beanManager, Class<?> type) {
        if (!type.isPrimitive()) {
            addConfigPropertyBean(abd, beanManager, type, type);
        }
    }

    private void addConfigPropertyBean(AfterBeanDiscovery abd, BeanManager beanManager, ParameterizedType type) throws ConfigTypeException {
        Type rType = type.getRawType();
        if (rType instanceof Class) {
            Class<?> clazz = (Class<?>) rType;
            addConfigPropertyBean(abd, beanManager, type, clazz);
        } else {
            //what else can it be here?!
            throw new ConfigTypeException(Tr.formatMessage(tc, "unable.to.determine.injection.type.CWMCG5001E", type));
        }
    }

    protected <T> void addConfigPropertyBean(AfterBeanDiscovery abd, BeanManager beanManager, Type beanType, Class<T> clazz) {
        ConfigPropertyBean<T> converterBean = new ConfigPropertyBean<T>(beanManager, beanType, clazz, ConfigPropertyLiteral.INSTANCE);
        abd.addBean(converterBean);
    }
}

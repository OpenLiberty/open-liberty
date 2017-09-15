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

import java.lang.annotation.Annotation;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

/**
 * ConfigProducer is a utility class which contains common methods used by the Config CDI extension.
 */
public class ConfigProducer {

    private static final TraceComponent tc = Tr.register(ConfigProducer.class);

    @FFDCIgnore(NoSuchElementException.class)
    static <T> T newValue(Config config, InjectionPoint injectionPoint, Class<T> type, boolean optional) {
        ConfigProperty qualifier = getConfigPropertyAnnotation(injectionPoint);
        String propertyName = getPropertyName(injectionPoint, qualifier);
        T value = null;
        try {
            value = config.getValue(propertyName, type);
        } catch (NoSuchElementException e) {
            //property not found, check for a default value on the annotation
            String defaultValue = qualifier.defaultValue();

            //if the defaultValue was not configured on the annotaion
            if (ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
                //if the property is optional just return null
                if (optional) {
                    value = null;
                } else {
                    //otherwise rethrow the exception
                    throw e;
                }
            } else if (defaultValue != null) {
                //if the default was not null, convert it
                WebSphereConfig wConfig = (WebSphereConfig) config;
                value = wConfig.convertValue(defaultValue, type);
            }

        }
        return value;
    }

    static <T> Optional<T> newOptional(Config config, InjectionPoint injectionPoint, Class<T> type) {
        T value = newValue(config, injectionPoint, type, true);
        Optional<T> optional = Optional.ofNullable(value);
        return optional;
    }

    static ConfigProperty getConfigPropertyAnnotation(InjectionPoint injectionPoint) {

        ConfigProperty configProperty = null;

        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        if (qualifiers != null) {

            //find the qualifier
            for (Annotation qualifier : qualifiers) {
                if (qualifier.annotationType().equals(ConfigProperty.class)) {
                    configProperty = (ConfigProperty) qualifier;
                    break;
                }
            }
        }
        return configProperty;
    }

    static String getPropertyName(InjectionPoint injectionPoint, ConfigProperty qualifier) {

        if (qualifier == null) {
            //couldn't find the qualifier
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getPropertyName", "Unsupported InjectionPoint: " + injectionPoint);
            }
            throw new IllegalArgumentException(Tr.formatMessage(tc, "unable.to.determine.injection.name.CWMCG5002E", injectionPoint));
        }

        String propertyName = qualifier.name();

        if (propertyName.equals("")) {
            Annotated annotated = injectionPoint.getAnnotated();
            if (annotated instanceof AnnotatedField) {
                AnnotatedField<?> field = (AnnotatedField<?>) annotated;
                propertyName = getDefaultPropertyName(field);
            } else if (annotated instanceof AnnotatedParameter) {
                //it isn't reliably possible to reflectively work out parameter names
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getPropertyName", "Unsupported InjectionPoint: " + injectionPoint);
                }
                throw new IllegalArgumentException(Tr.formatMessage(tc, "unable.to.determine.injection.name.CWMCG5002E", injectionPoint));
            }
        }

        return propertyName;
    }

    static <T> T getDefaultValue(Config config, ConfigProperty qualifier, Class<T> type) {
        String defaultValue = qualifier.defaultValue();

        T value = null;
        if (!ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
            WebSphereConfig wConfig = (WebSphereConfig) config;
            value = wConfig.convertValue(defaultValue, type);
        }

        return value;
    }

    static String getDefaultPropertyName(AnnotatedField<?> field) {
        String className = field.getDeclaringType().getJavaClass().getCanonicalName();
        String fieldName = field.getJavaMember().getName();
        return className + "." + fieldName;
    }

    static String lowerCaseInitialLetter(String string) {
        return string != null && string.length() >= 1 ? string.substring(0, 1).toLowerCase() + string.substring(1) : "";
    }

}

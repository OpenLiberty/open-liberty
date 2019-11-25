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
package com.ibm.ws.microprofile.config14.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessObserverMethod;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.config.cdi.ConfigPropertyBean;
import com.ibm.ws.microprofile.config12.cdi.Config12CDIExtension;
import com.ibm.ws.microprofile.config14.converters.Config14DefaultConverters;

/**
 * The Config12CDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
public class Config14CDIExtension extends Config12CDIExtension implements Extension, WebSphereCDIExtension {

    @Override
    protected Collection<? extends Type> getDefaultConverterTypes() {
        return Config14DefaultConverters.getDefaultConverters().getTypes();
    }

    @Override
    protected <T> void addConfigPropertyBean(AfterBeanDiscovery abd, BeanManager beanManager, Type beanType, Class<T> clazz) {
        ConfigPropertyBean<T> converterBean = new ConfigPropertyBean<T>(beanManager, beanType, clazz, Config14PropertyLiteral.INSTANCE);
        abd.addBean(converterBean);
    }

    void processObserverMethod(@Observes ProcessObserverMethod<?, ?> pot) {
        AnnotatedMethod<?> annotatedMethod = pot.getAnnotatedMethod();
        System.out.println("AnnotatedMethod: " + annotatedMethod.getJavaMember());
        List<?> parameters = annotatedMethod.getParameters();
        for (Object p : parameters) {
            AnnotatedParameter<?> parameter = (AnnotatedParameter<?>) p;
            Type type = parameter.getBaseType();
            System.out.println("AnnotatedParameter: " + type);
            Set<Annotation> annotations = parameter.getAnnotations();
            for (Annotation annotation : annotations) {
                System.out.println("Annotation: " + annotation);
                if (ConfigProperty.class.isAssignableFrom(annotation.annotationType())) {

                    ConfigProperty configProperty = (ConfigProperty) annotation;
                    System.out.println("ConfigProperty: " + configProperty);
                    String propertyName = configProperty.name();
                    String defaultValue = configProperty.defaultValue();

                    ClassLoader classLoader = annotatedMethod.getDeclaringType().getJavaClass().getClassLoader();

                    Throwable configException = validateConfigProperty(type, propertyName, defaultValue, classLoader);
//                    if (configException != null) {
//                        Tr.error(tc, "unable.to.resolve.injection.point.CWMCG5003E", injectionPoint, configException);
//                    }

//                    WebSphereConfig wConfig = (WebSphereConfig) ConfigProvider.getConfig(classLoader);
//
//                    try {
//                        ConfigProducer.newValue(wConfig, propertyName, defaultValue, type, false); //TODO Can it be Optional???
//                    } catch (Throwable t) {
//                        addBadInjectionType(type);
//                    }
                }
            }
        }
    }
}

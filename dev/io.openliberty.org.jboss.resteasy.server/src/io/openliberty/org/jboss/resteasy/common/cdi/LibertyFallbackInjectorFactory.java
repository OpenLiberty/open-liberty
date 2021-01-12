/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.jboss.resteasy.common.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.metadata.Parameter;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.resteasy.spi.metadata.ResourceLocator;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@SuppressWarnings("rawtypes")
public class LibertyFallbackInjectorFactory implements InjectorFactory {
    private static final TraceComponent tc = Tr.register(LibertyFallbackInjectorFactory.class);

    private final InjectorFactory delegate;
    
    public LibertyFallbackInjectorFactory() {
        InjectorFactory factory;
        try {
            factory = new LibertyCdiInjectorFactory();
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception initializing LibertyCdiInjectorFactory - expected if module declines CDI", t);
            }
            factory = new InjectorFactoryImpl();
        }
        delegate = factory;
    }
    @Override
    public ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory factory) {
        return delegate.createConstructor(constructor, factory);
    }

    @Override
    public PropertyInjector createPropertyInjector(Class resourceClass, ResteasyProviderFactory factory) {
        return delegate.createPropertyInjector(resourceClass, factory);
    }

    @Override
    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName, Class type, Type genericType, Annotation[] annotations, ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations, factory);
    }

    @Override
    public ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName, Class type, Type genericType, Annotation[] annotations, boolean useDefault, ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations, useDefault, factory);
    }

    @Override
    public ValueInjector createParameterExtractor(Parameter parameter, ResteasyProviderFactory providerFactory) {
        return delegate.createParameterExtractor(parameter, providerFactory);
    }

    @Override
    public MethodInjector createMethodInjector(ResourceLocator method, ResteasyProviderFactory factory) {
        return delegate.createMethodInjector(method, factory);
    }

    @Override
    public PropertyInjector createPropertyInjector(ResourceClass resourceClass, ResteasyProviderFactory providerFactory) {
        return delegate.createPropertyInjector(resourceClass, providerFactory);
    }

    @Override
    public ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory) {
        return delegate.createConstructor(constructor, providerFactory);
    }
}

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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This CDI Bean controls the creation of all raw types whose values are obtained via a Config instance.
 * They are all Dependent scope.
 */
public class ConfigPropertyBean<T> extends AbstractConfigBean<T> implements Bean<T>, PassivationCapable {

    private final Class<T> beanClass;

    private final BeanManager beanManager;

    public ConfigPropertyBean(BeanManager beanManager, Class<T> beanClass) {
        this(beanManager, beanClass, beanClass);
    }

    public ConfigPropertyBean(BeanManager beanManager, Type beanType, Class<T> beanClass) {
        super(beanManager, beanType, ConfigPropertyLiteral.INSTANCE);
        this.beanClass = beanClass;
        this.beanManager = beanManager;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public T create(CreationalContext<T> creationalContext) {
        InjectionPoint injectionPoint = getInjectionPoint(beanManager, creationalContext);

        // Note the config is cached per thread context class loader
        // This shouldn't matter though as the config object is updated with values dynamically
        // Also means that injecting config does things the same way as calling `getConfig().getValue()`
        Config config = ConfigProvider.getConfig();

        T instance = null;

        Type ipType = injectionPoint.getType();
        boolean optional = false;
        if (ipType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) ipType;
            Type rType = pType.getRawType();
            optional = (rType == Optional.class);
        }
        instance = (T) ConfigProducer.newValue(config, injectionPoint, ipType, optional);
        return instance;
    }

    private InjectionPoint getInjectionPoint(final BeanManager beanManager, final CreationalContext<?> creationalContext) {
        InjectionPoint injectionPoint = AccessController.doPrivileged(new PrivilegedAction<InjectionPoint>() {
            @Override
            public InjectionPoint run() {
                return (InjectionPoint) beanManager.getInjectableReference(new DummyInjectionPoint(), creationalContext);
            }
        });
        return injectionPoint;
    }

    /** {@inheritDoc} */
    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        creationalContext.release();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean isNullable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }
}

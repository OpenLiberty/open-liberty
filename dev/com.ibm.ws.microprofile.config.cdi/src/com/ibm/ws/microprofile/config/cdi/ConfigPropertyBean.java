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
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This CDI Bean controls the creation of all raw types whose values are obtained via a Config instance.
 * They are all Dependent scope.
 */
public class ConfigPropertyBean<T> extends AbstractConfigBean<T> implements Bean<T>, PassivationCapable {

    private static final TraceComponent tc = Tr.register(ConfigPropertyBean.class);

    private final Class<T> beanClass;

    public ConfigPropertyBean(BeanManager beanManager, Class<T> beanClass) {
        this(beanManager, beanClass, beanClass);
    }

    public ConfigPropertyBean(BeanManager beanManager, Type beanType, Class<T> beanClass) {
        super(beanManager, beanType, ConfigPropertyLiteral.INSTANCE);
        this.beanClass = beanClass;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public T create(CreationalContext<T> creationalContext) {
        CDI<Object> cdi = CDI.current();
        BeanManager beanManager = cdi.getBeanManager();
        InjectionPoint injectionPoint = getInjectionPoint(beanManager, creationalContext);

        //TODO when we get the Config to work in the correct CDI scopes, we'll want CDI to provide the Config here
//        Set<Bean<?>> beans = beanManager.getBeans(Config.class);
//        Bean<?> bean = beanManager.resolve(beans);
//
//        Config config = (Config) beanManager.getReference(bean, Config.class, creationalContext);

        //TODO for now we'll just get a new one so we can manually release it again straight away
        ConfigBuilder builder = ConfigProviderResolver.instance().getBuilder();
        builder.addDiscoveredConverters();
        builder.addDefaultSources();
        builder.addDiscoveredSources();
        Config config = builder.build();

        T instance = null;

        try {
            Type ipType = injectionPoint.getType();
            if (ipType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) ipType;
                Type rType = pType.getRawType();
                if (rType == Optional.class) {
                    Type[] tArgs = pType.getActualTypeArguments();
                    Type aType = tArgs[0];
                    Class<?> aClass = (Class<?>) aType;
                    instance = (T) getOptional(config, injectionPoint, aClass);
                } else {
                    throw new IllegalArgumentException(Tr.formatMessage(tc, "unable.to.determine.injection.type.CWMCG5001E", ipType));
                }
            } else if (ipType instanceof Class) {
                Class<T> ipClass = (Class<T>) ipType;
                instance = ConfigProducer.newValue(config, injectionPoint, ipClass, false);
            } else {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "unable.to.determine.injection.type.CWMCG5001E", ipType));
            }
        } finally {
            ConfigProviderResolver.instance().releaseConfig(config);
        }
        return instance;
    }

    private <K> Optional<K> getOptional(Config config, InjectionPoint injectionPoint, Class<K> clazz) {
        Optional<K> opt = ConfigProducer.newOptional(config, injectionPoint, clazz);
        return opt;
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
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNullable() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }
}

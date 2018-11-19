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

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.PassivationCapable;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This CDI Bean controls the creation and destruction of Config instances injected by CDI.
 * They are all Dependent scope.
 */
public class ConfigBean extends AbstractConfigBean<Config> implements Bean<Config>, PassivationCapable {

    /**
     * @param clazz
     */
    public ConfigBean(BeanManager beanManager) {
        super(beanManager, Config.class, DefaultLiteral.INSTANCE);
    }

    /** {@inheritDoc} */
    @Override
    public Config create(CreationalContext<Config> creationalContext) {
        // Although this is a request scoped bean, this config instance will be cached per classloader, so you don't really get a new config object per request
        // However, the config object is updated with new values dynamically, so the user shouldn't notice any difference.
        // This also means that injecting config gives the same result as calling getConfig() in user code
        return ConfigProvider.getConfig();
    }

    /** {@inheritDoc} */
    @Override
    public void destroy(Config config, CreationalContext<Config> creationalContext) {
        creationalContext.release();
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Class<?> getBeanClass() {
        return Config.class;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public boolean isNullable() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

}

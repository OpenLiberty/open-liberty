/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.cdi;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;

import org.eclipse.microprofile.config.Config;

import com.ibm.ws.microprofile.config.cdi.ConfigBean;

/**
 * This CDI Bean controls the creation and destruction of Config instances injected by CDI.
 * They are all Dependent scope.
 */
public class Config14ConfigBean extends ConfigBean {

    /** {@inheritDoc} */
    public Config14ConfigBean(BeanManager beanManager) {
        super(beanManager);
    }

    /** {@inheritDoc} */
    @Override
    public Config create(CreationalContext<Config> creationalContext) {
        // Although this is a singleton bean, the DelegatingConfig checks the current classloader on each method call
        // The config for each classloader is cached so this is ok
        // However, the config object is updated with new values dynamically, so the user shouldn't notice any difference.
        // This means that calling an injected Config always gives the same result as making the same call on the result of getConfig()
        return Config14DelegatingConfig.INSTANCE;
    }

}

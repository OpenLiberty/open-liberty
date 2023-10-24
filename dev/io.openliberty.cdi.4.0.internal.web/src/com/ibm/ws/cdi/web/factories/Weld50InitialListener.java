/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.web.factories;

import org.jboss.weld.bootstrap.BeanDeploymentModule;
import org.jboss.weld.bootstrap.BeanDeploymentModules;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.module.web.servlet.WeldInitialListener;

import jakarta.enterprise.event.Shutdown;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Any;
import jakarta.servlet.ServletContextEvent;

/**
 *
 */
public class Weld50InitialListener extends WeldInitialListener {

    private final BeanDeploymentModule module;
    private boolean initialized = false;

    public Weld50InitialListener(BeanManagerImpl beanManagerImpl) {
        super(beanManagerImpl);
        BeanDeploymentModules beanDeploymentModules = beanManagerImpl.getServices().get(BeanDeploymentModules.class);
        this.module = beanDeploymentModules != null ? beanDeploymentModules.getModule(beanManagerImpl) : null;
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        super.contextInitialized(event);
        initialized = true;
        if (this.module != null) {
            module.fireEvent(Startup.class, new Startup(), Any.Literal.INSTANCE);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (this.module != null) {
            module.fireEvent(Shutdown.class, new Shutdown(), Any.Literal.INSTANCE);
        }
        if (initialized) {
            super.contextDestroyed(event);
        }
    }
}

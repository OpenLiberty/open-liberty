/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.web.factories;

import javax.servlet.ServletContextEvent;

import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.servlet.WeldInitialListener;

public class Weld24InitialListener extends WeldInitialListener {

    private boolean initialized = false;

    public Weld24InitialListener(BeanManagerImpl beanManager) {
        super(beanManager);
    }

    /** {@inheritDoc} */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        super.contextInitialized(arg0);
        initialized = true;
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (initialized) {
            super.contextDestroyed(sce);
        }
    }

}

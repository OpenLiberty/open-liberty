/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jndi.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

import org.osgi.service.component.annotations.Component;

@Component(immediate = true, configurationPolicy = IGNORE, property = "service.vendor=IBM")
public class WASInitialContextFactoryBuilder implements InitialContextFactoryBuilder {

    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
                    throws NamingException {

        String icfFactory = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        InitialContextFactory icf = null;

        if (icfFactory != null) {
            try {
                Class<?> clazz = Class.forName(icfFactory, true, getClassLoader());
                icf = (InitialContextFactory) clazz.newInstance();
            } catch (Exception e) {
                //auto FFDC
            }
        }

        return icf;
    }

    private ClassLoader getClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run()
            {
                return Thread.currentThread().getContextClassLoader();
            }

        });
    }
}

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.pseudo.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * A pseudo implementation of {@link InitialContextFactoryBuilder} and {@link InitialContextFactory} that
 * will provide a {@link PseudoContext}, which will then determine when to throw an error with
 * a message indicating that the jndi feature isn't enabled.
 * 
 * <p>
 * 
 * This still supports specifying the {@link Context#INITIAL_CONTEXT_FACTORY} property, as
 * this functionality gives the ability for the user to specify the {@link InitialContextFactory}.
 */
public class PseudoContextFactory implements InitialContextFactoryBuilder, InitialContextFactory {

    @Override
    public Context getInitialContext(Hashtable<?, ?> env) throws NamingException {

        return new PseudoContext(env);
    }

    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> env) throws NamingException {
        String icfFactory = (String) env.get(Context.INITIAL_CONTEXT_FACTORY);

        if (icfFactory != null) {
            try {
                Class<?> clazz = Class.forName(icfFactory, true, getClassLoader());
                return (InitialContextFactory) clazz.newInstance();
            } catch (Exception e) {
                //auto FFDC
            }
        }

        return this;
    }

    private ClassLoader getClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }

        });
    }

}

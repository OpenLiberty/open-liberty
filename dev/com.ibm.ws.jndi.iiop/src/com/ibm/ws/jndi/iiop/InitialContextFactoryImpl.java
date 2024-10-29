/*******************************************************************************
 * Copyright (c) 2015,2024 IBM Corporation and others.
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
package com.ibm.ws.jndi.iiop;

import static org.osgi.framework.FrameworkUtil.getBundle;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class InitialContextFactoryImpl implements javax.naming.spi.InitialContextFactory {
    static final TraceComponent tc = Tr
                    .register(InitialContextFactoryImpl.class);

    private enum OrbHolder {
        INSTANCE;
        ORB orb = ORB.init((String[]) null, null);
        {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            orb.shutdown(true);
                            orb.destroy();
                        }
                    });
                    return null;
                }
            });
        }
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        final String methodName = "getInitialContext(): ";
        // if NOT in a bundle, create and return a context object
        if (getBundle(InitialContextFactoryImpl.class) == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "using internal ORB instance");
            return new JndiCosNamingContext(OrbHolder.INSTANCE.orb, environment);
        }
        // if the code reaches here, it is running in a bundle
        // so defer to the corbaloc handler to retrieve the context

        String uri = JndiCosNamingContext.getProviderUri(environment);
        // if the provider uri is unavailable, use the default
        if (uri == null) {
            uri = "corbaloc:rir:/NameService";
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + "using a provider uri of: " + uri);
        return (Context) new InitialContext().lookup(uri);
    }
}

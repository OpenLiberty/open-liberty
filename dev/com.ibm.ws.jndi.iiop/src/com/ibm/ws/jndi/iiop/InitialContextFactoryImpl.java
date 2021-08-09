/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi.iiop;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.omg.CORBA.ORB;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class InitialContextFactoryImpl implements
                javax.naming.spi.InitialContextFactory {
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
    public Context getInitialContext(Hashtable<?, ?> environment)
                    throws NamingException {
        final String methodName = "getInitialContext(): ";
        // if we are in a bundle, then defer to the corbaname lookup
        Bundle b = FrameworkUtil.getBundle(InitialContextFactoryImpl.class);
        if (b == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "using internal ORB instance");
            return new JndiCosNamingContext(OrbHolder.INSTANCE.orb, environment);
        }
        // if we get here, we are in a bundle
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

/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package com.ibm.ws.jndi.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Component(configurationPolicy = IGNORE, property = "service.vendor=IBM")
public class WASURLObjectFactoryFinder implements URLObjectFactoryFinder {
    private static final TraceComponent tc = Tr.register(WASURLObjectFactoryFinder.class);

    @Override
    public ObjectFactory findFactory(String urlSchema, Hashtable<?, ?> env) throws NamingException {
        final List<String> pkgPrefixes = new ArrayList<>();

        // Collect any package prefixes specified by the environment
        if (env != null) {
            String str = (String) env.get(Context.URL_PKG_PREFIXES);
            if (null != str) {
                StringTokenizer st = new StringTokenizer(str, ":");
                while (st.hasMoreTokens()) {
                    pkgPrefixes.add(st.nextToken());
                }
            }
        }

        // Always add default prefix
        pkgPrefixes.add("com.sun.jndi.url");

        try {
            return findFactory(urlSchema, pkgPrefixes);
        } catch (ClassNotFoundException e) {
            // auto-ffdc
            return null;
        }
    }

    @FFDCIgnore(ClassNotFoundException.class)
    private ObjectFactory findFactory(String urlSchema, List<String> pkgPrefixes) throws ClassNotFoundException {
        ClassNotFoundException cnfe = null;
        final ClassLoader tccl = Privileged.getThreadContextClassLoader();

        for (String pkgPrefix : pkgPrefixes) {
            String className = pkgPrefix + "." + urlSchema + "." + urlSchema + "URLContextFactory";

            try {
                return Privileged.getConstructor(tccl, className).newInstance();
            } catch (ClassNotFoundException e) {
		if (null == cnfe) cnfe = new ClassNotFoundException("Could not find factory for schema '" + urlSchema + "'");
		cnfe.addSuppressed(e);
            } catch (Exception e) {
                // auto FFDC - should be rare
            }
        }

        if (null != cnfe) throw cnfe;
        return null;
    }

    private enum Privileged implements PrivilegedAction<ClassLoader> {
        GET_TCCL {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        };

        static ClassLoader getThreadContextClassLoader() {
            return AccessController.doPrivileged(GET_TCCL);
        }

        @FFDCIgnore(PrivilegedActionException.class)
        static Constructor<? extends ObjectFactory> getConstructor(final ClassLoader tccl, final String className) throws Exception {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor<? extends ObjectFactory>>() {
                    public Constructor<? extends ObjectFactory> run() throws ClassNotFoundException, NoSuchMethodException {
                        Class<?> clazz = Class.forName(className, true, tccl);
                        if (!ObjectFactory.class.isAssignableFrom(clazz)) {
                            throw new ClassCastException(ObjectFactory.class.getName() + " is not assignable from " + clazz.getName());
                        }
                        @SuppressWarnings("unchecked")
                        Class<? extends ObjectFactory> ofc = (Class<? extends ObjectFactory>) clazz;
                        return ofc.getConstructor();
                    }
                });
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Component(configurationPolicy = IGNORE, property = "service.vendor=IBM")
public class WASURLObjectFactoryFinder implements URLObjectFactoryFinder {
    private static final TraceComponent tc = Tr.register(WASURLObjectFactoryFinder.class);

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public ObjectFactory findFactory(String urlSchema, Hashtable<?, ?> env) throws NamingException {
        List<String> pkgPrefixes = new ArrayList<String>();

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

        ClassNotFoundException cnfe = null;
        for (String pkgPrefix : pkgPrefixes) {
            String className = pkgPrefix + "." + urlSchema + "." + urlSchema + "URLContextFactory";

            try {
                Class<?> clazz = Class.forName(className, true, getClassLoader());
                return (ObjectFactory) clazz.newInstance();
            } catch (ClassNotFoundException e) {
                // Can occur quite often, so we want to minimize the noise
                // trace every occurrence, but only report FFDC for one occurrence
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Could not find class " + className, e);
                }
                cnfe = e;
            } catch (Exception e) {
                // auto FFDC - should be rare
            }
        }

        if (cnfe != null) {
            // If we get here it should be not-null unless some other exception were thrown -
            // in that case, the auto ffdc should have caught it
            FFDCFilter.processException(cnfe, WASURLObjectFactoryFinder.class.getName() + ".findFactory",
                                        "74", new Object[] { pkgPrefixes, urlSchema, env });
        }

        return null;
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

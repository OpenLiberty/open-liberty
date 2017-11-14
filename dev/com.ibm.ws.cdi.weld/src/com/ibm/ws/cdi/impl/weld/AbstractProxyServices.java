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
package com.ibm.ws.cdi.impl.weld;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.eclipse.osgi.util.ManifestElement;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;

import com.ibm.ws.cdi.CDIRuntimeException;

/**
 * This service is used to load proxy classes. We need a special classloader so that
 * we can load both weld classes and app classes.
 *
 *
 */
public abstract class AbstractProxyServices implements ProxyServices {

    private static final ManifestElement[] WELD_PACKAGES;

    static {
        try {
            WELD_PACKAGES = ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE, "org.jboss.weld.*");
        } catch (BundleException e) {
            throw new CDIRuntimeException(e);
        }
    }

    @Override
    public void cleanup() {
        // This implementation requires no cleanup
    }

    @Override
    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                // Must always use the bean type's classloader;
                // Otherwise package private access does not work.
                // Unfortunately this causes us issues for types from OSGi bundles.

                // It would be nice if we could have a marking header that allowed for
                // bundles to declare they provide CDI bean types, but this becomes
                // problematic for interface types that beans may be using for
                // injection types because the exporter may have no idea their types
                // are going to be used for CDI.  Therefore we have no way of knowing
                // ahead of time what bundles are providing CDI bean types.

                // This makes it impossible to use weaving hooks to add new dynamic
                // import packages.  The weaving hook approach requires
                // a weaving hook registration that knows ahead of time what
                // bundles provide CDI bean types and then on first class define using
                // that bundle's class loader the weaving hook would add the necessary
                // weld packages as dynamic imports.  We cannot and will
                // not be able to know exactly which bundles are providing bean
                // types until this getClassLoader method is called.  But by the time
                // this method is called it is too late for a weaving hook to do
                // anything because weld is going to use the returned class loader
                // immediately to reflectively define a proxy class.  The class loader
                // MUST have visibility to the weld packages before this reflective
                // call to defineClass.
                ClassLoader cl = proxiedBeanType.getClassLoader();
                if (cl instanceof BundleReference) {
                    Bundle b = ((BundleReference) cl).getBundle();
                    addWeldDynamicImports(b, WELD_PACKAGES);
                }
                return cl;
            }
        });
    }

    //implemented on a platform specific basis
    protected abstract void addWeldDynamicImports(Bundle b, ManifestElement[] dynamicImports);

    @Override
    public Class<?> loadBeanClass(final String className) {
        //This is tricky. Sometimes we need to use app classloader to load some app class
        try {
            return (Class<?>) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return Class.forName(className, true, getClassLoader(this.getClass()));
                }
            });
        } catch (PrivilegedActionException pae) {
            throw new CDIRuntimeException(pae.getException());
        }
    }

}

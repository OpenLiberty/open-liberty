/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.proxy;

import java.security.AccessController;
import java.security.AccessControlException;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.osgi.container.ModuleLoader;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.util.ManifestElement;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.ws.cdi.CDIRuntimeException;

/**
 * This service is used to load proxy classes. We need a special classloader so that
 * we can load both weld classes and app classes.
 */
//These classes do not extend a common abstract because it resulted in a circular dependency.
public class ProxyServicesImpl implements ProxyServices {
    private static final ManifestElement[] WELD_PACKAGES;
    private static final ClassLoader CLASS_LOADER_FOR_SYSTEM_CLASSES = org.jboss.weld.bean.ManagedBean.class.getClassLoader(); //I'm using this classloader because we'll need the weld classes to proxy anything.

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
System.out.println("Test");
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
        if (cl == null) {
            cl = CLASS_LOADER_FOR_SYSTEM_CLASSES;
        } else if (cl instanceof BundleReference) {
            Bundle b = ((BundleReference) cl).getBundle();
            addWeldDynamicImports(b, WELD_PACKAGES);
        }
            return cl;
    }
    
    @Override
    public Class<?> loadBeanClass(final String className) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    protected void addWeldDynamicImports(Bundle b, ManifestElement[] dynamicImports) {
        // There is no OSGi API for adding a dynamic import to a class loader except
        // by using a weaving hook, but that is too late for our usecase here.
        // Resorting to using Equinox internals to add imports just before weld does
        // a defineClass for the proxy class using the bundle's class loader.
        BundleWiring bWiring = b.adapt(BundleWiring.class);
        // Just doing a blind cast here because a cast exception will quickly tell us that something
        // changed in Equinox that broke us.
        ModuleWiring mWiring = (ModuleWiring) bWiring;
        ModuleLoader loader = mWiring.getModuleLoader();
        try {
            // there is not even Equinox API to do this.  Resorting to reflecting the internal
            // method of Equinox (org.eclipse.osgi.internal.loader.BundleLoader.addDynamicImportPackage(ManifestElement[]))
            // Note that BundleLoader is the implementation of ModuleLoader here.
            Method method = loader.getClass().getDeclaredMethod("addDynamicImportPackage", ManifestElement[].class);
            method.invoke(loader, new Object[] { dynamicImports });
            // Any exceptions here are bad news, just propagating them up.  Auto-FFDC is fine here
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
        }
    }
}


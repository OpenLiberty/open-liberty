/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

import org.eclipse.osgi.container.ModuleLoader;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.util.ManifestElement;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIRuntimeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This service is used to load proxy classes. We need a special classloader so that
 * we can load both weld classes and app classes.
 */
//These classes do not extend a common abstract because it resulted in a circular dependency.
public class ProxyServicesImpl implements ProxyServices {

    private static final TraceComponent tc = Tr.register(ProxyServicesImpl.class);

    private static final ReflectPermission SUPPRESS_ACCESS_CHECKS_PERMISSION = new ReflectPermission("suppressAccessChecks");
    private static final RuntimePermission DECLARED_MEMBERS_PERMISSION = new RuntimePermission("accessDeclaredMembers");
    private static final RuntimePermission GET_CLASS_LOADER_PERMISSION = new RuntimePermission("getClassLoader");

    private static final ManifestElement[] WELD_PACKAGES;
    private static final ClassLoader CLASS_LOADER_FOR_SYSTEM_CLASSES = org.jboss.weld.bean.ManagedBean.class.getClassLoader(); //I'm using this classloader because we'll need the weld classes to proxy anything.

    //A static enum with no instances is a way of ensuring nothing can create instances of this class. We only care about the static block.
    private static enum ClassLoaderMethods {
        ;//No enum instances

        private static final Method defineClass1, defineClass2, getClassLoadingLock;

        //This will be evaluated lazily when ClassLoaderMethods is first called.
        static {
            try {
                Method[] methods = AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>() {
                    @Override
                    public Method[] run() throws Exception {
                        Class<?> cl = Class.forName("java.lang.ClassLoader");
                        final String name = "defineClass";
                        final String getClassLoadingLockName = "getClassLoadingLock";

                        Method[] methods = new Method[3];

                        methods[0] = cl.getDeclaredMethod(name, String.class, byte[].class, int.class, int.class);
                        methods[1] = cl.getDeclaredMethod(name, String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
                        methods[2] = cl.getDeclaredMethod(getClassLoadingLockName, String.class);
                        methods[0].setAccessible(true);
                        methods[1].setAccessible(true);
                        methods[2].setAccessible(true);
                        return methods;
                    }
                });
                defineClass1 = methods[0];
                defineClass2 = methods[1];
                getClassLoadingLock = methods[2];
            } catch (PrivilegedActionException pae) {
                throw new RuntimeException("cannot initialize ClassPool", pae.getException());
            }
        }
    }

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
    @FFDCIgnore(ClassNotFoundException.class)
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len, ProtectionDomain protectionDomain) throws ClassFormatError {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "defineClass", originalClass, className, protectionDomain);
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SUPPRESS_ACCESS_CHECKS_PERMISSION);
            sm.checkPermission(DECLARED_MEMBERS_PERMISSION);
            sm.checkPermission(GET_CLASS_LOADER_PERMISSION);
        }

        ClassLoader loader = loaderMap.get(originalClass);
        Object classLoaderLock = null;
        try {
            classLoaderLock = ClassLoaderMethods.getClassLoadingLock.invoke(loader, className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        synchronized (classLoaderLock) {
            try {
                //First check we haven't defined this in another thread.
                return loadClass(className, loader);
            } catch (ClassNotFoundException e) {
                //Do nothing, move on to defining the class.
            }
            try {
                java.lang.reflect.Method method;
                Object[] args;
                if (protectionDomain == null) {
                    method = ClassLoaderMethods.defineClass1;
                    args = new Object[] { className, classBytes, off, len };
                } else {
                    method = ClassLoaderMethods.defineClass2;
                    args = new Object[] { className, classBytes, off, len, protectionDomain };
                }
                Class<?> clazz = (Class) method.invoke(loader, args); //This is the line that actually puts a new class into a ClassLoader.
                return clazz;
            } catch (RuntimeException e) {
                throw e;
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Class<?> loadClass(Class<?> originalClass, String classBinaryName) throws ClassNotFoundException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(GET_CLASS_LOADER_PERMISSION);

        ClassLoader cl = loaderMap.get(originalClass);
        return loadClass(classBinaryName, cl);
    }

    private Class<?> loadClass(String classBinaryName, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(classBinaryName, true, cl);
    }

    @Override
    public boolean supportsClassDefining() {
        return true;
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

    @Override
    public Class<?> loadBeanClass(final String className) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    private final ClassValue<ClassLoader> loaderMap = new ClassValue<ClassLoader>() {
        @Override
        public ClassLoader computeValue(Class<?> type) {
            return getClassLoader(type);
        }
    };
}

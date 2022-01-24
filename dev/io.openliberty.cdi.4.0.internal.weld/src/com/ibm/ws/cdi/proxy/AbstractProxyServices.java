/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.proxy;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.WeakHashMap;

import org.eclipse.osgi.util.ManifestElement;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;

import com.ibm.ws.cdi.CDIRuntimeException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * This service is used to load proxy classes. We need a special classloader so that
 * we can load both weld classes and app classes.
 */
public abstract class AbstractProxyServices implements ProxyServices {

	private static final ManifestElement[] WELD_PACKAGES;
	private static final ClassLoader CLASS_LOADER_FOR_SYSTEM_CLASSES = org.jboss.weld.bean.ManagedBean.class.getClassLoader(); //I'm using this classloader because we'll need the weld classes to proxy anything.

	private static enum ClassLoaderMethods {
		;//No enum instances

		private static final Method defineClass1, defineClass2, getClassLoadingLock;

		static {
			try {
				Method[] methods = AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>() {
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
				if (cl == null) {
					cl = CLASS_LOADER_FOR_SYSTEM_CLASSES;
				} else if (cl instanceof BundleReference) {
					Bundle b = ((BundleReference) cl).getBundle();
					addWeldDynamicImports(b, WELD_PACKAGES);
				}
				return cl;
			}
		});
	}

	@Override
	@FFDCIgnore(ClassNotFoundException.class)
	public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len, ProtectionDomain protectionDomain) throws ClassFormatError {

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
					args = new Object[]{className, classBytes, off, len};
				} else {
					method = ClassLoaderMethods.defineClass2;
					args = new Object[]{className, classBytes, off, len, protectionDomain};
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
		ClassLoader cl = loaderMap.get(originalClass);
		return loadClass(classBinaryName, cl);
	}

	private Class<?> loadClass(String classBinaryName, ClassLoader cl) throws ClassNotFoundException {
		return Class.forName(classBinaryName, true, cl);
	}

	public boolean supportsClassDefining() {
		return true;
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

	private final ClassValue<ClassLoader> loaderMap = new ClassValue<ClassLoader>() {
		public ClassLoader computeValue(Class<?> type) {
			return getClassLoader(type);
		}
	};
}
